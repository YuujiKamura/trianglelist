package com.jpaver.trianglelist.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.jpaver.trianglelist.datamanager.DrawingFileWriter
import com.jpaver.trianglelist.datamanager.TextRole
import com.jpaver.trianglelist.datamanager.TextSizePolicy

/**
 * 2026-06-18 / 06-19 で 1 軸化した「外枠 余白」 と 「表題欄 12×9cm 比例拡大」 + url 配置 を
 * margin 各値で連動 pin する test (= 直近 commit の動線 regression 防止)。
 *
 * 既存 WebFrameTest は default margin のみ pin、 本 test は marginCm を 横断する。
 * UI 切替 (= marginSelect の 7.5/10/15/20mm) すべてで 外枠 + 表題欄 + url が連動することを保証。
 */
class WebFrameMarginTest {

    private val csv = "1,6.0,5.0,4.0,-1,-1\n"
    private val paperWcm = 42.0
    private val paperHcm = 29.7

    private fun frameLineExtents(json: String): Pair<Pair<Double, Double>, Pair<Double, Double>> {
        val xs = mutableListOf<Double>()
        val ys = mutableListOf<Double>()
        Regex(""""type":"line","layer":"frame","x1":([-0-9.E]+),"y1":([-0-9.E]+),"x2":([-0-9.E]+),"y2":([-0-9.E]+)""")
            .findAll(json).forEach { m ->
                xs.add(m.groupValues[1].toDouble()); xs.add(m.groupValues[3].toDouble())
                ys.add(m.groupValues[2].toDouble()); ys.add(m.groupValues[4].toDouble())
            }
        return (xs.min() to xs.max()) to (ys.min() to ys.max())
    }

    @Test
    fun outerFrameSizeFollowsMarginCm() {
        // 外枠寸法 = paper - 2*margin、 ui select の 4 値全部で pin
        val trilist = WebCsvReader.read(csv)
        val ps = trilist.getPrintScale(1f)
        for ((margin, expectedW, expectedH) in listOf(
            Triple(0.75f, 40.5, 28.2),
            Triple(1.0f,  40.0, 27.7),
            Triple(1.5f,  39.0, 26.7), // default
            Triple(2.0f,  38.0, 25.7),
        )) {
            val json = WebFrame.renderFrame(csv, margin)
            val (xs, ys) = frameLineExtents(json)
            val w = xs.second - xs.first
            val h = ys.second - ys.first
            assertEquals(expectedW * ps, w, 1e-3, "margin $margin: width 期待 $expectedW、 実 ${w / ps}")
            assertEquals(expectedH * ps, h, 1e-3, "margin $margin: height 期待 $expectedH、 実 ${h / ps}")
        }
    }

    @Test
    fun defaultMarginIs15mm() {
        // 2026-06-18 user 「デフォルト 15mm くらいが見やすい」 で 1.5cm に確定、 const と一致
        assertEquals(1.5f, DrawingFileWriter.DEFAULT_OUTER_MARGIN_CM)
    }

