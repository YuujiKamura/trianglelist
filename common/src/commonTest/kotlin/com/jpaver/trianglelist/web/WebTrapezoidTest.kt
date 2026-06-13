package com.jpaver.trianglelist.web

import com.jpaver.trianglelist.datamanager.CsvCodec
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
        // 番号サークル 1 + 番号 "T1" + 3 寸法
        assertEquals(1, count(json, """"type":"circle""""))
        assertTrue(json.contains(""""text":"T1""""), "番号 T1 が出る: $json")
        assertEquals(3, count(json, """"layer":"dim""""), "底辺A・上辺C・延長B の 3 寸法: $json")
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
}
