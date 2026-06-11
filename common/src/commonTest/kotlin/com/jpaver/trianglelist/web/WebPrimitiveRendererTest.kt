package com.jpaver.trianglelist.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Web 段階1 (wasmJs facade) の中身 = commonMain 側の CSV→プリミティブ JSON 経路のテスト。
 * fixture は desktop/sample/sample_triangles.csv と同形 (最小 6 カラム形式、7 三角形)。
 */
class WebPrimitiveRendererTest {

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
    """.trimIndent()

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
    fun reader_builds_seven_triangles_with_lengths() {
        val trilist = WebCsvReader.read(sampleCsv)
        assertEquals(7, trilist.size())
        val t1 = trilist.getBy(1)
        assertEquals(6.0f, t1.lengthA_, 0.001f)
        assertEquals(5.0f, t1.lengthB_, 0.001f)
        assertEquals(4.0f, t1.lengthC_, 0.001f)
    }

    @Test
    fun reader_connects_child_a_edge_to_parent_b_edge() {
        val trilist = WebCsvReader.read(sampleCsv)
        val parent = trilist.getBy(1)
        val child = trilist.getBy(2) // 接続タイプ 1 = 親のB辺 (pointAB-pointBC)
        // 子のA辺 (point[0]-pointAB) の両端点が、親のB辺の両端点と一致する (順不同)
        val childEnds = listOf(child.point[0], child.pointAB)
        val parentEnds = listOf(parent.pointAB, parent.pointBC)
        val matched = parentEnds.all { pe -> childEnds.any { ce -> ce.nearBy(pe, 0.001) } }
        assertTrue(matched, "child A-edge ${childEnds.joinToString()} != parent B-edge ${parentEnds.joinToString()}")
    }

    @Test
    fun reader_applies_list_angle_via_recover_state() {
        val base = "1,6.0,5.0,4.0,-1,-1\n"
        // ListAngle 行なし → angle 0 (アプリ createNew と同じ向き、180° 基底から -180° 回転)
        val t0 = WebCsvReader.read(base).getBy(1)
        assertEquals(0f, t0.angle, 0.001f)
        // 角度 0 = A辺が +x 方向: pointAB は (lenA, 0)
        assertEquals(6.0, t0.pointAB.x, 0.001)
        assertEquals(0.0, t0.pointAB.y, 0.001)
        // ListAngle 180 → 180° 基底のまま (= recoverState 導入前の web と同じ向き)
        val t180 = WebCsvReader.read(base + "ListAngle, 180\n").getBy(1)
        assertEquals(180f, t180.angle, 0.001f)
        assertEquals(-6.0, t180.pointAB.x, 0.001)
        // 任意角度: ListAngle = 三角形1 の絶対角度 (4.11.csv で列22 と同値、その pin)
        val t90 = WebCsvReader.read(base + "ListAngle, 90\n").getBy(1)
        assertEquals(90f, t90.angle, 0.001f)
        assertEquals(0.0, t90.pointAB.x, 0.001)
        assertEquals(6.0, t90.pointAB.y, 0.001)
    }

    @Test
    fun render_emits_expected_primitive_counts() {
        val json = WebPrimitiveRenderer.renderCsv(sampleCsv, 1f)
        // 辺: 7 三角形 × 3 本
        assertEquals(21, count(json, """"type":"line","layer":"tri""""))
        // 番号サークル: 7 個
        assertEquals(7, count(json, """"type":"circle""""))
        // 番号テキスト: 7 個
        assertEquals(7, count(json, """"type":"text","layer":"num""""))
        // 寸法値: 先頭 3 + 子 6 × 2 (A辺は親との共有辺なので出さない) = 15
        assertEquals(15, count(json, """"type":"text","layer":"dim""""))
        // フラット配列であること
        assertTrue(json.startsWith("[") && json.endsWith("]"))
    }

    @Test
    fun render_emits_dimension_values_and_numbers() {
        val json = WebPrimitiveRenderer.renderCsv(sampleCsv, 1f)
        // formattedString(2): 6.0f は小数 1 桁 + 先頭スペース 1 個 = " 6.0"
        assertTrue(json.contains(""""text":" 6.0""""), "dim text ' 6.0' not found")
        // 番号 1..7
        for (n in 1..7) {
            assertTrue(json.contains(""""text":"$n""""), "number text $n not found")
        }
    }

    @Test
    fun render_places_number_circle_at_pointnumber() {
        val trilist = WebCsvReader.read(sampleCsv)
        val json = WebPrimitiveRenderer.render(trilist, 0.25f)
        // render 内の arrangePointNumbers 後の pointnumber がそのままサークル中心になる
        val pn = trilist.getBy(1).pointnumber
        assertTrue(json.contains(""""cx":${pn.x}"""), "circle cx ${pn.x} not found")
        assertTrue(json.contains(""""cy":${pn.y}"""), "circle cy ${pn.y} not found")
    }

    @Test
    fun render_align_codes_are_dxf_vertical_codes() {
        val json = WebPrimitiveRenderer.renderCsv(sampleCsv, 1f)
        // 寸法は 1 (下) / 3 (上)、番号は 2 (中央) のみ
        assertEquals(0, count(json, """"align":0"""))
        assertEquals(7, count(json, """"align":2"""))
        assertTrue(count(json, """"align":1""") + count(json, """"align":3""") == 15)
    }
}
