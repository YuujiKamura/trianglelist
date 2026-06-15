package com.jpaver.trianglelist.datamanager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CsvCodec の台形/TriTrap round-trip pin (B10)。
 * CsvCodecTest.kt (既存) は controls/deduction のみ。
 * ここでは台形混在 CSV の全フィールド保持を検証する。
 */
class CsvCodecTrapRoundTripTest {

    /** fixture の parse で CsvDoc の各バケツに正しく振り分けられる */
    @Test
    fun fixture_parse_distributes_trapRows_and_trapParentedTriRows() {
        val doc = CsvCodec.parse(TRAP_MIXED_CSV)
        assertEquals(4, doc.preLines.size, "preLines 4 行")
        assertEquals(3, doc.rows.size, "普通三角形 3 行")
        assertEquals(2, doc.trapRows.size, "台形 2 行")
        assertEquals(1, doc.trapParentedTriRows.size, "TriTrap 1 行")
        assertEquals(1, doc.dedRows.size, "控除 1 行")
        assertEquals(15.0f, doc.listAngle!!, 0.001f)
        assertEquals(5.0f, doc.textSize!!, 0.001f)
        // ListScale は named field に昇格済み → postLines には残らない
        assertEquals(0, doc.postLines.size, "postLines 0 行 (ListScale は named field に昇格)")
        assertEquals(1.0f, doc.listScale!!, 0.001f)
    }

    /** parse -> serialize -> parse が固定点 (idempotence) */
    @Test
    fun trap_csv_parse_serialize_idempotence() {
        val doc1 = CsvCodec.parse(TRAP_MIXED_CSV)
        val once = CsvCodec.serialize(doc1)
        val twice = CsvCodec.serialize(CsvCodec.parse(once))
        assertEquals(once, twice, "serialize -> parse -> serialize が固定点")
    }

    /** parse -> build -> bake -> serialize -> parse で CsvDoc フィールドが等価 */
    @Test
    fun trap_csv_full_roundtrip_preserves_trapRows() {
        val doc1 = CsvCodec.parse(TRAP_MIXED_CSV)
        val trilist = CsvCodec.build(doc1)
        val baked = CsvCodec.bake(trilist, doc1)
        val serialized = CsvCodec.serialize(baked)
        val doc2 = CsvCodec.parse(serialized)

        assertEquals(doc1.trapRows.size, doc2.trapRows.size, "trapRows 行数")
        assertEquals(doc1.trapParentedTriRows.size, doc2.trapParentedTriRows.size, "trapParentedTriRows 行数")
        assertEquals(doc1.preLines, doc2.preLines, "preLines 保持")
        assertEquals(doc1.dedRows.size, doc2.dedRows.size, "dedRows 行数")
        assertEquals(doc1.textSize, doc2.textSize, "textSize 保持")
        assertEquals(doc1.listAngle, doc2.listAngle, "listAngle 保持")
    }

    /** TriTrap chain: 台形子三角形の round-trip で辺長・接続情報が保持される */
    @Test
    fun tritrap_chain_roundtrip_preserves_edge_and_connection() {
        val doc1 = CsvCodec.parse(TRAP_MIXED_CSV)
        val trilist = CsvCodec.build(doc1)
        val baked = CsvCodec.bake(trilist, doc1)
        val serialized = CsvCodec.serialize(baked)
        val doc2 = CsvCodec.parse(serialized)

        assertEquals(doc1.trapParentedTriRows.size, doc2.trapParentedTriRows.size)
        for (i in doc1.trapParentedTriRows.indices) {
            val r1 = doc1.trapParentedTriRows[i]
            val r2 = doc2.trapParentedTriRows[i]
            // chunks: [TriTrap, num, ea, B, C, targetIdx, side]
            assertEquals(r1.chunks.getOrNull(1), r2.chunks.getOrNull(1), "TriTrap[$i] num")
            assertEquals(r1.chunks.getOrNull(3), r2.chunks.getOrNull(3), "TriTrap[$i] B")
            assertEquals(r1.chunks.getOrNull(4), r2.chunks.getOrNull(4), "TriTrap[$i] C")
            assertEquals(r1.chunks.getOrNull(5), r2.chunks.getOrNull(5), "TriTrap[$i] targetIdx")
            assertEquals(r1.chunks.getOrNull(6), r2.chunks.getOrNull(6), "TriTrap[$i] side")
        }
    }

    /** 旧 CSV (台形なし、28 列形式) の round-trip 等価 -- 回帰防止 */
    @Test
    fun legacy_csv_roundtrip_no_regression() {
        val doc1 = CsvCodec.parse(LEGACY_CSV)
        assertTrue(doc1.trapRows.isEmpty(), "旧 CSV に台形行はない")
        assertTrue(doc1.trapParentedTriRows.isEmpty(), "旧 CSV に TriTrap はない")

        val trilist = CsvCodec.build(doc1)
        val baked = CsvCodec.bake(trilist, doc1)
        val serialized = CsvCodec.serialize(baked)
        val doc2 = CsvCodec.parse(serialized)

        assertEquals(doc1.rows.size, doc2.rows.size, "旧 CSV 三角形行数保持")
        assertEquals(doc1.preLines, doc2.preLines, "旧 CSV preLines 保持")
        assertEquals(doc1.dedRows.size, doc2.dedRows.size, "旧 CSV dedRows 保持")
        assertEquals(doc1.textSize, doc2.textSize, "旧 CSV textSize 保持")
        assertEquals(doc1.listAngle, doc2.listAngle, "旧 CSV listAngle 保持")
    }

    /** dedRows の round-trip 等価 (viewscale=1) */
    @Test
    fun dedRows_roundtrip_viewscale1() {
        val doc1 = CsvCodec.parse(TRAP_MIXED_CSV)
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
        /** 台形 + TriTrap + 控除 入り CSV (Web bake 相当) */
        val TRAP_MIXED_CSV =
            "koujiname, 台形混在テスト\n" +
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
            "Trapezoid, 3.0, 2.0, 4.0, -1, 0.0, 0.0, 0.0, true, 4\n" +
            "Trapezoid, 2.5, 1.5, 3.5, 1, 0.0, 0.0, 0.0, false, 4\n" +
            "4, 3.0, 2.5, 2.0, 4, 1\n"  // parent=4 > rows.size=3 -> trapParentedTriRows

        /** 旧 app 由来 CSV (台形なし、28 列形式) -- 回帰防止用 */
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
