package com.jpaver.trianglelist.web

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 混在リスト段1 (B1): CSV の Rectangle 行 → Rectangle 構築 → Rectangle (台形) 描画のテスト。
 * プロジェクト統一規約: 全図形 時計回り (CW)、 br (Bottom-Right) 起点。
 */
class WebTrapezoidTest {

    private fun figs(doc: CsvCodec.CsvDoc, trilist: TriangleList, scale: Float = 1f): Pair<List<Rectangle>, List<Triangle>> {
        val mixed = CsvCodec.buildMixed(doc, trilist, scale)
        val all = (1..mixed.size()).map { mixed.get(it) }
        val rects = all.filterIsInstance<Rectangle>()
        val trilistTris: Set<Triangle> = (1..trilist.size()).map { trilist.getBy(it) }.toSet()
        val rectChildTris = all.filterIsInstance<Triangle>().filter { it !in trilistTris }
        return Pair(rects, rectChildTris)
    }

    private fun count(haystack: String, needle: String): Int {
        var c = 0
        var i = haystack.indexOf(needle)
        while (i >= 0) {
            c++
            i = haystack.indexOf(needle, i + needle.length)
        }
        return c
    }

    @Test
    fun independent_rectangle_emits_four_tri_lines() {
        val json = WebPrimitiveRenderer.renderCsv("Rectangle,1,5,4,3,-1,0\n", 1f)
        assertEquals(4, count(json, """"layer":"tri""""), "Rectangle は 4 辺ちょうど")
        assertEquals(1, count(json, """"type":"circle""""))
        assertTrue(json.contains(""""text":"1""""))
        assertEquals(3, count(json, """"layer":"dim""""), "底辺A・上辺C・延長B の 3 寸法")
    }

    /** 幾何精度テスト: br 起点 CW なら、 angle=180 で上向きに展開される */
    @Test
    fun independent_rectangle_geometry_matches_rectangle() {
        val doc = CsvCodec.parse("Rectangle,1,5,4,3,-1,0\n")
        val trilist = CsvCodec.build(doc)
        val (rects, _) = figs(doc, trilist, 1f)
        val lp = rects[0].calcPoint()
        
        // br 起点 (0,0)、 angle=180 (INDEP_TRAP_ANGLE)
        // bl = br + (4, 180deg) = (-4, 0)
        // perp = br->bl 右手 90deg = (0, 5)
        // tl = bl + perp = (-4, 5)
        // tr = br + perp = (0, 5)
        assertEquals(-4.0, lp.a.left.x, 0.001, "bl.x")
        assertEquals(0.0, lp.a.right.x, 0.001, "br.x")
        assertEquals(5.0, lp.b.left.y, 0.001, "tl.y (上向き成長)")
        assertEquals(3.0, lp.b.left.lengthTo(lp.b.right), 0.001, "上辺長 widthB")
    }

    @Test
    fun connected_rectangle_base_lies_on_parent_b_edge() {
        val csv = "1,6.0,5.0,4.0,-1,-1\nRectangle,1,5,4,3,1,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (rects, _) = figs(doc, trilist, 1f)
        val lp = rects[0].calcPoint()
        val parent = trilist.getBy(1)
        val onEdge = (lp.a.left.nearBy(parent.pointAB, 0.001) && lp.a.right.nearBy(parent.pointBC, 0.001)) ||
                     (lp.a.left.nearBy(parent.pointBC, 0.001) && lp.a.right.nearBy(parent.pointAB, 0.001))
        assertTrue(onEdge)
    }

    @Test
    fun triangle_can_be_child_of_rectangle() {
        val csv = "Rectangle,1,5,10,7,-1,0\n2,7,4,4,1,2\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (rects, tris) = figs(doc, trilist, 1f)
        val edge = rects[0].getLine(2) // 上辺C
        val tri = tris[0]
        val onEdge = (tri.point[0].nearBy(edge.left, 0.001) && tri.pointAB.nearBy(edge.right, 0.001)) ||
            (tri.point[0].nearBy(edge.right, 0.001) && tri.pointAB.nearBy(edge.left, 0.001))
        assertTrue(onEdge)
    }

    @Test
    fun triangle_child_of_rectangle_on_d_edge() {
        val csv = "Rectangle,1,5,10,7,-1,0\n2,0,4,4,1,3\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (rects, tris) = figs(doc, trilist, 1f)
        val dEdge = rects[0].getLine(3) // D 右脚
        val tri = tris[0]
        val onD = (tri.point[0].nearBy(dEdge.left, 0.001) && tri.pointAB.nearBy(dEdge.right, 0.001)) ||
            (tri.point[0].nearBy(dEdge.right, 0.001) && tri.pointAB.nearBy(dEdge.left, 0.001))
        assertTrue(onD)
    }

    // ---- アライメントテスト ----

    @Test
    fun alignment_left_keeps_top_left_above_base_left() {
        // br=(0,0), angle=180 で bl=(-10,0)。 左寄せなら tl が bl の真上。
        val lp = Rectangle(3.0, 10.0, 4.0, angle = 180.0, alignment = 0).calcPoint()
        assertEquals(-10.0, lp.b.left.x, 0.001, "左寄せ: tl.x = bl.x = -10")
        assertEquals(3.0, lp.b.left.y, 0.001, "上辺高さ 3")
    }

    @Test
    fun alignment_center_is_symmetric() {
        val lp = Rectangle(3.0, 10.0, 4.0, angle = 180.0, alignment = 1).calcPoint()
        // 底辺センター = -5。 上辺幅 4 なので tl = -5 - 2 = -7, tr = -5 + 2 = -3
        assertEquals(-7.0, lp.b.left.x, 0.001, "中央: tl.x = -7")
        assertEquals(-3.0, lp.b.right.x, 0.001, "中央: tr.x = -3")
    }

    @Test
    fun alignment_right_keeps_top_right_above_base_right() {
        val lp = Rectangle(3.0, 10.0, 4.0, angle = 180.0, alignment = 2).calcPoint()
        assertEquals(0.0, lp.b.right.x, 0.001, "右寄せ: tr.x = br.x = 0")
        assertEquals(-4.0, lp.b.left.x, 0.001, "右寄せ: tl.x = -4")
    }

    // ---- DXF/SFC 座標テスト (実寸 × 1000) ----

    @Test
    fun dxf_rectangle_line_coordinates_match_geometry() {
        val dxf = WebDrawingExport.buildDxfText("1,3.0,4.0,3.0,-1,-1\nRectangle,1,3,4,2,-1,0\n", "")
        // 浮動小数点誤差を許容する正規表現。 0.0 は 1.23E-13 等になる可能性がある
        val num = """-?(\d+\.\d+|\d+\.\d+[eE]-?\d+|0\.0|0)"""
        // 底辺A: br->bl = (0,0) -> (-4000,0)
        val reA = Regex("""10\n$num\n20\n$num\n30\n0\.0\n11\n-4000\.0\n21\n$num""")
        assertTrue(reA.containsMatchIn(dxf), "底辺A")
        // 右脚D: tr->br = (0,3000) -> (0,0)
        val reD = Regex("""10\n$num\n20\n(2999|3000)[^\n]*\n30\n0\.0\n11\n$num\n21\n$num""")
        assertTrue(reD.containsMatchIn(dxf), "右脚D")
    }

    @Test
    fun sfc_rectangle_line_coordinates_match_geometry() {
        val sfc = WebDrawingExport.buildSfcText("1,3.0,4.0,3.0,-1,-1\nRectangle,1,3,4,2,-1,0\n", "t.sfc")
        val num = """-?([0-9.]+|[0-9.]+[eE]-?[0-9]+)"""
        // br->bl = (0,0)->(-4000,0)
        val reA = Regex("""line_feature\('[^']+','[^']+','[^']+','[^']+','$num','$num','-4000\.0','$num'\)""")
        assertTrue(reA.containsMatchIn(sfc), "底辺A")
    }

    @Test
    fun rectangle_row_round_trips_through_serialize() {
        val csv = "1,6.0,5.0,4.0,-1,-1\nRectangle,1,5,4,3,1,1\n"
        val out = CsvCodec.serialize(CsvCodec.parse(csv))
        assertTrue(out.contains("Rectangle,1,5,4,3,1,1"))
    }

    @Test
    fun rect_child_triangle_renders() {
        val csv = "Rectangle,1,5,10,7,-1,0\n2,7,4,4,1,2\n"
        val json = WebPrimitiveRenderer.renderCsv(csv, 1f)
        assertEquals(7, count(json, """"layer":"tri""""))
    }

    @Test
    fun mixed_drawing_numbers_are_continuous_across_kinds() {
        val csv = "1,6.0,5.0,4.0,-1,-1\n2,5.0,4.0,3.0,1,1\nRectangle,1,5,4,3,-1,0\n3,3,3,3,3,2\n"
        val json = WebPrimitiveRenderer.renderCsv(csv, 1f)
        assertTrue(json.contains(""""text":"3""""))
        assertTrue(json.contains(""""text":"4""""))
    }
}
