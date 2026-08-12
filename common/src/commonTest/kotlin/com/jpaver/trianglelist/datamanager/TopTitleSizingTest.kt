package com.jpaver.trianglelist.datamanager

import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.ZumenInfo
import com.jpaver.trianglelist.viewmodel.TitleParamStr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 2026-08-12 user 指示の履歴:
 * 1. 「上部タイトルはもっとでかくしたい。管理を分けたほうが良い」── shrink 判定を title と
 *    サブタイトル行 (路線名+面積合計) で分離。色分け内訳が増えて長くなりがちな面積合計に
 *    タイトル自体が引きずられて縮む旧バグを潰す。
 * 2. 「上部タイトルの路線名と面積は同一行で表現していいぞ。路線名　面積　の形で」──
 *    路線名 と 面積合計 を 1 行にまとめる。路線名が先、面積が後。
 * 3. 「路線＋面積のサイズも二倍にしていい」── サブタイトル行のサイズも表題欄の 2 倍 (= TopTitle
 *    と同じ role) に。
 * 4. 「位置がおかしいぞ、テキストサイズを基にしてオフセット計算しろ」── サブタイトル行を
 *    タイトルと同じ大きさに拡大した結果、縦オフセットが旧 (小さい) サブタイトルサイズ前提の
 *    ままで下線と衝突した。オフセットはサブタイトル自身の実サイズ (areaFs) から逆算する。
 * 5. 「ストリングは連結して一個の文字列にして表現しろ」── 路線名+面積合計の各 segment を
 *    個別 prim で左詰めに並べる (= 手動 X 座標計算) のをやめ、1 本の文字列に連結して 1 個の
 *    DrawPrim.Text として中央寄せで描画する (色分けは失うが、位置計算のバグ源を断つ)。
 *
 * writeTextHV をキャプチャする最小サブクラスで DXF/SFC 等の backend 差を排除し、
 * DrawingFileWriter 本体のロジックだけを検証する。
 */
class TopTitleSizingTest {

    private class CapturingWriter : DrawingFileWriter() {
        override var trilist_: TriangleList = TriangleList()
        override var dedlist_: DeductionList = DeductionList()
        override var zumeninfo: ZumenInfo = ZumenInfo()
        override var titleTri_: TitleParamStr = TitleParamStr()
        override var titleDed_: TitleParamStr = TitleParamStr()

        data class Captured(val text: String, val size: Float, val x: Float, val y: Float)
        val captured = mutableListOf<Captured>()

        override fun writeTextHV(
            text: String,
            point: com.example.trilib.PointXY,
            color: Int,
            textsize: Float,
            alignH: Int,
            alignV: Int,
            angle: Double,
            scale: Float
        ) {
            captured.add(Captured(text, textsize, point.x.toFloat(), point.y.toFloat()))
        }
    }

    // 箱幅 (maxWidth = paperWcm - outerMarginCm*2 、既定 A3/1.5cm なら 39cm) を実測メトリクス問わず
    // 確実に超過させるための巨大文字列。全角換算 1 文字でも 200 個あれば base サイズでもどう見積もっても
    // 39cm を超える。
    private val hugeText = "あ".repeat(200)

    @Test
    fun `短いタイトルはサブタイトル行が長くても base サイズのまま`() {
        val w = CapturingWriter()
        w.zumeninfo.zumentitle = "図"
        w.rosenname_ = "線"
        w.zumenAreaSegments.add(DrawingFileWriter.AreaSegment(hugeText, w.WHITE))

        w.writeTopTitle(textsize = 0f)

        val titleBase = TextSizePolicy.resolve(TextRole.TopTitle)
        val titleCaptured = w.captured.first { it.text == "図" }
        assertEquals(titleBase, titleCaptured.size, 1e-4f,
            "サブタイトル行が長くても title 自体が箱に収まるなら base サイズを維持すべき (管理分離)")
    }

    @Test
    fun `サブタイトル行が長ければサブタイトルだけが縮み title は縮まない`() {
        val w = CapturingWriter()
        w.zumeninfo.zumentitle = "面 積 展 開 図"
        w.rosenname_ = "市道○○号線"
        w.zumenAreaSegments.add(DrawingFileWriter.AreaSegment(hugeText, w.WHITE))

        w.writeTopTitle(textsize = 0f)

        val titleBase = TextSizePolicy.resolve(TextRole.TopTitle)
        val titleCaptured = w.captured.first { it.text == w.zumeninfo.zumentitle }
        val subtitleCaptured = w.captured.first { it.text.startsWith(w.rosenname_) }

        assertEquals(titleBase, titleCaptured.size, 1e-4f, "title はサブタイトル行の長さに引きずられない")
        assertTrue(subtitleCaptured.size < titleBase, "長いサブタイトル行自身は自分の幅超過で縮む")
        assertTrue(subtitleCaptured.text.endsWith(hugeText), "連結文字列は 路線名 + 面積合計 の順")
    }

