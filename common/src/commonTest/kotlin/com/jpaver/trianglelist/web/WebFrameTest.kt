package com.jpaver.trianglelist.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WebFrame.renderFrame の pin。形の正は DrawingFileWriter.writeDrawingFrame
 * (外枠 40x27cm 中心 (21,14.85)) — ここでは「図形中心に枠中心が一致する」
 * 「枠の縦横が 40:27 × printscale になる」の相対配置だけ機械固定する。
 */
class WebFrameTest {

    private val csv = "1,6.0,5.0,4.0,-1,-1\n"

    @Test
    fun frame_outer_rect_centers_on_triangle_and_scales_with_printscale() {
        val trilist = WebCsvReader.read(csv)
        val ps = trilist.getPrintScale(1f)
        val center = trilist.center
        val json = WebFrame.renderFrame(csv)
        assertTrue(json.contains(""""layer":"frame""""), "frame prims expected: $json")

        // line prim の x/y 全域 = 外枠 (40x27cm × ps)、中心 = 図形中心
        val xs = mutableListOf<Double>()
        val ys = mutableListOf<Double>()
        Regex(""""type":"line","layer":"frame","x1":([-0-9.E]+),"y1":([-0-9.E]+),"x2":([-0-9.E]+),"y2":([-0-9.E]+)""")
            .findAll(json).forEach { m ->
                xs.add(m.groupValues[1].toDouble()); xs.add(m.groupValues[3].toDouble())
                ys.add(m.groupValues[2].toDouble()); ys.add(m.groupValues[4].toDouble())
            }
        assertTrue(xs.isNotEmpty(), "frame lines expected")
        // 外枠寸法: 用紙端から OUTER_MARGIN_CM (= 2.0cm = A1 基準) 余白、 A3 (42×29.7cm) で 38×25.7cm。
        // 2026-06-18 user 指示「A1 とかの厳しい基準を当てておく」 ── 同じ図面を A3 / A1 両方で
        // 納品し得るので A1 基準 (= 20mm) を採用。 旧 40×27 / 40.5×28.2 から変更。
        assertEquals(38.0 * ps, xs.max() - xs.min(), 1e-3)
        assertEquals(25.7 * ps, ys.max() - ys.min(), 1e-3)
        assertEquals(center.x.toDouble(), (xs.max() + xs.min()) / 2, 1e-3)
        assertEquals(center.y.toDouble(), (ys.max() + ys.min()) / 2, 1e-3)
    }

    @Test
    fun frame_center_matches_mixed_figure_center() {
        // user 指摘 2026-06-14「混在リスト全体の境界計算してセンタリングするのが出来てない」の pin。
        // 三角形 + 子台形 (混在) のリストで、枠 bbox 中心 = 全 figure (tri layer 線) bbox 中心 を満たす。
        // trilist.center だけでは台形・台形子三角形が無視され、figure の重心と枠中心がずれる。
        val mixedCsv = "1,6.0,5.0,4.0,-1,-1\nRectangle,1,5,10,7,1,1\n"

        // 全 figure (tri layer 線) の bbox 中心 — renderCsv の出力を直接解析
        val figJson = WebPrimitiveRenderer.renderCsv(mixedCsv, 1f)
        val figXs = mutableListOf<Double>(); val figYs = mutableListOf<Double>()
        Regex(""""type":"line","layer":"tri","x1":([-0-9.E]+),"y1":([-0-9.E]+),"x2":([-0-9.E]+),"y2":([-0-9.E]+)""")
            .findAll(figJson).forEach { m ->
                figXs.add(m.groupValues[1].toDouble()); figXs.add(m.groupValues[3].toDouble())
                figYs.add(m.groupValues[2].toDouble()); figYs.add(m.groupValues[4].toDouble())
            }
        assertTrue(figXs.isNotEmpty(), "figure tri lines expected: $figJson")
        val figCx = (figXs.max() + figXs.min()) / 2
        val figCy = (figYs.max() + figYs.min()) / 2

        // 枠 bbox 中心
        val frJson = WebFrame.renderFrame(mixedCsv)
        val frXs = mutableListOf<Double>(); val frYs = mutableListOf<Double>()
        Regex(""""type":"line","layer":"frame","x1":([-0-9.E]+),"y1":([-0-9.E]+),"x2":([-0-9.E]+),"y2":([-0-9.E]+)""")
            .findAll(frJson).forEach { m ->
                frXs.add(m.groupValues[1].toDouble()); frXs.add(m.groupValues[3].toDouble())
                frYs.add(m.groupValues[2].toDouble()); frYs.add(m.groupValues[4].toDouble())
            }
        assertTrue(frXs.isNotEmpty(), "frame lines expected")
        val frCx = (frXs.max() + frXs.min()) / 2
        val frCy = (frYs.max() + frYs.min()) / 2

        // 枠中心 = figure 全体中心 (混在リストの境界計算が正しく効いてる)
        assertEquals(figCx, frCx, 1e-3, "枠 x 中心が figure 全体 x 中心と一致 (frame=$frCx, fig=$figCx)")
        assertEquals(figCy, frCy, 1e-3, "枠 y 中心が figure 全体 y 中心と一致 (frame=$frCy, fig=$figCy)")
    }

    @Test
    fun parse_header_skips_meta_rows() {
        // web の serializeState はヘッダの後に ListAngle/TextSize/Deduction 行を出す。
        // ヘッダ 4 行が埋まっていないとき、これらをヘッダ値として誤読しない
        // (誤読すると図面番号欄に "TextSize, 30" が印字される — 2026-06-12 画面目視で発見)
        val csvWithMeta = "無題工事\n新規路線\nListAngle, 0\nTextSize, 30\n" + csv +
            "Deduction,1,仕切弁,0.23,0.0,1,Circle,0.0,1.0,0.0,2.0,0.0,0.0\n"
        val h = WebDrawingExport.parseHeader(csvWithMeta)
        assertEquals("無題工事", h.koujiname)
        assertEquals("新規路線", h.rosenname)
        assertEquals("", h.gyousyaname)
        assertEquals("", h.zumennum)
    }

    @Test
    fun frame_tags_title_fields_and_emits_blank_cells() {
        // タイトル欄の値テキストは field タグ付き、空欄でも prim が出る —
        // web の「空白デフォルト + 枠内 dblclick で書換え」(2026-06-12) の前提
        val labeled = "koujiname,テスト工事\nrosenname,\ngyousyaname,\nzumennum,\n" + csv
        val json = WebFrame.renderFrame(labeled)
        assertTrue(json.contains(""""text":"テスト工事"""), "koujiname value expected: $json")
        assertTrue(json.contains(""""field":"koujiname""""), "koujiname tag expected")
        assertTrue(json.contains(""""field":"rosenname""""), "blank rosenname cell expected")
        assertTrue(json.contains(""""field":"gyousyaname""""), "blank gyousyaname cell expected")
        assertTrue(json.contains(""""field":"zumennum""""), "blank zumennum cell expected")
        // 固定ラベル (題字) にはタグが付かない
        assertTrue(!json.contains(""""text":"図 面 名","x":[^,]+,"y":[^,]+,[^}]*"field"""".toRegex()))
    }

    @Test
    fun frame_carries_header_titles_and_empty_csv_yields_empty() {
        // ヘッダ付き CSV → 工事名・路線名が題字テキストに乗る (writeDrawingFrame の内容欄)
        val withHeader = "テスト工事\n路線A\n業者B\n1/1\n" + csv
        val json = WebFrame.renderFrame(withHeader)
        assertTrue(json.contains("テスト工事"), "koujiname expected: $json")
        assertTrue(json.contains("路線A"), "rosenname expected")
        assertTrue(json.contains("面 積 展 開 図"), "zumen title expected")
        // 三角形が無ければ空
        assertEquals("[]", WebFrame.renderFrame(""))
    }
}
