package com.jpaver.trianglelist.datamanager

import com.jpaver.trianglelist.editmodel.EditObject
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SoT 一本化 (2026-06-15): CsvCodec.buildAll が figureRows の出現順を保ったまま
 * 三角形 / Rectangle (台形) / Rectangle 子三角形 を 1 本の EditList<EditObject> に積むことを pin する。
 * user 確定 2026-06-16「TriTrap タグ排除」── CSV タグは Rectangle 1 種、台形子三角形は普通三角形行で
 * parent=混在通し番号 (= trilist.size() + Rectangle idx) で表現する。
 */
class CsvCodecBuildAllTest {

    private fun parse(csv: String) = CsvCodec.parse(csv + "\n")

    @Test
    fun empty_doc_returns_empty_list() {
        val doc = parse("")
        val mixed = CsvCodec.buildAll(doc)
        assertEquals(0, mixed.size(), "空 figureRows → 空 EditList")
        assertTrue(mixed.isEmpty(), "isEmpty() == true")
    }

    @Test
    fun triangles_only_preserve_count() {
        val doc = parse("1,6.0,5.0,4.0,-1,-1\n2,5.0,4.0,3.0,1,1")
        val mixed = CsvCodec.buildAll(doc)
        assertEquals(2, mixed.size())
        assertTrue(mixed.get(1) is Triangle, "1番目は Triangle")
        assertTrue(mixed.get(2) is Triangle, "2番目は Triangle")
    }

    @Test
    fun rectangle_only_appears_in_mixed_list() {
        val doc = parse("Rectangle,1,5,4,3,-1,0")
        val mixed = CsvCodec.buildAll(doc)
        assertEquals(1, mixed.size(), "Rectangle 1 行 → mixed.size==1")
        assertTrue(mixed.get(1) is Rectangle, "1番目は Rectangle")
    }

    @Test
    fun triangle_then_rectangle_keeps_csv_order() {
        val doc = parse("1,6.0,5.0,4.0,-1,-1\nRectangle,2,5,4,3,1,1")
        val mixed = CsvCodec.buildAll(doc)
        assertEquals(2, mixed.size())
        assertTrue(mixed.get(1) is Triangle, "CSV 1 行目 = Triangle")
        assertTrue(mixed.get(2) is Rectangle, "CSV 2 行目 = Rectangle")
    }

    @Test
    fun rectangle_then_child_triangle_keeps_csv_order() {
        // Rectangle 1 個の後に「parent=1 (= trilist.size()=0 + Rectangle idx 1)」 の Triangle 行
        val doc = parse("Rectangle,1,5,10,7,-1,0\n2,7,4,4,1,2")
        val mixed = CsvCodec.buildAll(doc)
        assertEquals(2, mixed.size())
        assertTrue(mixed.get(1) is Rectangle, "CSV 1 行目 = Rectangle")
        assertTrue(mixed.get(2) is Triangle, "CSV 2 行目 = Rectangle 子 Triangle")
    }

    @Test
    fun triangle_rectangle_rect_child_all_mixed_in_order() {
        // 三角形 → Rectangle → Rectangle 子三角形 の出現順を CSV から取り出す
        // parent=2 (trilist.size()=1 + Rectangle idx 1 = 混在通し 2)
        val doc = parse("1,6.0,5.0,4.0,-1,-1\nRectangle,2,5,10,7,1,1\n3,7,4,4,2,2")
        val mixed = CsvCodec.buildAll(doc)
        assertEquals(3, mixed.size(), "三角 + Rectangle + Rectangle 子 = 3")
        assertTrue(mixed.get(1) is Triangle,  "1 = Triangle")
        assertTrue(mixed.get(2) is Rectangle, "2 = Rectangle")
        assertTrue(mixed.get(3) is Triangle,  "3 = Rectangle 子 Triangle")
    }

    @Test
    fun for_each_item_visits_in_one_indexed_order() {
        val doc = parse("1,6.0,5.0,4.0,-1,-1\nRectangle,2,5,4,3,1,1")
        val mixed = CsvCodec.buildAll(doc)
        val kinds = mutableListOf<Pair<Int, String>>()
        mixed.forEachItemIndexed { i, e ->
            kinds.add(i to e::class.simpleName.orEmpty())
        }
        assertEquals(listOf(1 to "Triangle", 2 to "Rectangle"), kinds)
    }

    @Test
    fun index_of_finds_one_based_number() {
        val doc = parse("1,6.0,5.0,4.0,-1,-1\nRectangle,2,5,4,3,1,1")
        val mixed = CsvCodec.buildAll(doc)
        val first: EditObject? = mixed.firstItem()
        val last: EditObject? = mixed.lastItem()
        assertEquals(1, mixed.indexOfItem(first), "firstItem の番号 = 1")
        assertEquals(2, mixed.indexOfItem(last),  "lastItem の番号 = 2")
        assertEquals(-1, mixed.indexOfItem(null), "null は -1")
    }
}