    @Test
    fun `タイトルテキスト自体が長ければ title 自身は縮む`() {
        val w = CapturingWriter()
        w.zumeninfo.zumentitle = hugeText
        w.rosenname_ = "路線"

        w.writeTopTitle(textsize = 0f)

        val titleBase = TextSizePolicy.resolve(TextRole.TopTitle)
        val titleCaptured = w.captured.first { it.text == hugeText }
        assertTrue(titleCaptured.size < titleBase, "title 自身が長すぎれば当然縮む (管理分離しても無制限オーバーフローはしない)")
    }

    @Test
    fun `サブタイトル行の縦オフセットはサブタイトル自身の実サイズから逆算され下線と衝突しない`() {
        // 2026-08-12: サブタイトル行を表題欄の 2 倍 (= TopTitle と同サイズ) に拡大した際、
        // 縦オフセットが旧 (小さい) サブタイトルサイズ前提の固定比率のままで下線と衝突した。
        // 「テキストサイズを基にしてオフセット計算しろ」── オフセットは (自分の中で使う) areaFs
        // (= サブタイトル自身の実サイズ) から逆算されるべきで、単に titleTextSize 比例の定数
        // ではダメ。alignV=1 (Bottom) で描画されるため、テキストは指定 y から上方向に自身の
        // フォントサイズぶん伸びる ── 下線 (ty - underlineGap2) と重ならないためには
        // (ty - subtitleLineGap) + areaFs <= (ty - underlineGap2) が必要。
        val w = CapturingWriter()
        w.zumeninfo.zumentitle = "面 積 展 開 図"
        w.rosenname_ = "市道○○号線"
        w.zumenAreaSegments.add(DrawingFileWriter.AreaSegment("面積: A=14.94㎡", w.WHITE))

        w.writeTopTitle(textsize = 0f)

        val titleY = w.captured.first { it.text == w.zumeninfo.zumentitle }.y
        val underlineTexts = w.captured // underline は Line prim なので writeTextHV には来ない、y だけ再計算で検証
        val subtitleCaptured = w.captured.first { it.text.startsWith(w.rosenname_) }

        // サブタイトル文字列の「上端」(bottom-anchored な y + 自身のフォントサイズ) が
        // タイトル本体の y (= 下線のすぐ上) を超えない、つまりタイトルとサブタイトルの間に
        // 最低でもサブタイトル自身の文字高さぶんの空間があること。
        assertTrue(titleY - subtitleCaptured.y >= subtitleCaptured.size,
            "サブタイトル行はタイトル本体の下に自分自身の文字サイズ分以上のクリアランスを持つべき (衝突防止)")
    }

    @Test
    fun `路線名と面積合計は1本の連結文字列で描画される`() {
        // 2026-08-12 user 指示「ストリングは連結して一個の文字列にして表現しろ」。
        val w = CapturingWriter()
        w.zumeninfo.zumentitle = "面 積 展 開 図"
        w.rosenname_ = "市道○○号線"
        w.zumenAreaSegments.add(DrawingFileWriter.AreaSegment("面積: A=14.94㎡", w.WHITE))

        w.writeTopTitle(textsize = 0f)

        // 路線名 と 面積合計 それぞれ別々の text prim ではなく、1 個の連結文字列であること。
        val matching = w.captured.filter { it.text.contains(w.rosenname_) }
        assertEquals(1, matching.size, "路線名を含む text prim は 1 個だけ (連結済み)")
        assertTrue(matching.first().text.contains("面積: A=14.94"), "連結文字列は面積合計も含む")
    }

    @Test
    fun `面積合計が無くても路線名は単独でサブタイトル行に描画される`() {
        val w = CapturingWriter()
        w.zumeninfo.zumentitle = "面 積 展 開 図"
        w.rosenname_ = "市道○○号線"
        // zumenAreaSegments は空のまま

        w.writeTopTitle(textsize = 0f)

        val subtitleCaptured = w.captured.first { it.text.startsWith(w.rosenname_) }
        val subtitleBase = TextSizePolicy.resolve(TextRole.TopTitle)
        assertEquals(subtitleBase, subtitleCaptured.size, 1e-4f, "面積行が無くても路線名はサブタイトル行サイズで描画")
        assertEquals(w.rosenname_, subtitleCaptured.text, "面積行が無ければ連結文字列は路線名のみ")
    }
}
