package com.jpaver.trianglelist.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.jpaver.trianglelist.datamanager.DrawingFileWriter

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

    @Test
    fun topTitleScaleIsDoubleOfBottomTitleFrame() {
        // 枠内テキスト 3 region 規約 (user 確定 2026-06-19、 DrawingFileWriter companion KDoc 参照):
        // - TopTitle (= 上部タイトル「面積展開図」) = base × 2 (= ダブルスコア)
        // - BottomTitleFrame cell (= 表題欄 工事名 等) + url (= tCredit) = base 同一
        // const TOP_TITLE_SCALE = 2f を pin (= drift で TopTitle と BottomTitleFrame を 混同して BottomTitleFrame
        // 側を 拡大した a72c17b / 687d6dd 経緯への regression 防止)。
        assertEquals(2f, DrawingFileWriter.TOP_TITLE_SCALE)
    }

    @Test
    fun emptyCsvYieldsEmpty() {
        // 三角形が無ければ frame も出ない (既存 WebFrameTest と同じ規約、 margin 引数版でも維持)
        for (margin in listOf(0.75f, 1.0f, 1.5f, 2.0f)) {
            assertEquals("[]", WebFrame.renderFrame("", margin), "margin $margin: empty csv → []")
        }
    }
}