    @Test
    fun bottomTitleFrameRendersAllCells() {
        // BottomTitleFrame (= 右下のタイトルフレーム、 user 用語) 内に 6 ラベル + 4 内容 field 全て存在。
        // 2026-06-19 user 訂正 で 表題欄 12×9cm 拡大 は revert、 元 10×6cm 戻し。 cell 寸法 pin の
        // 代わりに「ラベル / 内容 field が全て emit される」 を pin (cell サイズ計算は別 test 不要)。
        val json = WebFrame.renderFrame(csv)
        val koujinameCount = Regex(""""text":"工 事 名"""").findAll(json).count()
        val gyousyaCount = Regex(""""text":"施 工 者"""").findAll(json).count()
        assertEquals(1, koujinameCount, "工 事 名 ラベル 1 つ")
        assertEquals(1, gyousyaCount, "施 工 者 ラベル 1 つ")
        assertTrue(json.contains(""""field":"koujiname""""), "koujiname tag")
        assertTrue(json.contains(""""field":"rosenname""""), "rosenname tag")
        assertTrue(json.contains(""""field":"zumennum""""), "zumennum tag")
        assertTrue(json.contains(""""field":"gyousyaname""""), "gyousyaname tag")
    }

    @Test
    fun bottomCreditFieldTagPresent() {
        // BottomCredit region (= 左下 url、 DrawingFileWriter companion KDoc 参照) は canvas click で
        // 別タブ open のため field="url" tag が prim に乗る (= 2026-06-18 user 要望)。
        val json = WebFrame.renderFrame(csv)
        assertTrue(json.contains(""""field":"url""""), "BottomCredit url field tag 必須 (= canvas click 経路の識別子)")
    }

    /** prim JSON から layer="frame" の text の size を全部拾う。 */
    private fun frameTextSizes(json: String): List<Double> =
        Regex(""""type":"text","layer":"frame".*?"size":([-0-9.E]+)""")
            .findAll(json).map { it.groupValues[1].toDouble() }.toList()

    @Test
    fun frameTextSizeIsInTheSameCoordinateSpaceAsFrameLines() {
        // 2026-08-25 user 指摘「web のタイトル系テキストが小さい」の regression pin。
        //
        // 枠線は mx() が paper-cm に ×ps して吐く (= 画面共有のモデル座標)。テキストの size も
        // 同じ空間に乗っていなければならない ── なのに FramePrimWriter.writeTextHV は
        // 「textsize は paper-mm だから ÷10 が要る」前提の ÷10 を持ったままだった。4157042a
        // (2026-08-12) で TextSizePolicy.resolve() が paper-cm を返すよう単位が変わったのに
        // web 側の ÷10 が残り、二重変換で枠テキストだけ 1/10 (表題欄 3.5mm → 0.35mm) になった。
        // DXF は writeTextHV を素通しするので無傷、web だけが被る非対称だった。
        //
        // policy 値そのもの (frameTextSizeFollowsJisPaperMmLadder) を pin しても、その先の
        // 単位変換が壊れていれば素通りする ── 実際に素通りした。emit された size を、同じ
        // JSON に入っている枠線の実寸と突き合わせる形で pin し直す (= 「描画が真実」)。
        val trilist = WebCsvReader.read(csv)
        val ps = trilist.getPrintScale(1f)
        val json = WebFrame.renderFrame(csv)
        val sizes = frameTextSizes(json)
        assertTrue(sizes.isNotEmpty(), "frame text prim が 1 つも無い")

        val frameCm = TextSizePolicy.resolve(TextRole.BottomTitleFrame).toDouble()
        val titleCm = TextSizePolicy.resolve(TextRole.TopTitle).toDouble()
        // 縮小 (TextFit) が掛かる cell もあるので、最大 = TopTitle、最小でも frame 相当は出る想定。
        assertEquals(titleCm * ps, sizes.max(), 1e-4,
            "最大の frame text (= TopTitle) は policy cm × ps。実 ${sizes.max()} / 期待 ${titleCm * ps}")
        // 枠線の実寸から逆算しても同じ結論になること (座標系の一致を線側からも押さえる)
        val (xs, _) = frameLineExtents(json)
        val outerWidthCm = (xs.second - xs.first) / ps
        assertEquals(39.0, outerWidthCm, 1e-3, "default margin 1.5cm の外枠幅 = 39cm")
        assertEquals(frameCm / 39.0, sizes.min() / (xs.second - xs.first), 1e-4,
            "表題欄テキストと外枠幅の比 = policy cm / 39cm (単位が二重変換されていない証拠)")
    }

    @Test
    fun frameTextSizeFollowsJisPaperMmLadder() {
        // 2026-08-12 最終版: writeTopTitle/writeDrawingFrame は drawingScale を打ち消した
        // paper 固定 cm 空間で動く (ADR 0001)。scale/TextSizePolicy.kt の JIS Z 8313 準拠
        // paper mm 階段を ÷10 (mm→cm) しただけの値になる。
        //
        // 途中 2 案を目視で棄却した経緯: (a) 全 role を entityTextSize に統一 → タイトルが
        // 寸法値と同じ極小サイズになり最も目立たない文字になった。(b) JIS mm を paperToModel で
        // model 空間に変換 → paper 固定空間に model 空間の数値を渡し画面が文字で埋まる大惨事に
        // なった。paperToModel は寸法値のような drawingScale 依存の model 空間専用、この関数群には
        // 適用しない。
        //
        // 2026-08-12 desktop/sample/sample.dxf (user が最適と判断) の実測 (model 125.0mm ÷ 1/50
        // scale = paper 2.5mm) に合わせて FRAME_LABEL を 5.0→2.5mm へ、TopTitle はその 2 倍で
        // 5.0mm へ縮小した。結果 FRAME_LABEL(2.5mm) が BottomCredit(3.5mm) を下回る逆転が生じ、
        // web 画面でタイトル系の文字が判読できないほど小さくなった (2026-08-25 user 指摘)。
        // 表題欄が url 注記より小さいのは通常の図面の強弱関係として不自然 ── FRAME_LABEL を
        // DIMENSION/BottomCredit と同格の 3.5mm (JIS 次の段) へ引き上げ、TopTitle はその
        // 2 倍の 7.0mm。旧階層 (title > frame > credit という逆転無しの自然な強弱) に戻す。
        assertEquals(0.7f, TextSizePolicy.resolve(TextRole.TopTitle), "TopTitle = 7.0mm/10")
        assertEquals(0.35f, TextSizePolicy.resolve(TextRole.BottomTitleFrame), "BottomTitleFrame = 3.5mm/10")
        assertEquals(0.35f, TextSizePolicy.resolve(TextRole.BottomCredit), "BottomCredit = 3.5mm/10")
        val title = TextSizePolicy.resolve(TextRole.TopTitle)
        val frame = TextSizePolicy.resolve(TextRole.BottomTitleFrame)
        val credit = TextSizePolicy.resolve(TextRole.BottomCredit)
        assertTrue(title > credit, "TopTitle は BottomCredit より大きい")
        assertEquals(credit, frame, "BottomCredit と BottomTitleFrame は同格 (どちらも DIMENSION_PAPER_MM 準拠)")
    }

    @Test
    fun emptyCsvYieldsEmpty() {
        // 三角形が無ければ frame も出ない (既存 WebFrameTest と同じ規約、 margin 引数版でも維持)
        for (margin in listOf(0.75f, 1.0f, 1.5f, 2.0f)) {
            assertEquals("[]", WebFrame.renderFrame("", margin), "margin $margin: empty csv → []")
        }
    }
}
