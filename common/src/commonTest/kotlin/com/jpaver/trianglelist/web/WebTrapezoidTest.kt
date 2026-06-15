package com.jpaver.trianglelist.web

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.EditObject
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 混在リスト段1 (B1): CSV の Trapezoid 行 → Rectangle 構築 → 台形描画のテスト。
 * contract は trap-design.md (`Trapezoid, num, length, widthA, widthB, parent, side`)。
 *
 * SoT 一本化 段3g (2026-06-15、 user「三角形を台形の子かどうかで data class を分けてるのは変な実装」):
 * buildFigures (= Pair<List<Rectangle>, List<Triangle>>) は廃止、 後継は buildMixed → 単一 EditList<EditObject>。
 * 本テストでは旧 interface の test 意図 (= 2 list 分解での assertion) を維持するため test-local helper `figs`
 * を残す。 内部は buildMixed の filterIsInstance で 2 種抽出 (TriTrap 識別 = Triangle.node.a is Rectangle)。
 */
class WebTrapezoidTest {

    /** test-local helper: buildMixed の戻り値を旧 buildFigures の (traps, trapTris) Pair 形式で返す。
     *  trapTris 識別 = trilist に含まれない Triangle (= 台形親 or TriTrap chain 親、 buildMixed 内で
     *  trilist.getBy(...) 経由でない経路で Triangle を構築している)。 node.a is Rectangle だと TriTrap chain
     *  の 2 段目以降 (= 親 = Triangle) を拾えない。 */
    private fun figs(doc: CsvCodec.CsvDoc, trilist: TriangleList, scale: Float = 1f): Pair<List<Rectangle>, List<Triangle>> {
        val mixed = CsvCodec.buildMixed(doc, trilist, scale)
        val all = (1..mixed.size()).map { mixed.get(it) }
        val traps = all.filterIsInstance<Rectangle>()
        val trilistTris: Set<Triangle> = (1..trilist.size()).map { trilist.getBy(it) }.toSet()
        val trapTris = all.filterIsInstance<Triangle>().filter { it !in trilistTris }
        return Pair(traps, trapTris)
    }

    private val sampleCsv = """
        テスト工事
        適当路線
        適当業者
        T-001
        1,6.0,5.0,4.0,-1,-1
        2,5.0,4.0,3.0,1,1
        3,4.0,3.5,3.0,1,2
        4,3.5,3.0,3.2,2,1
        5,3.0,2.8,3.0,3,2
        6,3.0,2.5,2.5,4,2
        7,2.8,2.2,2.5,5,1
    """.trimIndent() + "\n"

    private fun count(haystack: String, needle: String): Int {
        var c = 0
        var i = haystack.indexOf(needle)
        while (i >= 0) {
            c++
            i = haystack.indexOf(needle, i + needle.length)
        }
        return c
    }

    // figureRows フィルタリングヘルパー
    private fun CsvCodec.CsvDoc.triRows() = figureRows.filter {
        val t = it.chunks.firstOrNull(); t != "Trapezoid" && t != "TriTrap"
    }
    private fun CsvCodec.CsvDoc.trapRows() = figureRows.filter { it.chunks.firstOrNull() == "Trapezoid" }
    private fun CsvCodec.CsvDoc.trapParentedTriRows() = figureRows.filter { it.chunks.firstOrNull() == "TriTrap" }

