package com.jpaver.trianglelist.datamanager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CsvCodec の Rectangle (台形) 混在 round-trip pin。
 * CsvCodecTest.kt (既存) は controls/deduction のみ。
 * ここでは Rectangle 混在 CSV の全フィールド保持を検証する。
 *
 * user 確定 2026-06-16「TriTrap タグ排除」── CSV タグは Rectangle 1 種、 Rectangle 子三角形は
 * 普通三角形行で parent=混在通し番号 (= 三角形数 + Rectangle idx) で表現する。
 *
 * figureRows フィルタリングで種別カウントを行う:
 *   - Rectangle 行:    chunks[0] == "Rectangle"
 *   - 普通三角形行:    parent <= 直前までの三角形数 (or parent < 1)
 *   - Rectangle 子行: parent > 直前までの三角形数 (= 旧 TriTrap 相当)
 */
class CsvCodecTrapRoundTripTest {

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

    /** fixture の parse で figureRows に正しく振り分けられる */
    @Test
    fun fixture_parse_distributes_rect_and_rect_child_rows() {
        val doc = CsvCodec.parse(RECT_MIXED_CSV)
        assertEquals(4, doc.preLines.size, "preLines 4 行")
        assertEquals(3, doc.triRows().size, "普通三角形 3 行")
        assertEquals(2, doc.rectRows().size, "Rectangle 2 行")
        assertEquals(1, doc.rectChildTriRows().size, "Rectangle 子三角形 1 行")
        assertEquals(1, doc.dedRows.size, "控除 1 行")
        assertEquals(15.0f, doc.listAngle!!, 0.001f)
        assertEquals(5.0f, doc.textSize!!, 0.001f)
        // ListScale は named field に昇格済み → postLines には残らない
        assertEquals(0, doc.postLines.size, "postLines 0 行 (ListScale は named field に昇格)")
        assertEquals(1.0f, doc.listScale!!, 0.001f)
    }

    /** parse -> serialize -> parse が固定点 (idempotence) */
    @Test
    fun rect_csv_parse_serialize_idempotence() {
        val doc1 = CsvCodec.parse(RECT_MIXED_CSV)
        val once = CsvCodec.serialize(doc1)
        val twice = CsvCodec.serialize(CsvCodec.parse(once))
        assertEquals(once, twice, "serialize -> parse -> serialize が固定点")
    }

    /** parse -> build -> bake -> serialize -> parse で CsvDoc フィールドが等価 */
    @Test
    fun rect_csv_full_roundtrip_preserves_rect_rows() {
        val doc1 = CsvCodec.parse(RECT_MIXED_CSV)
        val trilist = CsvCodec.build(doc1)
        val baked = CsvCodec.bake(trilist, doc1)
        val serialized = CsvCodec.serialize(baked)
        val doc2 = CsvCodec.parse(serialized)

        assertEquals(doc1.rectRows().size, doc2.rectRows().size, "Rectangle 行数")
        assertEquals(doc1.rectChildTriRows().size, doc2.rectChildTriRows().size, "Rectangle 子三角形 行数")
        assertEquals(doc1.preLines, doc2.preLines, "preLines 保持")
        assertEquals(doc1.dedRows.size, doc2.dedRows.size, "dedRows 行数")
        assertEquals(doc1.textSize, doc2.textSize, "textSize 保持")
        assertEquals(doc1.listAngle, doc2.listAngle, "listAngle 保持")
    }

    /** Rectangle 子三角形の round-trip で辺長・接続情報が保持される */
    @Test
    fun rect_child_chain_roundtrip_preserves_edge_and_connection() {
        val doc1 = CsvCodec.parse(RECT_MIXED_CSV)
        val trilist = CsvCodec.build(doc1)
        val baked = CsvCodec.bake(trilist, doc1)
        val serialized = CsvCodec.serialize(baked)
        val doc2 = CsvCodec.parse(serialized)

        val tt1 = doc1.rectChildTriRows()
        val tt2 = doc2.rectChildTriRows()
        assertEquals(tt1.size, tt2.size)
        for (i in tt1.indices) {
            val r1 = tt1[i]
            val r2 = tt2[i]
            // chunks: [num, a, b, c, parent, side]
            assertEquals(r1.chunks.getOrNull(0), r2.chunks.getOrNull(0), "rectChild[$i] num")
            assertEquals(r1.chunks.getOrNull(1), r2.chunks.getOrNull(1), "rectChild[$i] a")
            assertEquals(r1.chunks.getOrNull(2), r2.chunks.getOrNull(2), "rectChild[$i] b")
            assertEquals(r1.chunks.getOrNull(3), r2.chunks.getOrNull(3), "rectChild[$i] c")
            assertEquals(r1.chunks.getOrNull(4), r2.chunks.getOrNull(4), "rectChild[$i] parent")
            assertEquals(r1.chunks.getOrNull(5), r2.chunks.getOrNull(5), "rectChild[$i] side")
        }
    }

