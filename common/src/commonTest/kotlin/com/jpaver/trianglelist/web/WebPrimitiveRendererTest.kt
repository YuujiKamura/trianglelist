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
    fun reader_restores_manual_placement_from_full_format_columns() {
        // 完全形式の手動配置列 (CsvLoader.finalizeBuildTriangle と同順):
        // 列7-9 = 番号サークル位置 (移動フラグ true 時のみ)、列11-16 = 寸法アライメント、
        // 列20-21 = 寸法手動フラグ。子を持たない行なら保存値がそのまま残る
        val single = "1,6.0,5.0,4.0,-1,-1,,4.0,-2.0,true,4,2,0,0,3,1,1\n"
        val t1 = WebCsvReader.read(single).getBy(1)
        // 番号サークル: 保存座標は絶対値、移動フラグが recoverState の回転から外すので据え置き
        assertEquals(4.0, t1.pointnumber.x.toDouble(), 0.001)
        assertEquals(-2.0, t1.pointnumber.y.toDouble(), 0.001)
        assertTrue(t1.pointNumber.flag.isMovedByUser)
        // 寸法アライメント: horizontal.a=2、vertical.a=3 が保存値どおり
        assertEquals(2, t1.dim.horizontal.a)
        assertEquals(3, t1.dim.vertical.a)
    }

    @Test
    fun reader_saved_aligns_win_over_auto_inner_for_doubled_child() {
        // 手動優先の順序: 二重断面の子 (conn 5) の A辺は add() の自動配置で内側(3) になるが、
        // 保存値 (列14 = 1 = 外側) が「後」に適用されて勝つ — CsvLoader と同じ順序
        val csv =
            "1,6.0,5.0,4.0,-1,-1\n" +
                "2,5.0,4.0,3.0,1,5,,0,0,false,4,0,0,0,1,1,1,2,1,2,true,false\n"
        val trilist = WebCsvReader.read(csv)
        val t2 = trilist.getBy(2)
        assertEquals(1, t2.dim.vertical.a)
        assertTrue(t2.dim.flag[1].isMovedByUser)
        assertTrue(!t2.dim.flag[2].isMovedByUser)
        // 親 C辺: 二重断面の子が付いたら親側の寸法も親の内側(3) へ — 重なる 2 寸法の帰属を
        // 分けるため (2026-06-11 user バグ判定で修正)。ロード経路でも効くことを pin
        // (旧バグ: connectionSide コードが add() 後にしか入らず、ロード時だけ外れていた)
        assertEquals(3, trilist.getBy(1).dim.vertical.c)
        // 描画にも乗る: tri2 side0 の寸法テキストが v=1 で出る
        val json = WebPrimitiveRenderer.render(trilist, 0.25f)
        assertTrue(json.contains(""""tri":2,"side":0,"h":0,"v":1"""), "saved v=1 not in render output")
    }

    @Test
    fun parent_manual_flag_beats_auto_inner_on_connected_edge() {
        // 親 C辺に手動フラグ (列21=true) + 保存値 1 (外側) があるとき、後続の二重断面の子の
        // add() が自動内側化で潰さない — 手動配置優先のガード
        val csv =
            "1,6.0,5.0,4.0,-1,-1,,0,0,false,4,0,0,0,1,1,1,0,0,2,false,true\n" +
                "2,5.0,4.0,3.0,1,5,,0,0,false,4,0,0,0,1,1,1,2,1,2,false,false\n"
        val trilist = WebCsvReader.read(csv)
        assertEquals(1, trilist.getBy(1).dim.vertical.c)
        assertTrue(trilist.getBy(1).dim.flag[2].isMovedByUser)
    }

    @Test
    fun render_emits_expected_primitive_counts() {
        val json = WebPrimitiveRenderer.renderCsv(sampleCsv, 1f)
        // 塗り: 7 三角形 × 1 (MyView.drawEntities:572-576 の「全三角形を塗る」の web 版)
        assertEquals(7, count(json, """"type":"fill","layer":"fill""""))
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
    fun fill_color_index_follows_csv_column10() {
        // 色の経路 pin: CSV 列 10 → CsvCodec.applyRowMeta:206 setColor → Triangle.mycolor →
        // fill prim の color。アプリの FAB (MainActivity.kt:1375 mycolor = colorindex) と同じ写像
        val csv = "1,3.0,3.0,3.0,-1,-1,,0,0,false,2\n"
        val json = WebPrimitiveRenderer.renderCsv(csv, 1f)
        assertTrue(json.contains(""""type":"fill","layer":"fill""""), "fill prim not found")
        assertTrue(json.contains(""""color":2"""), "color index 2 (CSV col10) not in fill prim")
        // 列 10 が無い最小形式は Triangle.mycolor 既定値 4 (Triangle.kt:147、アプリ colorindex 初期値と同じ)
        val jsonDefault = WebPrimitiveRenderer.renderCsv("1,3.0,3.0,3.0,-1,-1\n", 1f)
        assertTrue(jsonDefault.contains(""""color":4"""), "default color index 4 not in fill prim")
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