    /** (a) 独立台形単独 CSV で、台形の 4 辺 line prim (layer "tri") がちょうど 4 本出る */
    @Test
    fun independent_trapezoid_emits_four_tri_lines() {
        val json = WebPrimitiveRenderer.renderCsv("Trapezoid,1,5,4,3,-1,0\n", 1f)
        assertEquals(4, count(json, """"layer":"tri""""), "台形は 4 辺ちょうど: $json")
        // 番号サークル 1 + 番号 "1" (三角形0個 → 通し番号の先頭) + 3 寸法
        assertEquals(1, count(json, """"type":"circle""""))
        assertTrue(json.contains(""""text":"1""""), "番号 1 が出る (T接頭辞なし・三角形からの通し): $json")
        assertEquals(4, count(json, """"layer":"dim""""), "底辺A・上辺C・延長B + D右脚 の 4 寸法 (D右脚は派生辺だが接続子の親辺長 SoT、commit 6495541): $json")
    }

    /** (a)-2 独立台形の頂点が右台形 (底辺=widthA, 上辺=widthB, 延長=length 直交) になる */
    @Test
    fun independent_trapezoid_geometry_matches_rectangle() {
        val doc = CsvCodec.parse("Trapezoid,1,5,4,3,-1,0\n")
        val trilist = CsvCodec.build(doc)
        val (traps, _) = figs(doc, trilist, 1f)
        assertEquals(1, traps.size)
        val lp = traps[0].calcPoint()
        val baseLen = lp.a.left.lengthTo(lp.a.right)   // 底辺 = widthA = 4
        val topLen = lp.b.left.lengthTo(lp.b.right)     // 上辺 = widthB = 3
        val extLen = lp.b.left.lengthTo(lp.a.left)      // 延長 = length = 5 (leftB→baseline.left)
        assertEquals(4.0, baseLen, 0.001)
        assertEquals(3.0, topLen, 0.001)
        assertEquals(5.0, extLen, 0.001)
    }

    /** (c) 接続台形 (三角形1のB辺に接続) で、台形の底辺が親三角形のB辺にぴったり乗る */
    @Test
    fun connected_trapezoid_base_lies_on_parent_b_edge() {
        val csv = "1,6.0,5.0,4.0,-1,-1\nTrapezoid,1,5,4,3,1,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (traps, _) = figs(doc, trilist, 1f)
        assertEquals(1, traps.size)
        val lp = traps[0].calcPoint()
        // side=1 = 親のB辺 = Line(pointAB, pointBC) (TriangleUtilitiesExtensions.kt:145-147)
        val parent = trilist.getBy(1)
        assertTrue(lp.a.left.nearBy(parent.pointAB, 0.001), "底辺左 ${lp.a.left} != 親pointAB ${parent.pointAB}")
        assertTrue(lp.a.right.nearBy(parent.pointBC, 0.001), "底辺右 ${lp.a.right} != 親pointBC ${parent.pointBC}")
    }

    /**
     * 台形を親に持つ三角形 (TriTrap 行)。三角形の底辺(A)が台形の指定辺 (上辺C=side2) にぴったり乗る。
     * かつ TriTrap 行は triRows に入らない → build() (golden パイプライン) が一切見ない。
     */
    @Test
    fun triangle_can_be_child_of_trapezoid() {
        // 独立台形 (延長5/底辺10/上辺7) の上辺C(side2) に三角形(B=4,C=4)を接続
        val csv = "Trapezoid,1,5,10,7,-1,0\nTriTrap,2,7,4,4,1,2\n"
        val doc = CsvCodec.parse(csv)
        assertEquals(1, doc.trapParentedTriRows().size, "TriTrap 行が figureRows に入る")
        assertEquals(0, doc.triRows().size, "TriTrap は普通の三角形行に入らない → build は見ない (golden)")
        val trilist = CsvCodec.build(doc)
        val (traps, tris) = figs(doc, trilist, 1f)
        assertEquals(1, traps.size)
        assertEquals(1, tris.size, "台形を親に三角形が1個建つ")
        val edge = traps[0].getLine(2) // 上辺C
        val tri = tris[0]
        // 底辺が上辺Cに乗る。向きは問わない
        val onEdge = (tri.point[0].nearBy(edge.left, 0.001) && tri.pointAB.nearBy(edge.right, 0.001)) ||
            (tri.point[0].nearBy(edge.right, 0.001) && tri.pointAB.nearBy(edge.left, 0.001))
        assertTrue(onEdge, "三角形の底辺が台形上辺に乗らない: p0=${tri.point[0]} pAB=${tri.pointAB} edge=$edge")
        // 頂点 (pointBC) は親台形の外側に出る
        val lp = traps[0].calcPoint()
        val cx = (lp.a.left.x + lp.a.right.x + lp.b.left.x + lp.b.right.x) / 4f
        val cy = (lp.a.left.y + lp.a.right.y + lp.b.left.y + lp.b.right.y) / 4f
        val dx = edge.right.x - edge.left.x
        val dy = edge.right.y - edge.left.y
        val apexSide = dx * (tri.pointBC.y - edge.left.y) - dy * (tri.pointBC.x - edge.left.x)
        val centSide = dx * (cy - edge.left.y) - dy * (cx - edge.left.x)
        assertTrue(apexSide * centSide < 0f, "頂点が台形内部を向き重なっている: apexSide=$apexSide centSide=$centSide")
    }

    /** TriTrap 行が描画される: 台形(4辺) + 台形子三角形(3辺) = tri 線 7 本、番号サークル 2 */
    @Test
    fun trap_parented_triangle_renders() {
        val csv = "Trapezoid,1,5,10,7,-1,0\nTriTrap,2,7,4,4,1,2\n"
        val json = WebPrimitiveRenderer.renderCsv(csv, 1f)
        assertEquals(7, count(json, """"layer":"tri""""), "台形4辺 + 台形子三角形3辺 = 7: $json")
        assertEquals(2, count(json, """"type":"circle""""), "台形の番号 + 台形子三角形の番号 = 2 サークル: $json")
    }

    /** (b) 台形は純粋に末尾追加 — 三角形・控除の prim を一切動かさない (golden 不変の核) */
    @Test
    fun trapezoid_is_purely_additive_to_existing_prims() {
        val base = WebPrimitiveRenderer.renderCsv(sampleCsv, 1f)
        val withTrap = WebPrimitiveRenderer.renderCsv(sampleCsv + "Trapezoid,1,5,4,3,-1,0\n", 1f)
        assertTrue(withTrap.startsWith(base.dropLast(1)), "既存 prim が動いた")
        assertTrue(withTrap.length > base.length, "台形 prim が足されていない")
    }

    /** parse → serialize で Trapezoid 行が round-trip する (未知列も生のまま書き戻る) */
    @Test
    fun trapezoid_row_round_trips_through_serialize() {
        val csv = "1,6.0,5.0,4.0,-1,-1\nTrapezoid,1,5,4,3,1,1\n"
        val out = CsvCodec.serialize(CsvCodec.parse(csv))
        assertTrue(out.contains("Trapezoid,1,5,4,3,1,1"), "serialize 出力に台形行: $out")
    }

    // ---- 段3: 上辺アライメント (左/中/右) ----

    /** align=0 (左寄せ、従来) : 上辺左端が底辺左端の真上、上辺右端は widthB ぶん右 */
    @Test
    fun alignment_left_keeps_top_left_above_base_left() {
        val lp = Rectangle(3.0, 10.0, 4.0, alignment = 0).calcPoint()
        assertEquals(0.0, lp.b.left.x, 0.001, "左寄せ: 上辺左端 x=底辺左端 x=0")
        assertEquals(3.0, lp.b.left.y, 0.001, "上辺の高さ = 延長 3")
        assertEquals(4.0, lp.b.right.x, 0.001, "上辺右端 = 左端 + widthB(4)")
    }

    /** align=0 は alignment 引数を省いた従来構築とビット同値 (golden / 後方互換の核) */
    @Test
    fun alignment_zero_is_identical_to_legacy_default() {
        val legacy = Rectangle(3.0, 10.0, 4.0).calcPoint()
        val explicit = Rectangle(3.0, 10.0, 4.0, alignment = 0).calcPoint()
        assertEquals(legacy.b.left.x, explicit.b.left.x, 0.0, "leftB.x が従来と完全一致")
        assertEquals(legacy.b.left.y, explicit.b.left.y, 0.0, "leftB.y が従来と完全一致")
        assertEquals(legacy.b.right.x, explicit.b.right.x, 0.0, "rightB.x が従来と完全一致")
        assertEquals(legacy.b.right.y, explicit.b.right.y, 0.0, "rightB.y が従来と完全一致")
    }

    /** align=1 (中央) : 上辺の中点 x が底辺の中点 x (=5) に一致 (左右対称) */
    @Test
    fun alignment_center_is_symmetric() {
        val lp = Rectangle(3.0, 10.0, 4.0, alignment = 1).calcPoint()
        assertEquals(3.0, lp.b.left.x, 0.001, "中央: 上辺左端 x = (10-4)/2 = 3")
        assertEquals(7.0, lp.b.right.x, 0.001, "中央: 上辺右端 x = 3+4 = 7")
        val topMid = (lp.b.left.x + lp.b.right.x) / 2.0
        val baseMid = (lp.a.left.x + lp.a.right.x) / 2.0
        assertEquals(baseMid, topMid, 0.001, "上辺中点 x = 底辺中点 x")
    }

    /** align=2 (右寄せ) : 上辺右端が底辺右端(x=10)の真上 */
    @Test
    fun alignment_right_keeps_top_right_above_base_right() {
        val lp = Rectangle(3.0, 10.0, 4.0, alignment = 2).calcPoint()
        assertEquals(10.0, lp.b.right.x, 0.001, "右寄せ: 上辺右端 x=底辺右端 x=10")
        assertEquals(6.0, lp.b.left.x, 0.001, "右寄せ: 上辺左端 x = 10-4 = 6")
        assertEquals(3.0, lp.b.left.y, 0.001, "上辺の高さ = 延長 3")
    }

    /** CSV 8列目 align を buildFigures が読む。省略時0、左右で上辺左端が変わる */
    @Test
    fun csv_align_column_is_read_by_build_figures() {
        fun topLeft(alignCol: String): com.example.trilib.PointXY {
            val doc = CsvCodec.parse("Trapezoid,1,5,4,3,-1,0$alignCol\n")
            val trilist = CsvCodec.build(doc)
            val (traps, _) = figs(doc, trilist, 1f)
            return traps[0].calcPoint().b.left
        }
        val left = topLeft("")          // 列省略 → align=0 (後方互換)
        val leftExplicit0 = topLeft(",0")
        val right = topLeft(",2")
        assertEquals(left.x, leftExplicit0.x, 0.001, "align 列省略は align=0 と同値")
        assertEquals(left.y, leftExplicit0.y, 0.001, "align 列省略は align=0 と同値")
        assertTrue(kotlin.math.abs(left.x - right.x) > 0.1, "align=0 と align=2 で上辺左端 x が変わる: $left vs $right")
    }

    // ---- 段4-2 (R4): 台形を親にする接続 (台形→台形 B/C/D)、9 列目 parentKind ----

    /** 台形チェーン B (side=1=B左脚): trap2 の底辺が trap1.getLine(1) の両端に乗る */
    @Test
    fun trap_to_trap_chain_on_edge_b() {
        val csv = "Trapezoid,1,5,10,4,-1,0,0\nTrapezoid,2,3,4,3,1,1,0,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (traps, _) = figs(doc, trilist, 1f)
        assertEquals(2, traps.size, "台形 2 個が構築される")
        val parentEdge = traps[0].getLine(1)
        val childBase = traps[1].calcPoint().a
        assertTrue(childBase.left.nearBy(parentEdge.left, 0.001), "底辺左 ${childBase.left} != 親B辺左 ${parentEdge.left}")
        assertTrue(childBase.right.nearBy(parentEdge.right, 0.001), "底辺右 ${childBase.right} != 親B辺右 ${parentEdge.right}")
    }

    /** 台形チェーン C (side=2=C上辺): trap2 の底辺が trap1.getLine(2) の両端に乗る */
    @Test
    fun trap_to_trap_chain_on_edge_c() {
        val csv = "Trapezoid,1,5,10,4,-1,0,0\nTrapezoid,2,3,4,3,1,2,0,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (traps, _) = figs(doc, trilist, 1f)
        assertEquals(2, traps.size)
        val parentEdge = traps[0].getLine(2)
        val childBase = traps[1].calcPoint().a
        assertTrue(childBase.left.nearBy(parentEdge.left, 0.001), "底辺左 ${childBase.left} != 親C辺左 ${parentEdge.left}")
        assertTrue(childBase.right.nearBy(parentEdge.right, 0.001), "底辺右 ${childBase.right} != 親C辺右 ${parentEdge.right}")
    }

    /** 台形チェーン D (side=3=D右脚): trap2 の底辺が trap1.getLine(3) の両端に乗る */
    @Test
    fun trap_to_trap_chain_on_edge_d() {
        val csv = "Trapezoid,1,5,10,4,-1,0,0\nTrapezoid,2,3,4,3,1,3,0,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (traps, _) = figs(doc, trilist, 1f)
        assertEquals(2, traps.size)
        val parentEdge = traps[0].getLine(3)
        val childBase = traps[1].calcPoint().a
        assertTrue(childBase.left.nearBy(parentEdge.left, 0.001), "底辺左 ${childBase.left} != 親D辺左 ${parentEdge.left}")
        assertTrue(childBase.right.nearBy(parentEdge.right, 0.001), "底辺右 ${childBase.right} != 親D辺右 ${parentEdge.right}")
    }

    /** parentKind 省略 (8 列) は親=三角形のまま不変: trap の底辺が親三角形の B 辺に乗る */
    @Test
    fun parent_kind_omitted_8col_keeps_triangle_parent() {
        val csv = "1,6.0,5.0,4.0,-1,-1\nTrapezoid,1,5,4,3,1,1,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (traps, _) = figs(doc, trilist, 1f)
        assertEquals(1, traps.size)
        val lp = traps[0].calcPoint()
        val parent = trilist.getBy(1)
        assertTrue(lp.a.left.nearBy(parent.pointAB, 0.001), "8列: 底辺左 ${lp.a.left} != 親pointAB ${parent.pointAB}")
        assertTrue(lp.a.right.nearBy(parent.pointBC, 0.001), "8列: 底辺右 ${lp.a.right} != 親pointBC ${parent.pointBC}")
    }

    /** parentKind=1 だが親が前方参照/範囲外なら独立 fallback */
    @Test
    fun parent_kind_trap_out_of_range_falls_back_to_independent() {
        val doc = CsvCodec.parse("Trapezoid,1,5,4,3,1,1,0,1\n")
        val trilist = CsvCodec.build(doc)
        val (traps, _) = figs(doc, trilist, 1f)
        assertEquals(1, traps.size)
        assertEquals(null, traps[0].nodeA, "範囲外の台形親は独立 fallback (nodeA=null)")
    }

    /** 純三角形 golden 不変: 台形ゼロの三角形 CSV は build/serialize が完全同値 (台形は素通し) */
    @Test
    fun pure_triangle_csv_is_unchanged() {
        val triOnly = "1,6.0,5.0,4.0,-1,-1\n2,5.0,4.0,3.0,1,1\n3,4.0,3.5,3.0,1,2\n"
        val doc = CsvCodec.parse(triOnly)
        val (traps, _) = figs(doc, CsvCodec.build(doc), 1f)
        assertEquals(0, traps.size, "三角形のみなら台形ゼロ")
        assertEquals(triOnly, CsvCodec.serialize(doc), "純三角形 CSV は serialize 同値")
    }

    /** renderCsv で台形チェーン (台形→台形) が 2 個分の tri 線 (4本×2=8本) を出す */
    @Test
    fun render_csv_draws_trap_chain_eight_tri_lines() {
        val chainCsv = "Trapezoid,1,5,10,4,-1,0,0\nTrapezoid,2,3,4,3,1,1,0,1\n"
        val json = WebPrimitiveRenderer.renderCsv(chainCsv, 1f)
        assertEquals(8, count(json, """"layer":"tri""""), "台形 2 個で 4本×2=8 本の tri 線: $json")
        assertEquals(2, count(json, """"type":"circle""""), "番号サークル 2 個")
        assertTrue(json.contains(""""text":"1""""), "1 個目の番号 1 が出る: $json")
        assertTrue(json.contains(""""text":"2""""), "2 個目の番号 2 が出る (T接頭辞なし): $json")
    }

    /** 統合採番: 三角形 N 個の後の台形は N+1 から番号が続く */
    @Test
    fun trapezoid_number_continues_from_triangle_count() {
        val csv = "1,6.0,5.0,4.0,-1,-1\n2,5.0,4.0,3.0,1,1\nTrapezoid,1,5,4,3,-1,0\n"
        val json = WebPrimitiveRenderer.renderCsv(csv, 1f)
        assertTrue(json.contains(""""text":"3""""), "台形は三角形2個の次=番号3が出る: $json")
        assertTrue(!json.contains(""""text":"T"""), "T接頭辞付き番号は出ない: $json")
    }

    // ---- 延長 = 垂線の長さ (斜辺長ではない) + 中央/右寄せの点線ガイド ----

    /** 中央寄せ: 延長は底辺からの垂線の長さ (length=5) を出す */
    @Test
    fun trapezoid_extension_dim_is_perpendicular_not_slant_leg() {
        val json = WebPrimitiveRenderer.renderCsv("Trapezoid,1,5,10,7,-1,0,1\n", 1f)
        assertTrue(json.contains(""""text":" 5.0""""), "延長は垂線の長さ 5.0 を出す: $json")
        assertEquals(1, count(json, """"layer":"guide""""), "中央寄せで垂線ガイド1本: $json")
    }

    /** 左寄せ (align=0): 左脚=垂線なのでガイド線は引かない */
    @Test
    fun trapezoid_no_guide_when_left_aligned() {
        val json = WebPrimitiveRenderer.renderCsv("Trapezoid,1,5,4,3,-1,0,0\n", 1f)
        assertEquals(0, count(json, """"layer":"guide""""), "左寄せはガイド不要 (左脚=垂線): $json")
        assertTrue(json.contains(""""text":" 5.0""""), "延長 5.0 は左寄せでも出る: $json")
    }

    // ---- 混在接続の土台 (逆方向): 三角形を台形の辺に乗せる ----

    @Test
    fun triangle_can_attach_to_trapezoid_edge() {
        val trap = Rectangle(5.0, 10.0, 7.0)
        val lp = trap.calcPoint()
        val cx = (lp.a.left.x + lp.a.right.x + lp.b.left.x + lp.b.right.x) / 4f
        val cy = (lp.a.left.y + lp.a.right.y + lp.b.left.y + lp.b.right.y) / 4f
        for (side in 1..3) {
            val edge = trap.getLine(side)
            val tri = Triangle(trap as EditObject, side, 6f, 6f)
            val onEdge = (tri.point[0].nearBy(edge.left, 0.001) && tri.pointAB.nearBy(edge.right, 0.001)) ||
                (tri.point[0].nearBy(edge.right, 0.001) && tri.pointAB.nearBy(edge.left, 0.001))
            assertTrue(onEdge, "side=$side: 三角形A辺が台形辺に乗らない p0=${tri.point[0]} pAB=${tri.pointAB} edge=$edge")
            val dx = edge.right.x - edge.left.x
            val dy = edge.right.y - edge.left.y
            val apexSide = dx * (tri.pointBC.y - edge.left.y) - dy * (tri.pointBC.x - edge.left.x)
            val centSide = dx * (cy - edge.left.y) - dy * (cx - edge.left.x)
            assertTrue(apexSide * centSide < 0f, "side=$side: 頂点が台形内部を向き重なる apexSide=$apexSide centSide=$centSide")
        }
    }

    /**
     * DXF 書き出しに台形が出る。台形を含む CSV の DXF は、三角形のみより LINE エンティティが
     * ちょうど 4 本 (台形の4辺) 増える。
     */
    @Test
    fun dxf_export_includes_trapezoid_four_lines() {
        val triOnly = WebDrawingExport.buildDxfText("1,6.0,5.0,4.0,-1,-1\n", "")
        val withTrap = WebDrawingExport.buildDxfText("1,6.0,5.0,4.0,-1,-1\nTrapezoid,1,5,4,3,-1,0\n", "")
        assertTrue(withTrap.length > triOnly.length, "台形ぶん DXF が増える")
        assertEquals(count(triOnly, "AcDbLine") + 4, count(withTrap, "AcDbLine"),
            "台形の4辺ぶん LINE エンティティが増える (DXF に台形が出ている)")
    }

    /** TriTrap (台形を親に持つ三角形) も DXF に出る */
    @Test
    fun dxf_export_includes_trap_parented_triangle_lines() {
        val trapOnly = WebDrawingExport.buildDxfText("Trapezoid,1,5,10,7,-1,0\n", "")
        val withChild = WebDrawingExport.buildDxfText("Trapezoid,1,5,10,7,-1,0\nTriTrap,2,7,4,4,1,2\n", "")
        assertTrue(withChild.length > trapOnly.length, "TriTrap ぶん DXF が増える")
        assertEquals(count(trapOnly, "AcDbLine") + 3, count(withChild, "AcDbLine"),
            "台形子三角形の 3 辺ぶん LINE エンティティが増える (DXF に TriTrap が出ている): $withChild")
    }

    /** SFC でも TriTrap が出る */
    @Test
    fun sfc_export_includes_trap_parented_triangle_lines() {
        val trapOnly = WebDrawingExport.buildSfcText("Trapezoid,1,5,10,7,-1,0\n", "t.sfc")
        val withChild = WebDrawingExport.buildSfcText("Trapezoid,1,5,10,7,-1,0\nTriTrap,2,7,4,4,1,2\n", "t.sfc")
        assertTrue(withChild.length > trapOnly.length, "TriTrap ぶん SFC が増える")
    }

    /** TriTrap タグ廃止: 普通三角形行で「parent が三角形数を超える」と台形親と解釈される */
    @Test
    fun new_schema_triangle_row_with_trap_parent_is_equivalent_to_legacy_tritrap() {
        // 旧形式: TriTrap タグ。trap idx=1, side=2
        val legacy = "Trapezoid,1,5,10,7,-1,0\nTriTrap,2,7,4,4,1,2\n"
        // 新形式: 普通三角形行で parent=1 (三角形 0 個 + 台形 idx 1 = 混在通し番号 1)
        val modern = "Trapezoid,1,5,10,7,-1,0\n2,7,4,4,1,2\n"
        val legacyDoc = CsvCodec.parse(legacy)
        val modernDoc = CsvCodec.parse(modern)
        assertEquals(1, legacyDoc.trapParentedTriRows().size, "旧形式: TriTrap 行が別種別")
        assertEquals(1, modernDoc.trapParentedTriRows().size, "新形式: 普通三角形行 → 内部で同じ種別に振り分け")
        assertEquals(0, modernDoc.triRows().size, "新形式: 台形親の三角形は triRows に入らない (golden 不変)")
        // build 結果も同値
        val legacyTrilist = CsvCodec.build(legacyDoc)
        val modernTrilist = CsvCodec.build(modernDoc)
        val (legacyTraps, legacyTrapTris) = figs(legacyDoc, legacyTrilist, 1f)
        val (modernTraps, modernTrapTris) = figs(modernDoc, modernTrilist, 1f)
        assertEquals(legacyTraps.size, modernTraps.size)
        assertEquals(legacyTrapTris.size, modernTrapTris.size)
        assertEquals(1, modernTrapTris.size, "新形式でも台形子三角形が 1 個建つ")
    }

    /** TriTrap chain: 台形 → 台形子三角形 → 更にその子三角形 が新 schema で建つ */
    @Test
    fun tritrap_chain_can_be_built_from_tritrap_parent() {
        val csv = "Trapezoid,1,5,10,7,-1,0\n" +
            "2,7,4,4,1,2\n" +     // 台形 #1 の C 辺 (side=2) に乗る子三角形 (= TriTrap #1)
            "3,4,3,3,2,1\n"       // TriTrap #1 を親に新しい子三角形 (= TriTrap #2 chain)
        val doc = CsvCodec.parse(csv)
        assertEquals(0, doc.triRows().size, "普通三角形行は無し (全て台形子)")
        assertEquals(1, doc.trapRows().size, "台形 1")
        assertEquals(2, doc.trapParentedTriRows().size, "tritrap chain で 2 行とも台形子バケツへ振り分け")
        val trilist = CsvCodec.build(doc)
        val (traps, trapTris) = figs(doc, trilist, 1f)
        assertEquals(1, traps.size, "台形 1")
        assertEquals(2, trapTris.size, "tritrap chain 2 個建つ (1 番目=台形親、2 番目=tritrap親)")
        // 2 番目の三角形の底辺が 1 番目 (= TriTrap #1) の B 辺 (side=1) に乗る
        val tt1 = trapTris[0]
        val tt2 = trapTris[1]
        val edge = tt1.getLine(1) // 1 番目 tritrap の B 辺
        val onEdge = (tt2.point[0].nearBy(edge.left, 0.001) && tt2.pointAB.nearBy(edge.right, 0.001)) ||
            (tt2.point[0].nearBy(edge.right, 0.001) && tt2.pointAB.nearBy(edge.left, 0.001))
        assertTrue(onEdge, "2 番目の底辺が 1 番目 TriTrap の B 辺に乗らない: tt2.p0=${tt2.point[0]} tt2.pAB=${tt2.pointAB} edge=$edge")
    }

    /** TriTrap タグの round-trip: 旧形式を読み、書き戻すと TriTrap タグが消えて普通三角形行になる */
    @Test
    fun tritrap_serialize_writes_modern_triangle_row_and_round_trips() {
        val legacy = "Trapezoid,1,5,10,7,-1,0\nTriTrap,2,7,4,4,1,2\n"
        val written = CsvCodec.serialize(CsvCodec.parse(legacy))
        assertTrue(!written.contains("TriTrap"), "新規書き出しに TriTrap タグは含まない: $written")
        assertTrue(written.contains("2,7,4,4,1,2"), "普通三角形行形式 (num=2, A=7, B=4, C=4, parent=1, side=2): $written")
        // 再 parse で同じ内部表現
        val reparsed = CsvCodec.parse(written)
        assertEquals(1, reparsed.trapParentedTriRows().size, "再 parse で台形親三角形が同じ場所に振り分けられる")
        assertEquals(1, reparsed.trapRows().size, "台形は不変")
    }

    /** 混在通し番号の安全網: 三角形2 + 台形1 + TriTrap1 を CSV 順で書くと、番号は 1,2,3,4 が連続 */
    @Test
    fun mixed_drawing_numbers_are_continuous_across_kinds() {
        val csv = "1,6.0,5.0,4.0,-1,-1\n2,5.0,4.0,3.0,1,1\n" +
            "Trapezoid,1,5,4,3,1,1\nTriTrap,2,3,3,3,1,2\n"
        val json = WebPrimitiveRenderer.renderCsv(csv, 1f)
        assertTrue(json.contains(""""text":"1""""), "三角形1 → 番号 1: $json")
        assertTrue(json.contains(""""text":"2""""), "三角形2 → 番号 2: $json")
        assertTrue(json.contains(""""text":"3""""), "台形 → 番号 3 (三角形 2 個の後): $json")
        assertTrue(json.contains(""""text":"4""""), "TriTrap → 番号 4 (三角形+台形の後): $json")
        assertEquals(4, count(json, """"type":"circle""""), "番号サークル 4 個 (三角形2+台形1+TriTrap1): $json")
    }

    /** 混在順 build の安全網: 三角形→台形→TriTrap→三角形 のように CSV 出現順がバラついても全部建つ */
    @Test
    fun mixed_order_csv_builds_all_figures() {
        val csv = "1,6.0,5.0,4.0,-1,-1\n" +
            "Trapezoid,1,5,4,3,1,1\n" +
            "TriTrap,2,3,3,3,1,2\n" +
            "2,5.0,4.0,3.0,1,1\n"
        val doc = CsvCodec.parse(csv)
        assertEquals(4, doc.figureRows.size, "figureRows に 4 行が CSV 出現順で揃う")
        val trilist = CsvCodec.build(doc)
        val (traps, trapTris) = figs(doc, trilist, 1f)
        assertEquals(2, trilist.size(), "三角形 2 個 (CSV 順がバラついても両方建つ)")
        assertEquals(1, traps.size, "台形 1 個")
        assertEquals(1, trapTris.size, "台形子三角形 1 個")
    }

    /** 位置順ビルドの土台: parse が図形行を CSV 出現順で保持する */
    @Test
    fun parse_preserves_figure_row_order() {
        val csv = "1,6.0,5.0,4.0,-1,-1\nTrapezoid,1,5,4,3,1,1\n2,5.0,4.0,3.0,1,1\n"
        val doc = CsvCodec.parse(csv)
        val kinds = doc.figureRows.map { if (it.chunks.firstOrNull() == "Trapezoid") "trap" else "tri" }
        assertEquals(listOf("tri", "trap", "tri"), kinds, "図形行は CSV 出現順 (三角形→台形→三角形): $kinds")
        // 既存の種別分離も不変 (三角形2 + 台形1)
        assertEquals(2, doc.triRows().size)
        assertEquals(1, doc.trapRows().size)
    }
}