    /** 旧 CSV (Rectangle なし、28 列形式) の round-trip 等価 -- 回帰防止 */
    @Test
    fun legacy_csv_roundtrip_no_regression() {
        val doc1 = CsvCodec.parse(LEGACY_CSV)
        assertTrue(doc1.rectRows().isEmpty(), "旧 CSV に Rectangle 行はない")
        assertTrue(doc1.rectChildTriRows().isEmpty(), "旧 CSV に Rectangle 子三角形はない")

        val trilist = CsvCodec.build(doc1)
        val baked = CsvCodec.bake(trilist, doc1)
        val serialized = CsvCodec.serialize(baked)
        val doc2 = CsvCodec.parse(serialized)

        assertEquals(doc1.triRows().size, doc2.triRows().size, "旧 CSV 三角形行数保持")
        assertEquals(doc1.preLines, doc2.preLines, "旧 CSV preLines 保持")
        assertEquals(doc1.dedRows.size, doc2.dedRows.size, "旧 CSV dedRows 保持")
        assertEquals(doc1.textSize, doc2.textSize, "旧 CSV textSize 保持")
        assertEquals(doc1.listAngle, doc2.listAngle, "旧 CSV listAngle 保持")
    }

    /** Rectangle の色 (mycolor)・アライメント・各種プロパティの round-trip 等価検証 */
    @Test
    fun rect_color_and_properties_roundtrip() {
        val csv = "koujiname, Rectangle色テスト\n" +
                  "rosenname, テスト路線\n" +
                  "gyousyaname, テスト業者\n" +
                  "zumennum, 1\n" +
                  "Rectangle, 1, 3.00, 2.00, 4.00, -1, 0, 1, 0, 0, RecStation, , , , 3, , , , , , , , , , , , 2, false\n" + // color=3, align=1 (中央), dimHorizontal.s = 2
                  "ListAngle, 0.0\n" +
                  "ListScale, 1.0\n" +
                  "TextSize, 5.0\n"
        val doc1 = CsvCodec.parse(csv)
        val rectRows1 = doc1.rectRows()
        assertEquals(1, rectRows1.size)
        assertEquals("3", rectRows1[0].chunks.getOrNull(14), "parse 時に 15列目の色が 3 であること")
        assertEquals("1", rectRows1[0].chunks.getOrNull(7), "parse 時に 8列目の align が 1 であること")
        assertEquals("2", rectRows1[0].chunks.getOrNull(26), "parse 時に 27列目の dimHorizontal.s が 2 であること")

        val trilist = CsvCodec.build(doc1)
        val baked = CsvCodec.bake(trilist, doc1)
        val serialized = CsvCodec.serialize(baked)
        val doc2 = CsvCodec.parse(serialized)

        val rectRows2 = doc2.rectRows()
        assertEquals(1, rectRows2.size)
        assertEquals("3", rectRows2[0].chunks.getOrNull(14), "round-trip 後に 15列目の色が 3 であること")
        assertEquals("1", rectRows2[0].chunks.getOrNull(7), "round-trip 後に 8列目の align が 1 であること")
        assertEquals("2", rectRows2[0].chunks.getOrNull(26), "round-trip 後に 27列目の dimHorizontal.s が 2 であること")
    }


    /** dedRows の round-trip 等価 (viewscale=1) */
    @Test
    fun dedRows_roundtrip_viewscale1() {
        val doc1 = CsvCodec.parse(RECT_MIXED_CSV)
        val trilist = CsvCodec.build(doc1)
        val baked = CsvCodec.bake(trilist, doc1)
        val serialized = CsvCodec.serialize(baked)
        val doc2 = CsvCodec.parse(serialized)

        assertEquals(doc1.dedRows.size, doc2.dedRows.size, "dedRows 行数")
        for (i in doc1.dedRows.indices) {
            assertEquals(doc1.dedRows[i].chunks, doc2.dedRows[i].chunks, "dedRows[$i] 全列等価")
        }
    }

    // -- inline fixtures --

    companion object {
        /** Rectangle + Rectangle 子三角形 + 控除 入り CSV (Web bake 相当) */
        val RECT_MIXED_CSV =
            "koujiname, Rectangle 混在テスト\n" +
            "rosenname, テスト路線\n" +
            "gyousyaname, テスト業者\n" +
            "zumennum, 1\n" +
            "1, 6.0, 5.0, 4.0, -1, -1\n" +
            "2, 5.0, 4.0, 3.0, 1, 1\n" +
            "3, 4.0, 3.5, 3.0, 1, 2\n" +
            "ListAngle, 15.0\n" +
            "ListScale, 1.0\n" +
            "TextSize, 5.0\n" +
            "Deduction,1,水路,0.5,4.0,1,1,0.0,1.0,2.0,1.5,2.5,0.0\n" +
            "Rectangle, 3.0, 2.0, 4.0, -1, 0.0, 0.0, 0.0, true, 4\n" +
            "Rectangle, 2.5, 1.5, 3.5, 1, 0.0, 0.0, 0.0, false, 4\n" +
            "4, 3.0, 2.5, 2.0, 4, 1\n"  // parent=4 > triRows.size=3 -> Rectangle 子三角形

        /** 旧 app 由来 CSV (Rectangle なし、28 列形式) -- 回帰防止用 */
        val LEGACY_CSV =
            "koujiname, 工事A\n" +
            "rosenname, 路線B\n" +
            "gyousyaname, 業者C\n" +
            "zumennum, 1/1\n" +
            "1,6.0,5.0,4.0,-1,-1,No.1,4.0,-2.0,true,4,2,0,0,3,1,1,0,0,2,false,true,0.0,0.0,0.0,-180.0,0,false\n" +
            "2,5.0,4.0,3.0,1,5,,0.0,0.0,false,4,0,0,0,1,1,1,2,1,2,true,false,0.0,0.0,0.0,0.0,0,false\n" +
            "ListAngle, 15.0\n" +
            "ListScale, 1.0\n" +
            "TextSize, 5.0\n" +
            "Deduction,1,水路,0.5,4.0,1,1,0.0,1.0,2.0,1.5,2.5,0.0\n"
    }
}
