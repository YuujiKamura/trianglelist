package com.jpaver.trianglelist.datamanager

import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.ZumenInfo
import com.jpaver.trianglelist.viewmodel.TitleParamStr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 2026-08-12 user 指示「上部タイトルはもっとでかくしたい。管理を分けたほうが良い」。
 *
 * writeTopTitle() は旧実装で title / 面積合計行 / 路線名 の 3 者の最大幅から単一の shrink 係数を
 * 算出し、title と面積合計行 (areaFs) 両方に同じ係数を掛けていた。面積合計行は色分け内訳が
 * 増えると簡単に長くなるため、title 自体は短くても area 行の長さに引きずられて縮んでいた
 * (= 「管理が分かれていない」)。本 test は title(+路線名) の shrink 判定が area 行の幅を
 * 一切参照しないことを pin する。writeTextHV をキャプチャする最小サブクラスで DXF/SFC 等の
 * backend 差を排除し、DrawingFileWriter 本体のロジックだけを検証する。
 */
class TopTitleSizingTest {

    private class CapturingWriter : DrawingFileWriter() {
        override var trilist_: TriangleList = TriangleList()
        override var dedlist_: DeductionList = DeductionList()
        override var zumeninfo: ZumenInfo = ZumenInfo()
        override var titleTri_: TitleParamStr = TitleParamStr()
        override var titleDed_: TitleParamStr = TitleParamStr()

        data class Captured(val text: String, val size: Float, val y: Float)
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
            captured.add(Captured(text, textsize, point.y.toFloat()))
        }
    }

    // 箱幅 (maxWidth = paperWcm - outerMarginCm*2 、既定 A3/1.5cm なら 39cm) を実測メトリクス問わず
    // 確実に超過させるための巨大文字列。全角換算 1 文字でも 200 個あれば base サイズ (最大でも
    // TITLE=1.0cm) でどう見積もっても 39cm を超える (200 * 0.5cm 半角近似でも 100cm > 39cm)。
    private val hugeText = "あ".repeat(200)

    @Test
    fun `短いタイトルは面積合計行が長くても base サイズのまま`() {
        val w = CapturingWriter()
        w.zumeninfo.zumentitle = "図"
        w.rosenname_ = "線"
        w.zumenAreaSegments.add(DrawingFileWriter.AreaSegment(hugeText, w.WHITE))

        w.writeTopTitle(textsize = 0f)

        val titleBase = TextSizePolicy.resolve(TextRole.TopTitle)
        val titleCaptured = w.captured.first { it.text == "図" }
        assertEquals(titleBase, titleCaptured.size, 1e-4f,
            "面積合計行が長くても title 自体が箱に収まるなら base サイズを維持すべき (管理分離)")
    }

    @Test
    fun `面積合計行が長ければ area 行だけが縮み title は縮まない`() {
        val w = CapturingWriter()
        w.zumeninfo.zumentitle = "面 積 展 開 図"
        w.rosenname_ = "市道○○号線"
        w.zumenAreaSegments.add(DrawingFileWriter.AreaSegment(hugeText, w.WHITE))

        w.writeTopTitle(textsize = 0f)

        val titleBase = TextSizePolicy.resolve(TextRole.TopTitle)
        val areaBase = TextSizePolicy.resolve(TextRole.BottomTitleFrame)
        val titleCaptured = w.captured.first { it.text == w.zumeninfo.zumentitle }
        val areaCaptured = w.captured.first { it.text == hugeText }

        assertEquals(titleBase, titleCaptured.size, 1e-4f, "title は area 行の長さに引きずられない")
        assertTrue(areaCaptured.size < areaBase, "長い area 行自身は自分の幅超過で縮む")
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
    fun `面積合計行はタイトル本体と衝突しない縦間隔を保つ (タイトルサイズに比例)`() {
        // 2026-08-12、実際に CADWe'll/desktop viewer で目視して発見: タイトルを 7→10mm に拡大した際、
        // 面積合計行・下線・路線名の縦オフセットが旧 titleTextSize=0.7cm 時代の固定 cm 値のままだった
        // ため、拡大後のタイトル本体と面積合計行 ("面積: A=...") が視覚的に重なった。
        // 「箱の幅」は TextFit で守られていたが「縦の間隔」は守られていなかった、という別種の穴。
        val w = CapturingWriter()
        w.zumeninfo.zumentitle = "面 積 展 開 図"
        w.rosenname_ = "市道○○号線"
        w.zumenAreaSegments.add(DrawingFileWriter.AreaSegment("面積: A=14.94㎡", w.WHITE))

        w.writeTopTitle(textsize = 0f)

        val titleTextSize = TextSizePolicy.resolve(TextRole.TopTitle)
        val titleY = w.captured.first { it.text == w.zumeninfo.zumentitle }.y
        val areaY = w.captured.first { it.text.startsWith("面積: A=") }.y
        val rosennameY = w.captured.first { it.text == w.rosenname_ }.y

        assertTrue(titleY - areaY >= titleTextSize,
            "面積合計行はタイトル本体の下に titleTextSize 分以上のクリアランスを持つべき (衝突防止)")
        assertTrue(areaY > rosennameY, "路線名は面積合計行よりさらに下に位置する")
    }
}
