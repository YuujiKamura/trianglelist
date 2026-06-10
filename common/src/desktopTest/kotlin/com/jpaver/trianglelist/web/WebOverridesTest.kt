package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Web 段階2e (task #15): WebOverrides (式⊕override 二層の override 適用層) のテスト。
 *
 * 検証は 3 群:
 * 1. parse — mini parser が TS の JSON.stringify 出力 (フラット数値のみ) を読めること
 * 2. apply — dim 値・flag・pointnumber が controlDim* / setPointByUser と同じ規則で立つこと
 * 3. golden 同値 — override 空なら既存出力 (renderCsv/buildDxfText/buildSfcText) と
 *    完全一致、override 有りなら dim 値・旗揚げ線・pointnumber が反映されること
 */
class WebOverridesTest {

    /** WebPrimitiveGoldenTest.sampleCsv と同一 (7 三角形、B/C 接続・再接続が混ざる実戦形) */
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

    // ---- 1. parse ----

    @Test
    fun parse_empty_and_blank_yield_no_overrides() {
        for (json in listOf("", " ", "{}", """{"dims":[],"numbers":[]}""")) {
            val o = WebOverrides.parse(json)
            assertEquals(0, o.dims.size, "dims for [$json]")
            assertEquals(0, o.numbers.size, "numbers for [$json]")
        }
    }

    @Test
    fun parse_reads_dims_and_numbers() {
        val o = WebOverrides.parse(
            """{"dims":[{"tri":1,"side":0,"h":3,"v":1},{"tri":2,"side":1,"v":3}],"numbers":[{"tri":1,"x":1.25,"y":-0.8}]}"""
        )
        assertEquals(2, o.dims.size)
        assertEquals(WebOverrides.DimOverride(1, 0, 3, 1), o.dims[0])
        assertEquals(WebOverrides.DimOverride(2, 1, null, 3), o.dims[1])
        assertEquals(1, o.numbers.size)
        assertEquals(1, o.numbers[0].tri)
        assertEquals(1.25, o.numbers[0].x, 1e-9)
        assertEquals(-0.8, o.numbers[0].y, 1e-9)
    }

    @Test
    fun parse_skips_entries_without_required_keys() {
        val o = WebOverrides.parse(
            """{"dims":[{"tri":1,"side":1},{"side":1,"h":2}],"numbers":[{"tri":1,"x":0.5}]}"""
        )
        assertEquals(0, o.dims.size) // h/v 両方無し・tri 無しは skip
        assertEquals(0, o.numbers.size) // y 無しは skip
    }

    // ---- 2. apply ----

    @Test
    fun apply_dim_horizontal_sets_value_and_user_flag_like_controlDim() {
        val trilist = WebCsvReader.read(sampleCsv)
        WebOverrides.applyJson(trilist, """{"dims":[{"tri":2,"side":1,"h":3}]}""")
        val tri = trilist.getBy(2)
        assertEquals(3, tri.dim.horizontal.b)
        assertTrue(tri.dim.flag[1].isMovedByUser, "controlHorizontal(B) と同じく flag が立つ")
        // side A の horizontal は flag を立てない (Dims.controlHorizontal:114 と同じ)
        WebOverrides.applyJson(trilist, """{"dims":[{"tri":1,"side":0,"h":1}]}""")
        assertEquals(1, trilist.getBy(1).dim.horizontal.a)
        assertEquals(false, trilist.getBy(1).dim.flag[0].isMovedByUser)
    }

    @Test
    fun apply_dim_vertical_sets_value_and_user_flag() {
        val trilist = WebCsvReader.read(sampleCsv)
        val before = trilist.getBy(3).dim.vertical.c
        val flipped = if (before == 1) 3 else 1
        WebOverrides.applyJson(trilist, """{"dims":[{"tri":3,"side":2,"v":$flipped}]}""")
        assertEquals(flipped, trilist.getBy(3).dim.vertical.c)
        assertTrue(trilist.getBy(3).dim.flag[2].isMovedByUser)
    }

    @Test
    fun apply_invalid_values_and_out_of_range_tri_are_skipped() {
        val trilist = WebCsvReader.read(sampleCsv)
        val h0 = trilist.getBy(1).dim.horizontal.b
        val v0 = trilist.getBy(1).dim.vertical.b
        // tri 99 は存在しない / h=9 は範囲外 / v=2 は OUTER(1)/INNER(3) 以外 — 全部 no-op
        WebOverrides.applyJson(
            trilist,
            """{"dims":[{"tri":99,"side":1,"h":1},{"tri":1,"side":1,"h":9},{"tri":1,"side":1,"v":2}]}"""
        )
        assertEquals(h0, trilist.getBy(1).dim.horizontal.b)
        assertEquals(v0, trilist.getBy(1).dim.vertical.b)
    }

    @Test
    fun apply_number_moves_and_survives_arrangePointNumbers() {
        val trilist = WebCsvReader.read(sampleCsv)
        // pointcenter 近傍 (BORDER=20*scale 内) の点へ移動
        val c = trilist.getBy(1).pointcenter
        val target = PointXY(c.x + 0.5, c.y + 0.3)
        WebOverrides.applyJson(trilist, """{"numbers":[{"tri":1,"x":${target.x},"y":${target.y}}]}""")
        assertEquals(target.x, trilist.getBy(1).pointnumber.x, 1e-9)
        assertEquals(target.y, trilist.getBy(1).pointnumber.y, 1e-9)
        // render が呼ぶ autoAlign (PointNumberManager.kt:36) から保護される
        trilist.arrangePointNumbers()
        assertEquals(target.x, trilist.getBy(1).pointnumber.x, 1e-9)
        assertEquals(target.y, trilist.getBy(1).pointnumber.y, 1e-9)
    }

    @Test
    fun apply_number_far_from_center_is_ignored_like_app_border() {
        val trilist = WebCsvReader.read(sampleCsv)
        val before = trilist.getBy(1).pointnumber
        // BORDER = 20f * scaleFactor (PointNumberManager.kt:19) を大きく超える点
        WebOverrides.applyJson(trilist, """{"numbers":[{"tri":1,"x":1000,"y":1000}]}""")
        assertEquals(before.x, trilist.getBy(1).pointnumber.x, 1e-9)
        assertEquals(before.y, trilist.getBy(1).pointnumber.y, 1e-9)
    }

    // ---- 3. golden 同値 (override 空 = 既存出力と完全一致 / 有り = 反映) ----

    @Test
    fun empty_overrides_keep_primitives_identical() {
        val base = WebPrimitiveRenderer.renderCsv(sampleCsv, 1f)
        assertEquals(base, WebPrimitiveRenderer.renderCsv(sampleCsv, 1f, ""))
        assertEquals(base, WebPrimitiveRenderer.renderCsv(sampleCsv, 1f, "{}"))
    }

    @Test
    fun empty_overrides_keep_dxf_and_sfc_identical() {
        assertEquals(WebDrawingExport.buildDxfText(sampleCsv), WebDrawingExport.buildDxfText(sampleCsv, "{}"))
        assertEquals(
            WebDrawingExport.buildSfcText(sampleCsv, "t.sfc"),
            WebDrawingExport.buildSfcText(sampleCsv, "t.sfc", "{}")
        )
    }

    @Test
    fun horizontal_over_2_emits_flag_line_and_h_field() {
        val base = WebPrimitiveRenderer.renderCsv(sampleCsv, 1f)
        val flipped = WebPrimitiveRenderer.renderCsv(sampleCsv, 1f, """{"dims":[{"tri":2,"side":1,"h":3}]}""")
        assertNotEquals(base, flipped)
        assertTrue(flipped.contains(""""tri":2,"side":1,"h":3"""), "dim テキストに到達値 h=3 が出る")
        // 旗揚げ線 (WebPrimitiveRenderer: horizontal>2 で dim layer の line) が 1 本増える
        fun dimLines(json: String) = Regex(""""type":"line","layer":"dim"""").findAll(json).count()
        assertEquals(dimLines(base) + 1, dimLines(flipped))
    }

    @Test
    fun vertical_flip_changes_v_field_and_output() {
        val base = WebPrimitiveRenderer.renderCsv(sampleCsv, 1f)
        // 実効値を JSON から読む (TS と同じやり方) — tri2/side1 の v
        val m = Regex(""""tri":2,"side":1,"h":(\d+),"v":(\d+)""").find(base)
        assertTrue(m != null, "tri2/side1 の dim テキストが存在する")
        val v = m!!.groupValues[2].toInt()
        val flipped = if (v == 1) 3 else 1
        val out = WebPrimitiveRenderer.renderCsv(sampleCsv, 1f, """{"dims":[{"tri":2,"side":1,"v":$flipped}]}""")
        assertNotEquals(base, out)
        assertTrue(out.contains(""""tri":2,"side":1,"h":0,"v":$flipped"""))
    }

    @Test
    fun number_override_moves_circle_in_primitives() {
        // 期待座標は同じ override を model に直接適用して導出 (文字列化の桁差を避ける)
        val trilist = WebCsvReader.read(sampleCsv)
        val c = trilist.getBy(1).pointcenter
        val ov = """{"numbers":[{"tri":1,"x":${c.x + 0.5},"y":${c.y + 0.3}}]}"""
        WebOverrides.applyJson(trilist, ov)
        val pn = trilist.getBy(1).pointnumber
        val out = WebPrimitiveRenderer.renderCsv(sampleCsv, 1f, ov)
        assertTrue(out.contains(""""cx":${pn.x},"cy":${pn.y}"""), "番号サークルが指定座標へ動く")
        assertNotEquals(WebPrimitiveRenderer.renderCsv(sampleCsv, 1f), out)
    }

    @Test
    fun dim_override_lands_in_dxf_output() {
        val base = WebDrawingExport.buildDxfText(sampleCsv)
        val out = WebDrawingExport.buildDxfText(sampleCsv, """{"dims":[{"tri":2,"side":1,"h":3}]}""")
        assertNotEquals(base, out, "W フリップ (旗揚げ) が DXF にも乗る")
    }
}
