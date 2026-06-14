package com.jpaver.trianglelist.web

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.EditObject
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 混在リスト段1 (B1): CSV の Trapezoid 行 → Rectangle 構築 → 台形描画のテスト。
 * contract は trap-design.md (`Trapezoid, num, length, widthA, widthB, parent, side`)。
 */
class WebTrapezoidTest {

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
        val traps = CsvCodec.buildTrapezoids(doc, trilist, 1f)
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
        val traps = CsvCodec.buildTrapezoids(doc, trilist, 1f)
        assertEquals(1, traps.size)
        val lp = traps[0].calcPoint()
        // side=1 = 親のB辺 = Line(pointAB, pointBC) (TriangleUtilitiesExtensions.kt:145-147)
        val parent = trilist.getBy(1)
        assertTrue(lp.a.left.nearBy(parent.pointAB, 0.001), "底辺左 ${lp.a.left} != 親pointAB ${parent.pointAB}")
        assertTrue(lp.a.right.nearBy(parent.pointBC, 0.001), "底辺右 ${lp.a.right} != 親pointBC ${parent.pointBC}")
    }

    /**
     * 台形を親に持つ三角形 (TriTrap 行)。三角形の底辺(A)が台形の指定辺 (上辺C=side2) にぴったり乗る。
     * かつ TriTrap 行は rows に入らない → build() (golden パイプライン) が一切見ない。
     * これが「台形に三角形を接続」の本体 (三角形ビルドが台形より先で親を参照できない循環を、
     * 台形ビルド後の buildTrapParentedTriangles で解く)。
     */
    @Test
    fun triangle_can_be_child_of_trapezoid() {
        // 独立台形 (延長5/底辺10/上辺7) の上辺C(side2) に三角形(B=4,C=4)を接続
        val csv = "Trapezoid,1,5,10,7,-1,0\nTriTrap,2,7,4,4,1,2\n"
        val doc = CsvCodec.parse(csv)
        assertEquals(1, doc.trapParentedTriRows.size, "TriTrap 行が別バケツに入る")
        assertEquals(0, doc.rows.size, "TriTrap は普通の三角形行(rows)に入らない → build は見ない (golden)")
        val trilist = CsvCodec.build(doc)
        val traps = CsvCodec.buildTrapezoids(doc, trilist, 1f)
        assertEquals(1, traps.size)
        val tris = CsvCodec.buildTrapParentedTriangles(doc, traps, 1f)
        assertEquals(1, tris.size, "台形を親に三角形が1個建つ")
        val edge = traps[0].getLine(2) // 上辺C
        val tri = tris[0]
        // 底辺が上辺Cに乗る。向きは問わない (頂点を外へ出すため base を反転する場合があるので、
        // 始点/終点が辺の両端のどちらに当たってもよい — 本質は「底辺辺が上辺と一致」)。
        val onEdge = (tri.point[0].nearBy(edge.left, 0.001) && tri.pointAB.nearBy(edge.right, 0.001)) ||
            (tri.point[0].nearBy(edge.right, 0.001) && tri.pointAB.nearBy(edge.left, 0.001))
        assertTrue(onEdge, "三角形の底辺が台形上辺に乗らない: p0=${tri.point[0]} pAB=${tri.pointAB} edge=$edge")
        // 頂点 (pointBC) は親台形の外側に出る (重なり防止が本フィックスの核)。base 線に対して
        // 頂点と台形重心が反対側 (cross 積が異符号) なら外向き。
        val lp = traps[0].calcPoint()
        val cx = (lp.a.left.x + lp.a.right.x + lp.b.left.x + lp.b.right.x) / 4f
        val cy = (lp.a.left.y + lp.a.right.y + lp.b.left.y + lp.b.right.y) / 4f
        val dx = edge.right.x - edge.left.x
        val dy = edge.right.y - edge.left.y
        val apexSide = dx * (tri.pointBC.y - edge.left.y) - dy * (tri.pointBC.x - edge.left.x)
        val centSide = dx * (cy - edge.left.y) - dy * (cx - edge.left.x)
        assertTrue(apexSide * centSide < 0f, "頂点が台形内部を向き重なっている: apexSide=$apexSide centSide=$centSide")
    }

    /** TriTrap 行が描画される: 台形(4辺) + 台形子三角形(3辺) = tri 線 7 本、番号サークル 2 (台形 + 子三角形) */
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
        // base は "]" で閉じる。その手前までが withTrap の先頭に丸ごと一致すれば、
        // 既存 prim は 1 つも動いていない (台形は配列末尾に足されただけ)
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
    // 底辺=10, 上辺=4, 延長=3, angle=0 (水平底辺 (0,0)→(10,0))。上辺は y=3 (延長ぶん上)。
    // 左寄せ=上辺左端が底辺左端(x=0)の真上、右寄せ=上辺右端が底辺右端(x=10)の真上、中央=左右対称。

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

    /** CSV 8列目 align を buildTrapezoids が読む。省略時0、左右で上辺左端が変わる */
    @Test
    fun csv_align_column_is_read_by_build_trapezoids() {
        // angle=180 (INDEP_TRAP_ANGLE) の独立台形。align で leftB→baseline.left の延長辺長は不変(=length)
        // だが leftB の位置が変わるので、align=0 と align=2 で上辺左端座標が一致しないことを確認。
        fun topLeft(alignCol: String) =
            CsvCodec.buildTrapezoids(
                CsvCodec.parse("Trapezoid,1,5,4,3,-1,0$alignCol\n"),
                CsvCodec.build(CsvCodec.parse("Trapezoid,1,5,4,3,-1,0\n")),
                1f,
            )[0].calcPoint().b.left
        val left = topLeft("")          // 列省略 → align=0 (後方互換)
        val leftExplicit0 = topLeft(",0")
        val right = topLeft(",2")
        assertEquals(left.x, leftExplicit0.x, 0.001, "align 列省略は align=0 と同値")
        assertEquals(left.y, leftExplicit0.y, 0.001, "align 列省略は align=0 と同値")
        assertTrue(kotlin.math.abs(left.x - right.x) > 0.1, "align=0 と align=2 で上辺左端 x が変わる: $left vs $right")
    }

    // ---- 段4-2 (R4): 台形を親にする接続 (台形→台形 B/C/D)、9 列目 parentKind ----
    // trap1 = 独立 (length=5, widthA=10, widthB=4, angle=180)。trap2 が trap1 の辺に底辺を乗せる。
    // parentKind=1 で parent は台形番号 (構築順 1 始まり)。side 1=B左脚/2=C上辺/3=D右脚 (Rectangle.getLine 規約)。

    /** 台形チェーン B (side=1=B左脚): trap2 の底辺が trap1.getLine(1) の両端に乗る */
    @Test
    fun trap_to_trap_chain_on_edge_b() {
        val csv = "Trapezoid,1,5,10,4,-1,0,0\nTrapezoid,2,3,4,3,1,1,0,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val traps = CsvCodec.buildTrapezoids(doc, trilist, 1f)
        assertEquals(2, traps.size, "台形 2 個が構築される")
        val parentEdge = traps[0].getLine(1)   // trap1 の B 左脚
        val childBase = traps[1].calcPoint().a   // trap2 の底辺
        assertTrue(childBase.left.nearBy(parentEdge.left, 0.001), "底辺左 ${childBase.left} != 親B辺左 ${parentEdge.left}")
        assertTrue(childBase.right.nearBy(parentEdge.right, 0.001), "底辺右 ${childBase.right} != 親B辺右 ${parentEdge.right}")
    }

    /** 台形チェーン C (side=2=C上辺): trap2 の底辺が trap1.getLine(2) の両端に乗る */
    @Test
    fun trap_to_trap_chain_on_edge_c() {
        val csv = "Trapezoid,1,5,10,4,-1,0,0\nTrapezoid,2,3,4,3,1,2,0,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val traps = CsvCodec.buildTrapezoids(doc, trilist, 1f)
        assertEquals(2, traps.size)
        val parentEdge = traps[0].getLine(2)   // trap1 の C 上辺
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
        val traps = CsvCodec.buildTrapezoids(doc, trilist, 1f)
        assertEquals(2, traps.size)
        val parentEdge = traps[0].getLine(3)   // trap1 の D 右脚
        val childBase = traps[1].calcPoint().a
        assertTrue(childBase.left.nearBy(parentEdge.left, 0.001), "底辺左 ${childBase.left} != 親D辺左 ${parentEdge.left}")
        assertTrue(childBase.right.nearBy(parentEdge.right, 0.001), "底辺右 ${childBase.right} != 親D辺右 ${parentEdge.right}")
    }

    /** parentKind 省略 (8 列) は親=三角形のまま不変: trap の底辺が親三角形の B 辺に乗る (R2 と同挙動) */
    @Test
    fun parent_kind_omitted_8col_keeps_triangle_parent() {
        // 三角形1 + 8 列の台形 (parent=1, side=1, align=1, parentKind 無し)。親は三角形 1 のまま。
        val csv = "1,6.0,5.0,4.0,-1,-1\nTrapezoid,1,5,4,3,1,1,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val traps = CsvCodec.buildTrapezoids(doc, trilist, 1f)
        assertEquals(1, traps.size)
        val lp = traps[0].calcPoint()
        val parent = trilist.getBy(1)
        // side=1 = 親三角形の B 辺 = Line(pointAB, pointBC)
        assertTrue(lp.a.left.nearBy(parent.pointAB, 0.001), "8列: 底辺左 ${lp.a.left} != 親pointAB ${parent.pointAB}")
        assertTrue(lp.a.right.nearBy(parent.pointBC, 0.001), "8列: 底辺右 ${lp.a.right} != 親pointBC ${parent.pointBC}")
    }

    /** parentKind=1 だが親が前方参照/範囲外なら独立 fallback (例外を投げず描ける) */
    @Test
    fun parent_kind_trap_out_of_range_falls_back_to_independent() {
        // trap1 が parentKind=1 で parent=1 を指す = 自分自身/前方参照 (pIdx=0 だが構築中で result 空) → 独立
        val doc = CsvCodec.parse("Trapezoid,1,5,4,3,1,1,0,1\n")
        val trilist = CsvCodec.build(doc)
        val traps = CsvCodec.buildTrapezoids(doc, trilist, 1f)
        assertEquals(1, traps.size)
        // 独立構築なら nodeA は null
        assertEquals(null, traps[0].nodeA, "範囲外の台形親は独立 fallback (nodeA=null)")
    }

    /** 純三角形 golden 不変: 台形ゼロの三角形 CSV は build/serialize が完全同値 (台形は素通し) */
    @Test
    fun pure_triangle_csv_is_unchanged() {
        val triOnly = "1,6.0,5.0,4.0,-1,-1\n2,5.0,4.0,3.0,1,1\n3,4.0,3.5,3.0,1,2\n"
        val doc = CsvCodec.parse(triOnly)
        val traps = CsvCodec.buildTrapezoids(doc, CsvCodec.build(doc), 1f)
        assertEquals(0, traps.size, "三角形のみなら台形ゼロ")
        // serialize は三角形行のみで完全同値
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

    /** 統合採番: 三角形 N 個の後の台形は N+1 から番号が続く (図形種別を意識しない通し番号) */
    @Test
    fun trapezoid_number_continues_from_triangle_count() {
        // 三角形 2 個 (番号 1,2) + 独立台形 1 個 → 台形は番号 3
        val csv = "1,6.0,5.0,4.0,-1,-1\n2,5.0,4.0,3.0,1,1\nTrapezoid,1,5,4,3,-1,0\n"
        val json = WebPrimitiveRenderer.renderCsv(csv, 1f)
        assertTrue(json.contains(""""text":"3""""), "台形は三角形2個の次=番号3が出る: $json")
        // "T3" のような接頭辞付きは出ない (誤った別系列採番の残骸チェック)
        assertTrue(!json.contains(""""text":"T"""), "T接頭辞付き番号は出ない: $json")
    }

    // ---- 延長 = 垂線の長さ (斜辺長ではない) + 中央/右寄せの点線ガイド ----

    /** 中央寄せ: 延長は底辺からの垂線の長さ (length=5) を出す。左脚は斜め (≈5.22) だが延長は5。 */
    @Test
    fun trapezoid_extension_dim_is_perpendicular_not_slant_leg() {
        // 底辺10/上辺7/延長5, align=1(中央)。上辺左端 x=(10-7)/2=1.5 → 左脚は斜辺、延長は垂線=5。
        val json = WebPrimitiveRenderer.renderCsv("Trapezoid,1,5,10,7,-1,0,1\n", 1f)
        // formattedString(2): .x0 の値は小数1桁+先頭スペース → " 5.0" (底辺" 10.0"/上辺" 7.0"と区別)
        assertTrue(json.contains(""""text":" 5.0""""), "延長は垂線の長さ 5.0 を出す: $json")
        // 中央寄せでは左脚が斜辺になるので、垂線の点線ガイド (layer "guide") を別に引く
        assertEquals(1, count(json, """"layer":"guide""""), "中央寄せで垂線ガイド1本: $json")
    }

    /** 左寄せ (align=0): 左脚=垂線なのでガイド線は引かない (脚そのものが延長を示す) */
    @Test
    fun trapezoid_no_guide_when_left_aligned() {
        val json = WebPrimitiveRenderer.renderCsv("Trapezoid,1,5,4,3,-1,0,0\n", 1f)
        assertEquals(0, count(json, """"layer":"guide""""), "左寄せはガイド不要 (左脚=垂線): $json")
        assertTrue(json.contains(""""text":" 5.0""""), "延長 5.0 は左寄せでも出る: $json")
    }

    // ---- 混在接続の土台 (逆方向): 三角形を台形の辺に乗せる ----

    /**
     * 三角形が台形 (EditObject) の任意の辺に底辺(A)を乗せて構築できる。
     * Triangle(parent: EditObject, side, B, C) が共通の継ぎ目 initByParent→getLine(side) を使うので、
     * build()/buildTrapezoids() の別リスト・順序とは独立に、三角形側でも台形を親に取れることを直接検証。
     */
    @Test
    fun triangle_can_attach_to_trapezoid_edge() {
        val trap = Rectangle(5.0, 10.0, 7.0)   // 独立台形 (延長5/底辺10/上辺7、angle=0)
        val lp = trap.calcPoint()
        val cx = (lp.a.left.x + lp.a.right.x + lp.b.left.x + lp.b.right.x) / 4f
        val cy = (lp.a.left.y + lp.a.right.y + lp.b.left.y + lp.b.right.y) / 4f
        for (side in 1..3) {
            val edge = trap.getLine(side)
            val tri = Triangle(trap as EditObject, side, 6f, 6f)
            // A辺 (point[0]→pointAB) が台形辺の両端に乗る。向きは問わない (頂点を外へ出すため反転し得る)。
            val onEdge = (tri.point[0].nearBy(edge.left, 0.001) && tri.pointAB.nearBy(edge.right, 0.001)) ||
                (tri.point[0].nearBy(edge.right, 0.001) && tri.pointAB.nearBy(edge.left, 0.001))
            assertTrue(onEdge, "side=$side: 三角形A辺が台形辺に乗らない p0=${tri.point[0]} pAB=${tri.pointAB} edge=$edge")
            // 頂点は台形の外側に出る (重なり防止)。base 線に対し頂点と台形重心が反対側 (cross 積が異符号)。
            val dx = edge.right.x - edge.left.x
            val dy = edge.right.y - edge.left.y
            val apexSide = dx * (tri.pointBC.y - edge.left.y) - dy * (tri.pointBC.x - edge.left.x)
            val centSide = dx * (cy - edge.left.y) - dy * (cx - edge.left.x)
            assertTrue(apexSide * centSide < 0f, "side=$side: 頂点が台形内部を向き重なる apexSide=$apexSide centSide=$centSide")
        }
    }

    /**
     * 出力の穴を塞ぐ: DXF 書き出しに台形が出る。台形を含む CSV の DXF は、三角形のみより LINE
     * エンティティ (AcDbLine) がちょうど 4 本 (台形の4辺) 増える。DrawPrim 経由 (buildTrapezoidPrims)
     * なので backend (DxfFileWriter) を触らず出る (ADR 0010)。三角形のみの byte 不変は app の golden test が担保。
     */
    @Test
    fun dxf_export_includes_trapezoid_four_lines() {
        val triOnly = WebDrawingExport.buildDxfText("1,6.0,5.0,4.0,-1,-1\n", "")
        val withTrap = WebDrawingExport.buildDxfText("1,6.0,5.0,4.0,-1,-1\nTrapezoid,1,5,4,3,-1,0\n", "")
        assertTrue(withTrap.length > triOnly.length, "台形ぶん DXF が増える")
        assertEquals(count(triOnly, "AcDbLine") + 4, count(withTrap, "AcDbLine"),
            "台形の4辺ぶん LINE エンティティが増える (DXF に台形が出ている)")
    }

    /** TriTrap (台形を親に持つ三角形) も DXF に出る。user 2026-06-14「複合図形でも図面形式へのセーブと
     *  ロードが出来ること」── これまでは DXF writer の trapTris_ ループが無く、TriTrap 行が無視されていた。 */
    @Test
    fun dxf_export_includes_trap_parented_triangle_lines() {
        val trapOnly = WebDrawingExport.buildDxfText("Trapezoid,1,5,10,7,-1,0\n", "")
        val withChild = WebDrawingExport.buildDxfText("Trapezoid,1,5,10,7,-1,0\nTriTrap,2,7,4,4,1,2\n", "")
        assertTrue(withChild.length > trapOnly.length, "TriTrap ぶん DXF が増える")
        assertEquals(count(trapOnly, "AcDbLine") + 3, count(withChild, "AcDbLine"),
            "台形子三角形の 3 辺ぶん LINE エンティティが増える (DXF に TriTrap が出ている): $withChild")
    }

    /** SFC でも TriTrap が出る (DXF と同じ思想)。SfcWriter の trapTris_ ループ確認。 */
    @Test
    fun sfc_export_includes_trap_parented_triangle_lines() {
        val trapOnly = WebDrawingExport.buildSfcText("Trapezoid,1,5,10,7,-1,0\n", "t.sfc")
        val withChild = WebDrawingExport.buildSfcText("Trapezoid,1,5,10,7,-1,0\nTriTrap,2,7,4,4,1,2\n", "t.sfc")
        assertTrue(withChild.length > trapOnly.length, "TriTrap ぶん SFC が増える")
    }

    /** TriTrap タグ廃止: 普通三角形行で「parent が三角形数を超える」と台形親と解釈される。
     *  旧 TriTrap 形式と新形式 (普通三角形行) で内部 build 結果が同じであることを pin。
     *  user 2026-06-14「TriTrap みたいな妙なデータ型は廃止しろ」── CSV schema の cleanup。 */
    @Test
    fun new_schema_triangle_row_with_trap_parent_is_equivalent_to_legacy_tritrap() {
        // 旧形式: TriTrap タグ。trap idx=1, side=2
        val legacy = "Trapezoid,1,5,10,7,-1,0\nTriTrap,2,7,4,4,1,2\n"
        // 新形式: 普通三角形行で parent=1 (三角形 0 個 + 台形 idx 1 = 混在通し番号 1)
        val modern = "Trapezoid,1,5,10,7,-1,0\n2,7,4,4,1,2\n"
        val legacyDoc = CsvCodec.parse(legacy)
        val modernDoc = CsvCodec.parse(modern)
        assertEquals(1, legacyDoc.trapParentedTriRows.size, "旧形式: TriTrap 行が別バケツ")
        assertEquals(1, modernDoc.trapParentedTriRows.size, "新形式: 普通三角形行 → 内部で同じバケツに振り分け")
        assertEquals(0, modernDoc.rows.size, "新形式: 台形親の三角形は rows に入らない (golden 不変)")
        // build 結果も同値
        val legacyMixed = CsvCodec.buildMixed(legacyDoc, 1f)
        val modernMixed = CsvCodec.buildMixed(modernDoc, 1f)
        assertEquals(legacyMixed.traps.size, modernMixed.traps.size)
        assertEquals(legacyMixed.trapTris.size, modernMixed.trapTris.size)
        assertEquals(1, modernMixed.trapTris.size, "新形式でも台形子三角形が 1 個建つ")
    }

    /** TriTrap chain: 台形 → 台形子三角形 → 更にその子三角形 が新 schema で建つ。
     *  parent 混在通し番号で「parent > 三角形数 + 台形数」なら親が TriTrap (chain) と解釈。
     *  user 2026-06-15「台形にくっついてる三角形からも派生したい」── tritrap → tritrap の chain。 */
    @Test
    fun tritrap_chain_can_be_built_from_tritrap_parent() {
        // 台形1 (混在通し 1) → 子三角形 (混在通し 2、parent=1=台形) → 更に孫三角形 (混在通し 3、parent=2=子三角形)
        val csv = "Trapezoid,1,5,10,7,-1,0\n" +
            "2,7,4,4,1,2\n" +     // 台形 #1 の C 辺 (side=2) に乗る子三角形 (= TriTrap #1)
            "3,4,3,3,2,1\n"       // TriTrap #1 を親に新しい子三角形 (= TriTrap #2 chain)、parent=2 (混在通し)
        val doc = CsvCodec.parse(csv)
        assertEquals(0, doc.rows.size, "普通三角形行は無し (全て台形子) ")
        assertEquals(1, doc.trapRows.size, "台形 1")
        assertEquals(2, doc.trapParentedTriRows.size, "tritrap chain で 2 行とも台形子バケツへ振り分け")
        val mixed = CsvCodec.buildMixed(doc, 1f)
        assertEquals(1, mixed.traps.size, "台形 1")
        assertEquals(2, mixed.trapTris.size, "tritrap chain 2 個建つ (1 番目=台形親、2 番目=tritrap親)")
        // 2 番目の三角形の底辺が 1 番目 (= TriTrap #1) の B 辺 (side=1) に乗る
        val tt1 = mixed.trapTris[0]
        val tt2 = mixed.trapTris[1]
        val edge = tt1.getLine(1) // 1 番目 tritrap の B 辺
        val onEdge = (tt2.point[0].nearBy(edge.left, 0.001) && tt2.pointAB.nearBy(edge.right, 0.001)) ||
            (tt2.point[0].nearBy(edge.right, 0.001) && tt2.pointAB.nearBy(edge.left, 0.001))
        assertTrue(onEdge, "2 番目の底辺が 1 番目 TriTrap の B 辺に乗らない: tt2.p0=${tt2.point[0]} tt2.pAB=${tt2.pointAB} edge=$edge")
    }

    /** TriTrap タグの round-trip: 旧形式を読み、書き戻すと TriTrap タグが消えて普通三角形行になる。
     *  再度 parse すると内部表現が同じに戻る。これで「古い CSV を読んで新しい CSV を書く」マイグレーションが成立。 */
    @Test
    fun tritrap_serialize_writes_modern_triangle_row_and_round_trips() {
        val legacy = "Trapezoid,1,5,10,7,-1,0\nTriTrap,2,7,4,4,1,2\n"
        val written = CsvCodec.serialize(CsvCodec.parse(legacy))
        assertTrue(!written.contains("TriTrap"), "新規書き出しに TriTrap タグは含まない: $written")
        assertTrue(written.contains("2,7,4,4,1,2"), "普通三角形行形式 (num=2, A=7, B=4, C=4, parent=1, side=2): $written")
        // 再 parse で同じ内部表現
        val reparsed = CsvCodec.parse(written)
        assertEquals(1, reparsed.trapParentedTriRows.size, "再 parse で台形親三角形が同じ場所に振り分けられる")
        assertEquals(1, reparsed.trapRows.size, "台形は不変")
    }

    /** 混在通し番号の安全網: 三角形2 + 台形1 + TriTrap1 を CSV 順で書くと、描画上の番号は
     *  1,2,3,4 が連続して出る (三角形→台形→TriTrap の規約)。これを破ると DXF / SFC / XLSX
     *  の番号付けが食い違うので、build 統合作業の前にここで pin しておく。 */
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

    /** 混在順 build の安全網: 三角形→台形→TriTrap→三角形 のように CSV 出現順がバラついても、
     *  既存 build パイプライン (rows / trapRows / trapParentedTriRows の 3 分離) で全部建つ。
     *  build を figureRows 1 パス依存解決に書き換える時、このテストが green であり続けることが
     *  「順不同 build に統合しても結果が同じ」の証拠になる。 */
    @Test
    fun mixed_order_csv_builds_all_figures() {
        val csv = "1,6.0,5.0,4.0,-1,-1\n" +
            "Trapezoid,1,5,4,3,1,1\n" +
            "TriTrap,2,3,3,3,1,2\n" +
            "2,5.0,4.0,3.0,1,1\n"
        val doc = CsvCodec.parse(csv)
        assertEquals(4, doc.figureRows.size, "figureRows に 4 行が CSV 出現順で揃う")
        val trilist = CsvCodec.build(doc)
        val traps = CsvCodec.buildTrapezoids(doc, trilist, 1f)
        val trapTris = CsvCodec.buildTrapParentedTriangles(doc, traps, 1f)
        assertEquals(2, trilist.size(), "三角形 2 個 (CSV 順がバラついても両方建つ)")
        assertEquals(1, traps.size, "台形 1 個")
        assertEquals(1, trapTris.size, "台形子三角形 1 個")
    }

    /** 位置順ビルドの土台: parse が図形行を CSV 出現順で保持する (rows/trapRows 分離では失われる行順)。
     *  三角形→台形→三角形 の混在で、figureRows がその順序を保つ (親が子より先に在ることを後で使う)。 */
    @Test
    fun parse_preserves_figure_row_order() {
        val csv = "1,6.0,5.0,4.0,-1,-1\nTrapezoid,1,5,4,3,1,1\n2,5.0,4.0,3.0,1,1\n"
        val doc = CsvCodec.parse(csv)
        val kinds = doc.figureRows.map { if (it.chunks.firstOrNull() == "Trapezoid") "trap" else "tri" }
        assertEquals(listOf("tri", "trap", "tri"), kinds, "図形行は CSV 出現順 (三角形→台形→三角形): $kinds")
        // 既存の種別分離も不変 (三角形2 + 台形1)
        assertEquals(2, doc.rows.size)
        assertEquals(1, doc.trapRows.size)
    }
}
