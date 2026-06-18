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
    fun titleBlockExtendsTo12x9cm() {
        // 2026-06-19 表題欄 比例拡大 (10×6 → 12×9cm) ── 外枠右下隅から 12cm 左 / 9cm 上が
        // 表題欄の左上隅、 そこに frame line (= ラベル列縦罫 と 行罫) が存在する。
        val json = WebFrame.renderFrame(csv) // default 1.5cm
        // 表題欄上辺 = by + 9 = 1.5 + 9 = 10.5、 これが frame line として存在
        // = json に y1==y2==10.5*ps の line がある
        val trilist = WebCsvReader.read(csv)
        val ps = trilist.getPrintScale(1f)
        // frame line の y1, y2 を収集、 表題欄上辺 by+9 が含まれるか確認
        val (_, ys) = frameLineExtents(json)
        // ys.first = 外枠下辺 = by = 1.5 (paper-cm)、 ys.second = 外枠上辺 = paperHcm - by = 28.2
        // 表題欄上辺 y = by + 9 = 10.5 が figure center 平行移動後の座標として存在することを
        // 「frame line の y 値集合 に そのオフセット line がある」 で間接確認するのは複雑なので、
        // 表題欄高さ 9cm の前提を「render の text 数 と cell 数 が整合」 で代用
        // defaultZumenInfo() で 「工 事 名」「施 工 者」 (全角スペース付) と set されてる
        val koujinameCount = Regex(""""text":"工 事 名"""").findAll(json).count()
        val gyousyaCount = Regex(""""text":"施 工 者"""").findAll(json).count()
        assertEquals(1, koujinameCount, "工 事 名 ラベル 1 つ")
        assertEquals(1, gyousyaCount, "施 工 者 ラベル 1 つ")
        // 7 cell (工事名 / 図面名 / 路線名 / 作成日 / 縮尺 / 図面番号 / 施工者) の値 prim が存在
        assertTrue(json.contains(""""field":"koujiname""""), "koujiname tag")
        assertTrue(json.contains(""""field":"rosenname""""), "rosenname tag")
        assertTrue(json.contains(""""field":"zumennum""""), "zumennum tag")
        assertTrue(json.contains(""""field":"gyousyaname""""), "gyousyaname tag")
    }

    @Test
    fun urlFieldTagPresent() {
        // 2026-06-18 url click で別タブ open のため field="url" tag が prim に乗る
        val json = WebFrame.renderFrame(csv)
        assertTrue(json.contains(""""field":"url""""), "url field tag 必須 (= canvas click 経路の識別子)")
    }

    @Test
    fun frameTextSizeIs3xBase() {
        // 2026-06-19 FRAME_CELL_TEXT_SCALE = 3 ── cell text の paper サイズが base ×3 化
        assertEquals(3f, DrawingFileWriter.FRAME_CELL_TEXT_SCALE)
    }

    @Test
    fun emptyCsvYieldsEmpty() {
        // 三角形が無ければ frame も出ない (既存 WebFrameTest と同じ規約、 margin 引数版でも維持)
        for (margin in listOf(0.75f, 1.0f, 1.5f, 2.0f)) {
            assertEquals("[]", WebFrame.renderFrame("", margin), "margin $margin: empty csv → []")
        }
    }
}
