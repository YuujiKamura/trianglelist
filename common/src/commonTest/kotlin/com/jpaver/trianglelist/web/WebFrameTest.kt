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
        assertEquals(40.0 * ps, xs.max() - xs.min(), 1e-3)
        assertEquals(27.0 * ps, ys.max() - ys.min(), 1e-3)
        assertEquals(center.x.toDouble(), (xs.max() + xs.min()) / 2, 1e-3)
        assertEquals(center.y.toDouble(), (ys.max() + ys.min()) / 2, 1e-3)
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
