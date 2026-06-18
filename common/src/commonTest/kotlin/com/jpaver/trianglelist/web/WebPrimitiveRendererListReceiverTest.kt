package com.jpaver.trianglelist.web

import com.jpaver.trianglelist.editmodel.EditList
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SoT 一本化 段2 受け口 pin (2026-06-15): WebPrimitiveRenderer.render(list: EditList<CycleShape>)
 * の最小スケルトン (塗り + 辺 + 番号サークル) が形状種別 (Triangle / Rectangle) に対して
 * 多態的に正しく prim を吐くことを pin する。
 *
 * 段階1 のスコープ: 塗り + 辺 (getLine) + 番号サークル + 番号テキストのみ。
 * 段階2 で寸法 (DimensionLayout) と Deduction overlay を多態で追加する。
 */
class WebPrimitiveRendererListReceiverTest {

    private val ts = 0.25f

    @Test
    fun empty_list_returns_empty_array() {
        val list = EditList<CycleShape>()
        val json = WebPrimitiveRenderer.render(list, ts)
        assertEquals("[]", json, "空 list は [] を返す (golden)")
    }

    @Test
    fun single_triangle_outputs_fill_three_lines_circle_text() {
        val list = EditList<CycleShape>()
        list.add(Triangle(6f, 5f, 4f))
        val json = WebPrimitiveRenderer.render(list, ts)
        // 期待: fill 1 + line 3 + circle 1 + text 1 = 6 prim
        val fillCount = """"type":"fill"""".toRegex().findAll(json).count()
        val triLineCount = """"type":"line","layer":"tri"""".toRegex().findAll(json).count()
        val numCircleCount = """"type":"circle","layer":"num"""".toRegex().findAll(json).count()
        val numTextCount = """"type":"text","layer":"num"""".toRegex().findAll(json).count()
        assertEquals(1, fillCount, "Triangle 1個 → fill 1")
        assertEquals(3, triLineCount, "Triangle 1個 → tri line 3")
        assertEquals(1, numCircleCount, "Triangle 1個 → num circle 1")
        assertEquals(1, numTextCount, "Triangle 1個 → num text 1")
    }

    @Test
    fun single_rectangle_outputs_four_lines_no_fill() {
        val list = EditList<CycleShape>()
        list.add(Rectangle(5.0, 6.0, 4.0))
        val json = WebPrimitiveRenderer.render(list, ts)
        val fillCount = """"type":"fill"""".toRegex().findAll(json).count()
        val triLineCount = """"type":"line","layer":"tri"""".toRegex().findAll(json).count()
        val numCircleCount = """"type":"circle","layer":"num"""".toRegex().findAll(json).count()
        val numTextCount = """"type":"text","layer":"num"""".toRegex().findAll(json).count()
        assertEquals(2, fillCount, "Rectangle は bl-tr 対角線で 2 三角形に分割塗り (717c318)")
        assertEquals(4, triLineCount, "Rectangle 1個 → tri line 4 (4辺)")
        assertEquals(1, numCircleCount, "Rectangle 1個 → num circle 1")
        assertEquals(1, numTextCount, "Rectangle 1個 → num text 1")
    }

    @Test
    fun mixed_list_outputs_in_order() {
        val list = EditList<CycleShape>()
        list.add(Triangle(6f, 5f, 4f))
        list.add(Rectangle(5.0, 6.0, 4.0))
        val json = WebPrimitiveRenderer.render(list, ts)
        // 期待: 三角形塗り 1 + 台形塗り 2 (bl-tr 対角線分割) + (三角形辺3 + 三角形円 + 三角形テキスト) + (台形辺4 + 台形円 + 台形テキスト) = 14
        val fillCount = """"type":"fill"""".toRegex().findAll(json).count()
        val triLineCount = """"type":"line","layer":"tri"""".toRegex().findAll(json).count()
        val numCircleCount = """"type":"circle","layer":"num"""".toRegex().findAll(json).count()
        val numTextCount = """"type":"text","layer":"num"""".toRegex().findAll(json).count()
        assertEquals(3, fillCount, "三角形塗り 1 + 台形 2 三角形分割塗り (717c318)")
        assertEquals(7, triLineCount, "三角形3辺 + 台形4辺")
        assertEquals(2, numCircleCount, "番号サークル 2 (図形ごと 1)")
        assertEquals(2, numTextCount, "番号テキスト 2 (図形ごと 1)")
    }

    @Test
    fun side_index_is_polymorphic_by_sideCount() {
        // Triangle.sideCount=3 → side=0,1,2 が出る
        // Rectangle.sideCount=4 → side=0,1,2,3 が出る
        val list = EditList<CycleShape>()
        list.add(Triangle(6f, 5f, 4f))
        list.add(Rectangle(5.0, 6.0, 4.0))
        val json = WebPrimitiveRenderer.render(list, ts)
        // Triangle (tri:1) は side 0/1/2、 Rectangle (tri:2) は side 0/1/2/3
        assertTrue(json.contains(""""tri":1,"side":0"""), "Triangle side 0")
        assertTrue(json.contains(""""tri":1,"side":1"""), "Triangle side 1")
        assertTrue(json.contains(""""tri":1,"side":2"""), "Triangle side 2")
        assertTrue(!json.contains(""""tri":1,"side":3"""), "Triangle に side 3 は無い")
        assertTrue(json.contains(""""tri":2,"side":0"""), "Rectangle side 0")
        assertTrue(json.contains(""""tri":2,"side":1"""), "Rectangle side 1")
        assertTrue(json.contains(""""tri":2,"side":2"""), "Rectangle side 2")
        assertTrue(json.contains(""""tri":2,"side":3"""), "Rectangle side 3")
    }

    @Test
    fun triangle_emits_three_dim_specs() {
        val list = EditList<CycleShape>()
        list.add(Triangle(6f, 5f, 4f))
        val json = WebPrimitiveRenderer.render(list, ts)
        // Triangle (独立、connectionSide=-1) → A/B/C 3 辺の寸法が出る
        val dimTextCount = """"type":"text","layer":"dim"""".toRegex().findAll(json).count()
        assertEquals(3, dimTextCount, "独立 Triangle → 寸法 3 (A/B/C)")
    }

    @Test
    fun rectangle_emits_three_dim_specs_when_independent() {
        val list = EditList<CycleShape>()
        list.add(Rectangle(5.0, 6.0, 4.0))
        val json = WebPrimitiveRenderer.render(list, ts)
        // 独立 Rectangle (nodeA=null) → A底辺 / B垂線延長 / C上辺 の 3 寸法。
        // 斜辺 (D 右脚 / 中央揃え時の左脚) は出さない (2026-06-17 yuuji 指示)。
        val dimTextCount = """"type":"text","layer":"dim"""".toRegex().findAll(json).count()
        assertEquals(3, dimTextCount, "独立 Rectangle → 寸法 3 (A/B/C、斜辺なし)")
    }

    @Test
    fun number_text_uses_1based_index_from_forEachItemIndexed() {
        val list = EditList<CycleShape>()
        list.add(Triangle(6f, 5f, 4f))
        list.add(Triangle(5f, 4f, 3f))
        list.add(Rectangle(5.0, 6.0, 4.0))
        val json = WebPrimitiveRenderer.render(list, ts)
        // 番号テキストは 1始まり ("1", "2", "3")
        assertTrue(json.contains(""""text":"1""""), "番号 1")
        assertTrue(json.contains(""""text":"2""""), "番号 2")
        assertTrue(json.contains(""""text":"3""""), "番号 3")
    }
}
