package com.jpaver.trianglelist.web

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 混在リスト段1 (B1): CSV の Rectangle 行 → Rectangle 構築 → Rectangle (台形) 描画のテスト。
 * contract は trap-design.md (`Rectangle, num, length, widthA, widthB, parent, side`)。
 *
 * user 確定 2026-06-16「TriTrap タグ排除、 Triangle 行 1 種で親種別を統合」── CSV タグは Rectangle 1 種、
 * Rectangle 子三角形は普通三角形行で parent=混在通し番号 (= trilist.size() + Rectangle idx) で表現する。
 *
 * test-local helper `figs` は buildMixed の戻り値を旧 buildFigures の (rects, rectChildTris) Pair 形式で
 * 返す。rectChildTris 識別 = trilist に含まれない Triangle (= Rectangle 親 or chain 親、 buildMixed 内で
 * trilist.getBy(...) 経由でない経路で Triangle を構築している)。
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

    // figureRows フィルタリングヘルパー (rectChildTriRows は parent > 直前 tri 数 で判定)
    private fun CsvCodec.CsvDoc.rectRows() = figureRows.filter { it.chunks.firstOrNull() == "Rectangle" }
    private fun CsvCodec.CsvDoc.splitTriRows(): Pair<List<CsvCodec.CsvRow>, List<CsvCodec.CsvRow>> {
        val triRows = mutableListOf<CsvCodec.CsvRow>()
        val rectChildRows = mutableListOf<CsvCodec.CsvRow>()
        var triCount = 0
        for (row in figureRows) {
            if (row.chunks.firstOrNull() == "Rectangle") continue
            val parent = row.chunks.getOrNull(4)?.toIntOrNull() ?: -1
            if (parent > triCount && parent > 0) {
                rectChildRows.add(row)
            } else {
                triRows.add(row)
                triCount++
            }
        }
        return triRows to rectChildRows
    }
    private fun CsvCodec.CsvDoc.triRows() = splitTriRows().first
    private fun CsvCodec.CsvDoc.rectChildTriRows() = splitTriRows().second

    /** (a) 独立 Rectangle 単独 CSV で、 Rectangle の 4 辺 line prim (layer "tri") がちょうど 4 本出る */
    @Test
    fun independent_rectangle_emits_four_tri_lines() {
        val json = WebPrimitiveRenderer.renderCsv("Rectangle,1,5,4,3,-1,0\n", 1f)
        assertEquals(4, count(json, """"layer":"tri""""), "Rectangle は 4 辺ちょうど: $json")
        // 番号サークル 1 + 番号 "1" + 3 寸法 (底辺A・上辺C・延長B のみ。斜辺は出さない 2026-06-17)
        assertEquals(1, count(json, """"type":"circle""""))
        assertTrue(json.contains(""""text":"1""""), "番号 1 が出る (三角形からの通し): $json")
        assertEquals(3, count(json, """"layer":"dim""""), "底辺A・上辺C・延長B の 3 寸法 (斜辺なし): $json")
    }

    /** (a)-2 独立 Rectangle の頂点が右台形 (底辺=widthA, 上辺=widthB, 延長=length 直交) になる */
    @Test
    fun independent_rectangle_geometry_matches_rectangle() {
        val doc = CsvCodec.parse("Rectangle,1,5,4,3,-1,0\n")
        val trilist = CsvCodec.build(doc)
        val (rects, _) = figs(doc, trilist, 1f)
        assertEquals(1, rects.size)
        val lp = rects[0].calcPoint()
        val baseLen = lp.a.left.lengthTo(lp.a.right)   // 底辺 = widthA = 4
        val topLen = lp.b.left.lengthTo(lp.b.right)     // 上辺 = widthB = 3
        val extLen = lp.b.left.lengthTo(lp.a.left)      // 延長 = length = 5 (leftB→baseline.left)
        assertEquals(4.0, baseLen, 0.001)
        assertEquals(3.0, topLen, 0.001)
        assertEquals(5.0, extLen, 0.001)
    }

    /** (c) 接続 Rectangle (三角形1のB辺に接続) で、 Rectangle の底辺が親三角形のB辺にぴったり乗る */
    @Test
    fun connected_rectangle_base_lies_on_parent_b_edge() {
        val csv = "1,6.0,5.0,4.0,-1,-1\nRectangle,1,5,4,3,1,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (rects, _) = figs(doc, trilist, 1f)
        assertEquals(1, rects.size)
        val lp = rects[0].calcPoint()
        // initByParent は親辺を逆走するため、底辺端点は親辺端点と順序不問で一致する
        val parent = trilist.getBy(1)
        val onEdge = (lp.a.left.nearBy(parent.pointAB, 0.001) && lp.a.right.nearBy(parent.pointBC, 0.001)) ||
                     (lp.a.left.nearBy(parent.pointBC, 0.001) && lp.a.right.nearBy(parent.pointAB, 0.001))
        assertTrue(onEdge, "底辺 [${lp.a.left}..${lp.a.right}] が親B辺 [${parent.pointAB}..${parent.pointBC}] に乗らない")
    }

    /**
     * Rectangle を親に持つ三角形 (新 schema: Triangle 行 + parent=混在通し番号)。
     * 三角形の底辺(A)が Rectangle の指定辺 (上辺C=side2) にぴったり乗る。
     * かつ parent > 直前 tri 数なので普通三角形行に入らない → build() (golden パイプライン) が一切見ない。
     */
    @Test
    fun triangle_can_be_child_of_rectangle() {
        // 独立 Rectangle (延長5/底辺10/上辺7) の上辺C(side2) に三角形(B=4,C=4)を接続
        // parent=1 (trilist.size()=0 + Rectangle idx 1 = 混在通し 1)
        val csv = "Rectangle,1,5,10,7,-1,0\n2,7,4,4,1,2\n"
        val doc = CsvCodec.parse(csv)
        assertEquals(1, doc.rectChildTriRows().size, "Rectangle 子三角形が figureRows に入る")
        assertEquals(0, doc.triRows().size, "Rectangle 子は普通三角形行に入らない → build は見ない (golden)")
        val trilist = CsvCodec.build(doc)
        val (rects, tris) = figs(doc, trilist, 1f)
        assertEquals(1, rects.size)
        assertEquals(1, tris.size, "Rectangle を親に三角形が1個建つ")
        val edge = rects[0].getLine(2) // 上辺C
        val tri = tris[0]
        // 底辺が上辺Cに乗る。向きは問わない
        val onEdge = (tri.point[0].nearBy(edge.left, 0.001) && tri.pointAB.nearBy(edge.right, 0.001)) ||
            (tri.point[0].nearBy(edge.right, 0.001) && tri.pointAB.nearBy(edge.left, 0.001))
        assertTrue(onEdge, "三角形の底辺が Rectangle 上辺に乗らない: p0=${tri.point[0]} pAB=${tri.pointAB} edge=$edge")
        // 頂点 (pointBC) は親 Rectangle の外側に出る
        val lp = rects[0].calcPoint()
        val cx = (lp.a.left.x + lp.a.right.x + lp.b.left.x + lp.b.right.x) / 4f
        val cy = (lp.a.left.y + lp.a.right.y + lp.b.left.y + lp.b.right.y) / 4f
        val dx = edge.right.x - edge.left.x
        val dy = edge.right.y - edge.left.y
        val apexSide = dx * (tri.pointBC.y - edge.left.y) - dy * (tri.pointBC.x - edge.left.x)
        val centSide = dx * (cy - edge.left.y) - dy * (cx - edge.left.x)
        assertTrue(apexSide * centSide < 0f, "頂点が Rectangle 内部を向き重なっている: apexSide=$apexSide centSide=$centSide")
    }

    /**
     * Triangle が Rectangle の D辺 (side=3) に接続する。
     * 回帰テスト: conn=3 を ConnCode に通すと「B辺双重断面」に誤解釈されていたバグ。
     * fix: CsvCodec.buildMixed で pObj is Rectangle のとき conn を直接 side として扱う。
     */
    @Test
    fun triangle_child_of_rectangle_on_d_edge() {
        // 独立 Rectangle (延長5/底辺10/上辺7) の D辺(side=3) に三角形(B=4,C=4)を接続
        // parent=1 (Rectangle が混在通し #1)
        val csv = "Rectangle,1,5,10,7,-1,0\n2,0,4,4,1,3\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (rects, tris) = figs(doc, trilist, 1f)
        assertEquals(1, rects.size)
        assertEquals(1, tris.size, "Rectangle D辺を親に三角形が1個建つ")
        val dEdge = rects[0].getLine(3) // D 右脚
        val bEdge = rects[0].getLine(1) // B 左脚 (誤接続先)
        val tri = tris[0]
        // 底辺が D辺に乗る (順序不問)
        val onD = (tri.point[0].nearBy(dEdge.left, 0.001) && tri.pointAB.nearBy(dEdge.right, 0.001)) ||
            (tri.point[0].nearBy(dEdge.right, 0.001) && tri.pointAB.nearBy(dEdge.left, 0.001))
        // B辺に乗っていないこと (旧バグで誤接続していた辺)
        val onB = (tri.point[0].nearBy(bEdge.left, 0.001) && tri.pointAB.nearBy(bEdge.right, 0.001)) ||
            (tri.point[0].nearBy(bEdge.right, 0.001) && tri.pointAB.nearBy(bEdge.left, 0.001))
        assertFalse(onB, "D辺接続なのに B辺に乗っている — conn=3 が B双重断面に誤解釈されている: p0=${tri.point[0]} pAB=${tri.pointAB} bEdge=$bEdge")
        assertTrue(onD, "三角形の底辺が Rectangle D辺に乗らない: p0=${tri.point[0]} pAB=${tri.pointAB} dEdge=$dEdge")
    }

    /** Rectangle 子三角形が描画される: Rectangle(4辺) + 子三角形(3辺) = tri 線 7 本、 番号サークル 2 */
    @Test
    fun rect_child_triangle_renders() {
        val csv = "Rectangle,1,5,10,7,-1,0\n2,7,4,4,1,2\n"
        val json = WebPrimitiveRenderer.renderCsv(csv, 1f)
        assertEquals(7, count(json, """"layer":"tri""""), "Rectangle 4辺 + 子三角形 3辺 = 7: $json")
        assertEquals(2, count(json, """"type":"circle""""), "Rectangle の番号 + 子三角形の番号 = 2 サークル: $json")
    }

    /** (b) Rectangle 追加で三角形 prim の数は不変、 全長は増加 */
    @Test
    fun rectangle_is_purely_additive_to_existing_prims() {
        val base = WebPrimitiveRenderer.renderCsv(sampleCsv, 1f)
        val withRect = WebPrimitiveRenderer.renderCsv(sampleCsv + "Rectangle,1,5,4,3,-1,0\n", 1f)
        val triLineRe = """"type":"line","layer":"tri","[^}]*"tri":\d+""".toRegex()
        val triCount = { json: String -> triLineRe.findAll(json).count() }
        assertTrue(withRect.length > base.length, "Rectangle prim が足されていない")
        // Rectangle は 1 個 (4 辺) 追加されるので tri line 数は base + 4。 三角形分は不変
        assertEquals(triCount(base) + 4, triCount(withRect), "三角形 line 数が動いた (Rectangle 4 辺以外の差分)")
    }

    /** parse → serialize で Rectangle 行が round-trip する (未知列も生のまま書き戻る) */
    @Test
    fun rectangle_row_round_trips_through_serialize() {
        val csv = "1,6.0,5.0,4.0,-1,-1\nRectangle,1,5,4,3,1,1\n"
        val out = CsvCodec.serialize(CsvCodec.parse(csv))
        assertTrue(out.contains("Rectangle,1,5,4,3,1,1"), "serialize 出力に Rectangle 行: $out")
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

    /** CSV 8列目 align を buildMixed が読む。省略時0、左右で上辺左端が変わる */
    @Test
    fun csv_align_column_is_read_by_build_figures() {
        fun topLeft(alignCol: String): com.example.trilib.PointXY {
            val doc = CsvCodec.parse("Rectangle,1,5,4,3,-1,0$alignCol\n")
            val trilist = CsvCodec.build(doc)
            val (rects, _) = figs(doc, trilist, 1f)
            return rects[0].calcPoint().b.left
        }
        val left = topLeft("")          // 列省略 → align=0 (後方互換)
        val leftExplicit0 = topLeft(",0")
        val right = topLeft(",2")
        assertEquals(left.x, leftExplicit0.x, 0.001, "align 列省略は align=0 と同値")
        assertEquals(left.y, leftExplicit0.y, 0.001, "align 列省略は align=0 と同値")
        assertTrue(kotlin.math.abs(left.x - right.x) > 0.1, "align=0 と align=2 で上辺左端 x が変わる: $left vs $right")
    }

    // ---- 段4-2 (R4): Rectangle を親にする接続 (Rectangle → Rectangle B/C/D)、9 列目 parentKind ----

    // initByParent は親辺を必ず逆走する。底辺端点は親辺端点と順序不問で一致する。
    private fun assertChildBaseOnEdge(childBase: com.jpaver.trianglelist.editmodel.Line, parentEdge: com.jpaver.trianglelist.editmodel.Line, tag: String) {
        val fwd = childBase.left.nearBy(parentEdge.left, 0.001) && childBase.right.nearBy(parentEdge.right, 0.001)
        val rev = childBase.left.nearBy(parentEdge.right, 0.001) && childBase.right.nearBy(parentEdge.left, 0.001)
        assertTrue(fwd || rev, "$tag 底辺 [${childBase.left}..${childBase.right}] が親辺 [${parentEdge.left}..${parentEdge.right}] に乗らない")
    }

    /** Rectangle チェーン B (side=1=B左脚): rect2 の底辺が rect1.getLine(1) の両端に乗る */
    @Test
    fun rect_to_rect_chain_on_edge_b() {
        val csv = "Rectangle,1,5,10,4,-1,0,0\nRectangle,2,3,4,3,1,1,0,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (rects, _) = figs(doc, trilist, 1f)
        assertEquals(2, rects.size, "Rectangle 2 個が構築される")
        assertChildBaseOnEdge(rects[1].calcPoint().a, rects[0].getLine(1), "B辺チェーン")
    }

    /** Rectangle チェーン C (side=2=C上辺): rect2 の底辺が rect1.getLine(2) の両端に乗る */
    @Test
    fun rect_to_rect_chain_on_edge_c() {
        val csv = "Rectangle,1,5,10,4,-1,0,0\nRectangle,2,3,4,3,1,2,0,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (rects, _) = figs(doc, trilist, 1f)
        assertEquals(2, rects.size)
        assertChildBaseOnEdge(rects[1].calcPoint().a, rects[0].getLine(2), "C辺チェーン")
    }

    /** Rectangle チェーン D (side=3=D右脚): rect2 の底辺が rect1.getLine(3) の両端に乗る */
    @Test
    fun rect_to_rect_chain_on_edge_d() {
        val csv = "Rectangle,1,5,10,4,-1,0,0\nRectangle,2,3,4,3,1,3,0,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (rects, _) = figs(doc, trilist, 1f)
        assertEquals(2, rects.size)
        assertChildBaseOnEdge(rects[1].calcPoint().a, rects[0].getLine(3), "D辺チェーン")
    }

    /** Rectangle 親も混在通し番号で解決する: 三角形2個の後の Rectangle #1 は mixed #3 */
    @Test
    fun rect_to_rect_after_triangles_uses_mixed_parent_number() {
        val csv = "1,1.0,1.0,1.0,-1,-1\n" +
            "2,1.0,0.8,0.8,1,1\n" +
            "Rectangle,1,1.0,1.0,0.8,2,1,0,0\n" +
            "Rectangle,2,1.0,1.0,0.8,3,2,0,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (rects, _) = figs(doc, trilist, 1f)
        assertEquals(2, rects.size, "Rectangle 2 個が構築される")
        val parentEdge = rects[0].getLine(2)
        val childBase = rects[1].calcPoint().a
        assertChildBaseOnEdge(childBase, parentEdge, "混在通し番号C辺")
    }

    /** parentKind 省略 (8 列) は親=三角形のまま不変: Rectangle の底辺が親三角形の B 辺に乗る */
    @Test
    fun parent_kind_omitted_8col_keeps_triangle_parent() {
        val csv = "1,6.0,5.0,4.0,-1,-1\nRectangle,1,5,4,3,1,1,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val (rects, _) = figs(doc, trilist, 1f)
        assertEquals(1, rects.size)
        val lp = rects[0].calcPoint()
        val parent = trilist.getBy(1)
        val onEdge = (lp.a.left.nearBy(parent.pointAB, 0.001) && lp.a.right.nearBy(parent.pointBC, 0.001)) ||
                     (lp.a.left.nearBy(parent.pointBC, 0.001) && lp.a.right.nearBy(parent.pointAB, 0.001))
        assertTrue(onEdge, "8列: 底辺 [${lp.a.left}..${lp.a.right}] が親B辺 [${parent.pointAB}..${parent.pointBC}] に乗らない")
    }

    /** parentKind=1 だが親が前方参照/範囲外なら独立 fallback */
    @Test
    fun parent_kind_rect_out_of_range_falls_back_to_independent() {
        val doc = CsvCodec.parse("Rectangle,1,5,4,3,1,1,0,1\n")
        val trilist = CsvCodec.build(doc)
        val (rects, _) = figs(doc, trilist, 1f)
        assertEquals(1, rects.size)
        assertEquals(null, rects[0].nodeA, "範囲外の Rectangle 親は独立 fallback (nodeA=null)")
    }

    /** 純三角形 golden 不変: Rectangle ゼロの三角形 CSV は build/serialize が完全同値 */
    @Test
    fun pure_triangle_csv_is_unchanged() {
        val triOnly = "1,6.0,5.0,4.0,-1,-1\n2,5.0,4.0,3.0,1,1\n3,4.0,3.5,3.0,1,2\n"
        val doc = CsvCodec.parse(triOnly)
        val (rects, _) = figs(doc, CsvCodec.build(doc), 1f)
        assertEquals(0, rects.size, "三角形のみなら Rectangle ゼロ")
        assertEquals(triOnly, CsvCodec.serialize(doc), "純三角形 CSV は serialize 同値")
    }

    /** renderCsv で Rectangle チェーン (Rectangle → Rectangle) が 2 個分の tri 線 (4本×2=8本) を出す */
    @Test
    fun render_csv_draws_rect_chain_eight_tri_lines() {
        val chainCsv = "Rectangle,1,5,10,4,-1,0,0\nRectangle,2,3,4,3,1,1,0,1\n"
        val json = WebPrimitiveRenderer.renderCsv(chainCsv, 1f)
        assertEquals(8, count(json, """"layer":"tri""""), "Rectangle 2 個で 4本×2=8 本の tri 線: $json")
        assertEquals(2, count(json, """"type":"circle""""), "番号サークル 2 個")
        assertTrue(json.contains(""""text":"1""""), "1 個目の番号 1 が出る: $json")
        assertTrue(json.contains(""""text":"2""""), "2 個目の番号 2 が出る: $json")
    }

    /** 統合採番: 三角形 N 個の後の Rectangle は N+1 から番号が続く */
    @Test
    fun rectangle_number_continues_from_triangle_count() {
        val csv = "1,6.0,5.0,4.0,-1,-1\n2,5.0,4.0,3.0,1,1\nRectangle,1,5,4,3,-1,0\n"
        val json = WebPrimitiveRenderer.renderCsv(csv, 1f)
        assertTrue(json.contains(""""text":"3""""), "Rectangle は三角形 2 個の次=番号 3 が出る: $json")
    }

    // ---- 延長 = 垂線の長さ (斜辺長ではない) + 中央/右寄せの点線ガイド ----

    /** 中央寄せ: 延長は底辺からの垂線の長さ (length=5) を出す */
    @Test
    fun rectangle_extension_dim_is_perpendicular_not_slant_leg() {
        val json = WebPrimitiveRenderer.renderCsv("Rectangle,1,5,10,7,-1,0,1\n", 1f)
        assertTrue(json.contains(""""text":" 5.0""""), "延長は垂線の長さ 5.0 を出す: $json")
        assertEquals(1, count(json, """"layer":"guide""""), "中央寄せで垂線ガイド1本: $json")
    }

    /** 左寄せ (align=0): 左脚=垂線なのでガイド線は引かない */
    @Test
    fun rectangle_no_guide_when_left_aligned() {
        val json = WebPrimitiveRenderer.renderCsv("Rectangle,1,5,4,3,-1,0,0\n", 1f)
        assertEquals(0, count(json, """"layer":"guide""""), "左寄せはガイド不要 (左脚=垂線): $json")
        assertTrue(json.contains(""""text":" 5.0""""), "延長 5.0 は左寄せでも出る: $json")
    }

    // ---- 混在接続の土台 (逆方向): 三角形を Rectangle の辺に乗せる ----

    @Test
    fun triangle_can_attach_to_rectangle_edge() {
        val rect = Rectangle(5.0, 10.0, 7.0)
        val lp = rect.calcPoint()
        val cx = (lp.a.left.x + lp.a.right.x + lp.b.left.x + lp.b.right.x) / 4f
        val cy = (lp.a.left.y + lp.a.right.y + lp.b.left.y + lp.b.right.y) / 4f
        for (side in 1..3) {
            val edge = rect.getLine(side)
            val tri = Triangle(rect as CycleShape, side, 6f, 6f)
            val onEdge = (tri.point[0].nearBy(edge.left, 0.001) && tri.pointAB.nearBy(edge.right, 0.001)) ||
                (tri.point[0].nearBy(edge.right, 0.001) && tri.pointAB.nearBy(edge.left, 0.001))
            assertTrue(onEdge, "side=$side: 三角形A辺が Rectangle 辺に乗らない p0=${tri.point[0]} pAB=${tri.pointAB} edge=$edge")
            val dx = edge.right.x - edge.left.x
            val dy = edge.right.y - edge.left.y
            val apexSide = dx * (tri.pointBC.y - edge.left.y) - dy * (tri.pointBC.x - edge.left.x)
            val centSide = dx * (cy - edge.left.y) - dy * (cx - edge.left.x)
            assertTrue(apexSide * centSide < 0f, "side=$side: 頂点が Rectangle 内部を向き重なる apexSide=$apexSide centSide=$centSide")
        }
    }

    /**
     * DXF 書き出しに Rectangle が出る。 Rectangle を含む CSV の DXF は、 三角形のみより LINE エンティティが
     * ちょうど 4 本 (Rectangle の 4 辺) 増える。
     */
    @Test
    fun dxf_export_includes_rectangle_four_lines() {
        val triOnly = WebDrawingExport.buildDxfText("1,6.0,5.0,4.0,-1,-1\n", "")
        val withRect = WebDrawingExport.buildDxfText("1,6.0,5.0,4.0,-1,-1\nRectangle,1,5,4,3,-1,0\n", "")
        assertTrue(withRect.length > triOnly.length, "Rectangle ぶん DXF が増える")
        assertEquals(count(triOnly, "AcDbLine") + 6, count(withRect, "AcDbLine"),
            "Rectangle の 4 辺 + 直角マーカー 2 本ぶん LINE エンティティが増える")
    }

    /** Rectangle 子三角形も DXF に出る */
    @Test
    fun dxf_export_includes_rect_child_triangle_lines() {
        val rectOnly = WebDrawingExport.buildDxfText("Rectangle,1,5,10,7,-1,0\n", "")
        val withChild = WebDrawingExport.buildDxfText("Rectangle,1,5,10,7,-1,0\n2,7,4,4,1,2\n", "")
        assertTrue(withChild.length > rectOnly.length, "Rectangle 子三角形ぶん DXF が増える")
        assertEquals(count(rectOnly, "AcDbLine") + 3, count(withChild, "AcDbLine"),
            "Rectangle 子三角形の 3 辺ぶん LINE エンティティが増える (DXF に Rectangle 子が出ている): $withChild")
    }

    /**
     * DXF の LINE 座標が台形幾何と一致する (座標精度テスト)。
     * Rectangle,1,length=3,widthA=4,widthB=2,-1,0 (独立, INDEP_TRAP_ANGLE=180) の calcPoint:
     *   bl=(0,0), br=(-4,≈0), tl=(≈0,-3), tr=(-2,-3)
     *   ※ br.y と tl.x は sin(π)/cos(-π/2) の浮動小数点誤差で厳密 0 でない (4.9e-13 / 1.8e-13 オーダー)
     * DXF unitscale=1000 → ×1000。traps_ は trilist の center 移動対象外なので原点起点のまま出る。
     * 各辺の「非ゼロ」座標値 (widthA/widthB/length が乗る軸) を確認する。
     */
    @Test
    fun dxf_rectangle_line_coordinates_match_geometry() {
        // 三角形を 1 個混ぜて trilist が非空にする (center 計算が安全になる)
        val dxf = WebDrawingExport.buildDxfText("1,3.0,4.0,3.0,-1,-1\nRectangle,1,3,4,2,-1,0\n", "")
        // 底辺A: bl(0,0)→br(-4000,≈0)  ※br.y は sin(π) の丸め誤差で厳密 0 でない
        assertTrue(dxf.contains("10\n0.0\n20\n0.0\n30\n0.0\n11\n-4000.0\n21"),
            "底辺A: 始点(0,0) / 終点x=-4000.0 (widthA×1000) が DXF に無い")
        // 右脚D: br(-4000,≈0)→tr(-2000,-3000)  br.y は誤差 → 始点 x のみ確認、終点は全座標確認
        val reD = Regex("10\n-4000\\.0\n20\n[^\n]+\n30\n0\\.0\n11\n-2000\\.0\n21\n-3000\\.0")
        assertTrue(reD.containsMatchIn(dxf),
            "右脚D: br.x=-4000 → tr(-2000,-3000) (widthB=2, length=3 の組合せ) が DXF に無い")
        // 上辺C: tr(-2000,-3000)→tl(≈0,-3000)  tl.x は cos(-π/2) の誤差 → 終点 y のみ確認
        val reC = Regex("10\n-2000\\.0\n20\n-3000\\.0\n30\n0\\.0\n11\n[^\n]+\n21\n-3000\\.0")
        assertTrue(reC.containsMatchIn(dxf),
            "上辺C: tr(-2000,-3000) → tl.y=-3000 が DXF に無い")
        // 左脚B: tl(≈0,-3000)→bl(0,0)  tl.x は誤差 → 終点(0,0) のみ確認
        val reB = Regex("10\n[^\n]+\n20\n-3000\\.0\n30\n0\\.0\n11\n0\\.0\n21\n0\\.0")
        assertTrue(reB.containsMatchIn(dxf),
            "左脚B: tl.y=-3000 → bl(0,0) が DXF に無い")
    }

    /** SFC の Rectangle 4 辺座標が幾何と一致する (DXF と同じ unitscale=1000、line_feature 形式) */
    @Test
    fun sfc_rectangle_line_coordinates_match_geometry() {
        val sfc = WebDrawingExport.buildSfcText("1,3.0,4.0,3.0,-1,-1\nRectangle,1,3,4,2,-1,0\n", "t.sfc")
        // 底辺A: bl(0,0)→br(-4000,≈0)  br.y は sin(π) 誤差 → 始点(0,0) と bx=-4000.0 のみ厳密確認
        val reA = Regex("""line_feature\('[^']+','[^']+','[^']+','[^']+','0\.0','0\.0','-4000\.0','[^']*'\)""")
        assertTrue(reA.containsMatchIn(sfc), "底辺A: bl(0,0)→br.x=-4000.0 が SFC に無い")
        // 右脚: br(-4000,≈0)→tr(-2000,-3000)  br.y は誤差 → 始点 x のみ確認
        val reD = Regex("""line_feature\('[^']+','[^']+','[^']+','[^']+','-4000\.0','[^']*','-2000\.0','-3000\.0'\)""")
        assertTrue(reD.containsMatchIn(sfc), "右脚: br.x=-4000 → tr(-2000,-3000) が SFC に無い")
        // 上辺: tr(-2000,-3000)→tl(≈0,-3000)  tl.x は誤差 → 終点 y のみ確認
        val reC = Regex("""line_feature\('[^']+','[^']+','[^']+','[^']+','-2000\.0','-3000\.0','[^']*','-3000\.0'\)""")
        assertTrue(reC.containsMatchIn(sfc), "上辺: tr(-2000,-3000) → tl.y=-3000 が SFC に無い")
        // 左脚: tl(≈0,-3000)→bl(0,0)  tl.x は誤差 → 終点(0,0) のみ確認
        val reB = Regex("""line_feature\('[^']+','[^']+','[^']+','[^']+','[^']*','-3000\.0','0\.0','0\.0'\)""")
        assertTrue(reB.containsMatchIn(sfc), "左脚: tl.y=-3000 → bl(0,0) が SFC に無い")
    }

    /** SFC でも Rectangle 子三角形が出る */
    @Test
    fun sfc_export_includes_rect_child_triangle_lines() {
        val rectOnly = WebDrawingExport.buildSfcText("Rectangle,1,5,10,7,-1,0\n", "t.sfc")
        val withChild = WebDrawingExport.buildSfcText("Rectangle,1,5,10,7,-1,0\n2,7,4,4,1,2\n", "t.sfc")
        assertTrue(withChild.length > rectOnly.length, "Rectangle 子三角形ぶん SFC が増える")
    }

    /** Rectangle 子 chain: Rectangle → 子三角形 → 更にその子三角形 が新 schema で建つ */
    @Test
    fun rect_child_chain_can_be_built_from_rect_child_parent() {
        val csv = "Rectangle,1,5,10,7,-1,0\n" +
            "2,7,4,4,1,2\n" +     // Rectangle #1 の C 辺 (side=2) に乗る子三角形
            "3,4,3,3,2,1\n"       // 子三角形 #1 を親に新しい子三角形 (chain 2 段目)
        val doc = CsvCodec.parse(csv)
        assertEquals(0, doc.triRows().size, "普通三角形行は無し (全て Rectangle 子)")
        assertEquals(1, doc.rectRows().size, "Rectangle 1")
        assertEquals(2, doc.rectChildTriRows().size, "Rectangle 子 chain で 2 行とも Rectangle 子バケツへ振り分け")
        val trilist = CsvCodec.build(doc)
        val (rects, rectChildTris) = figs(doc, trilist, 1f)
        assertEquals(1, rects.size, "Rectangle 1")
        assertEquals(2, rectChildTris.size, "Rectangle 子 chain 2 個建つ (1 番目= Rectangle 親、 2 番目=子 Triangle 親)")
        // 2 番目の三角形の底辺が 1 番目 (= Rectangle 子 #1) の B 辺 (side=1) に乗る
        val tt1 = rectChildTris[0]
        val tt2 = rectChildTris[1]
        val edge = tt1.getLine(1) // 1 番目の B 辺
        val onEdge = (tt2.point[0].nearBy(edge.left, 0.001) && tt2.pointAB.nearBy(edge.right, 0.001)) ||
            (tt2.point[0].nearBy(edge.right, 0.001) && tt2.pointAB.nearBy(edge.left, 0.001))
        assertTrue(onEdge, "2 番目の底辺が 1 番目 Rectangle 子の B 辺に乗らない: tt2.p0=${tt2.point[0]} tt2.pAB=${tt2.pointAB} edge=$edge")
    }

    /** 混在通し番号の安全網: 三角形2 + Rectangle 1 + Rectangle 子 1 を CSV 順で書くと、番号は 1,2,3,4 が連続 */
    @Test
    fun mixed_drawing_numbers_are_continuous_across_kinds() {
        // 三角形 2 + Rectangle 1 + Rectangle 子 1。 Rectangle 子の parent = trilist.size()(=2) + rect idx(=1) = 3
        val csv = "1,6.0,5.0,4.0,-1,-1\n2,5.0,4.0,3.0,1,1\n" +
            "Rectangle,1,5,4,3,1,1\n3,3,3,3,3,2\n"
        val json = WebPrimitiveRenderer.renderCsv(csv, 1f)
        assertTrue(json.contains(""""text":"1""""), "三角形1 → 番号 1: $json")
        assertTrue(json.contains(""""text":"2""""), "三角形2 → 番号 2: $json")
        assertTrue(json.contains(""""text":"3""""), "Rectangle → 番号 3 (三角形 2 個の後): $json")
        assertTrue(json.contains(""""text":"4""""), "Rectangle 子 → 番号 4 (三角形 + Rectangle の後): $json")
        assertEquals(4, count(json, """"type":"circle""""), "番号サークル 4 個 (三角形 2 + Rectangle 1 + Rectangle 子 1): $json")
    }

    /** 混在順 build の安全網: 三角形 → Rectangle → Rectangle 子 → 三角形 のように CSV 出現順がバラついても全部建つ */
    @Test
    fun mixed_order_csv_builds_all_figures() {
        // 三角形 1 → Rectangle → Rectangle 子 (parent=3 = 最終 trilist.size()(=2) + rect idx(=1)) → 三角形 2
        val csv = "1,6.0,5.0,4.0,-1,-1\n" +
            "Rectangle,1,5,4,3,1,1\n" +
            "3,3,3,3,3,2\n" +
            "2,5.0,4.0,3.0,1,1\n"
        val doc = CsvCodec.parse(csv)
        assertEquals(4, doc.figureRows.size, "figureRows に 4 行が CSV 出現順で揃う")
        val trilist = CsvCodec.build(doc)
        val (rects, rectChildTris) = figs(doc, trilist, 1f)
        assertEquals(2, trilist.size(), "三角形 2 個 (CSV 順がバラついても両方建つ)")
        assertEquals(1, rects.size, "Rectangle 1 個")
        assertEquals(1, rectChildTris.size, "Rectangle 子三角形 1 個")
    }

    /** 位置順ビルドの土台: parse が図形行を CSV 出現順で保持する */
    @Test
    fun parse_preserves_figure_row_order() {
        val csv = "1,6.0,5.0,4.0,-1,-1\nRectangle,1,5,4,3,1,1\n2,5.0,4.0,3.0,1,1\n"
        val doc = CsvCodec.parse(csv)
        val kinds = doc.figureRows.map { if (it.chunks.firstOrNull() == "Rectangle") "rect" else "tri" }
        assertEquals(listOf("tri", "rect", "tri"), kinds, "図形行は CSV 出現順 (三角形 → Rectangle → 三角形): $kinds")
        // 既存の種別分離も不変 (三角形 2 + Rectangle 1)
        assertEquals(2, doc.triRows().size)
        assertEquals(1, doc.rectRows().size)
    }

    @Test
    fun overrides_use_mixed_numbers_when_rectangle_precedes_triangle() {
        val csv = "1,6.0,5.0,4.0,-1,-1\n" +
            "Rectangle,1,5,4,3,1,1\n" +
            "2,5.0,4.0,3.0,1,1\n"
        val base = CsvCodec.parse(WebDrawingExport.buildCsvText(csv, "")).figureRows[2].chunks

        val rectTarget = CsvCodec.parse(
            WebDrawingExport.buildCsvText(csv, """{"dims":[{"tri":2,"side":1,"h":4,"v":3}]}"""),
        ).figureRows[2].chunks
        assertEquals(base[12], rectTarget[12], "mixed #2 は Rectangle なので後続 Triangle の B 横配置へ誤適用しない")
        assertEquals(base[15], rectTarget[15], "mixed #2 は Rectangle なので後続 Triangle の B 縦配置へ誤適用しない")
        assertEquals(base[20], rectTarget[20], "mixed #2 は Rectangle なので後続 Triangle の手動フラグへ誤適用しない")

        val triTarget = CsvCodec.parse(
            WebDrawingExport.buildCsvText(csv, """{"dims":[{"tri":3,"side":1,"h":4,"v":3}]}"""),
        ).figureRows[2].chunks
        assertEquals("4", triTarget[12], "mixed #3 の後続 Triangle に B 横配置 override が当たる")
        assertEquals("3", triTarget[15], "mixed #3 の後続 Triangle に B 縦配置 override が当たる")
        assertEquals("true", triTarget[20], "mixed #3 の後続 Triangle に手動フラグが立つ")
    }

    @Test
    fun csv_export_bakes_rect_child_triangle_overrides() {
        val csv = "Rectangle,1,5,10,7,-1,0\n2,7,4,4,1,2\n"
        val baked = CsvCodec.parse(
            WebDrawingExport.buildCsvText(csv, """{"dims":[{"tri":2,"side":1,"h":4,"v":3}]}"""),
        )
        val child = baked.figureRows[1].chunks
        assertEquals("4", child[12], "Rectangle 子 Triangle の B 横配置 override を元の Triangle 行へ焼く")
        assertEquals("3", child[15], "Rectangle 子 Triangle の B 縦配置 override を元の Triangle 行へ焼く")
        assertEquals("true", child[20], "Rectangle 子 Triangle の手動フラグを元の Triangle 行へ焼く")
    }
}
