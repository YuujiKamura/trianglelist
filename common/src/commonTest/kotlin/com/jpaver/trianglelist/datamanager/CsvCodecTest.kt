package com.jpaver.trianglelist.datamanager

import com.jpaver.trianglelist.web.WebOverrides
import com.jpaver.trianglelist.web.WebPrimitiveRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CSV codec (ADR 0008) の round-trip テスト — リファクタの安全網。
 * - parse → serialize がトークンを失わない (未知の列・行も保持、schema evolution の定石)
 * - build → bake → serialize → parse → build が同じ絵になる (model レベルの round-trip)
 * - overrides の bake = 手動配置の書き戻し (web → app のユーザー損失解消) の pin
 */
class CsvCodecTest {

    /** アプリ writeCSV (MainActivity:2745-2792) と同じ構成の完全形式 fixture */
    private val fullCsv =
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

    @Test
    fun parse_serialize_preserves_everything() {
        val doc = CsvCodec.parse(fullCsv)
        assertEquals(4, doc.preLines.size)
        assertEquals(2, doc.rows.size)
        assertEquals(15.0f, doc.listAngle!!, 0.001f)
        assertEquals(3, doc.postLines.size) // ListScale + TextSize + Deduction
        assertEquals(28, doc.rows[0].chunks.size)
        // serialize → parse → serialize が固定点 (idempotence)
        val once = CsvCodec.serialize(doc)
        val twice = CsvCodec.serialize(CsvCodec.parse(once))
        assertEquals(once, twice)
        // 行の全トークンが保持される
        assertEquals(doc.rows[1].chunks, CsvCodec.parse(once).rows[1].chunks)
    }

    @Test
    fun parse_preserves_unknown_columns_and_lines() {
        val csv = "1,3.0,3.0,3.0,-1,-1" + ",x".repeat(22) + ",FUTURE1,FUTURE2\n" +
            "SomeFutureDirective, 42\n"
        val doc = CsvCodec.parse(csv)
        assertEquals(30, doc.rows[0].chunks.size)
        assertEquals("FUTURE2", doc.rows[0].chunks[29])
        assertEquals(listOf("SomeFutureDirective, 42"), doc.postLines)
        val out = CsvCodec.serialize(doc)
        assertTrue(out.contains("FUTURE2") && out.contains("SomeFutureDirective, 42"))
    }

    @Test
    fun build_bake_rebuild_renders_identically() {
        // 最小形式 (二重断面+フロート入り) → 構築 → bake → 再構築 が同じ絵になる
        val csv =
            "1,3.0,3.0,3.0,-1,-1\n" +
                "2,3.0,2.5,2.5,1,1\n" +
                "3,1.2,2.0,2.0,1,5\n" +
                "4,1.5,1.5,1.5,2,10\n" +
                "ListAngle, 10\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val baked = CsvCodec.serialize(CsvCodec.bake(trilist, doc))
        val rebuilt = CsvCodec.build(CsvCodec.parse(baked))
        assertEquals(
            WebPrimitiveRenderer.render(trilist, 0.25f),
            WebPrimitiveRenderer.render(rebuilt, 0.25f),
        )
        // bake は完全形式 28 列 + ListAngle を書く
        val bakedDoc = CsvCodec.parse(baked)
        assertEquals(28, bakedDoc.rows[0].chunks.size)
        assertEquals(10.0f, bakedDoc.listAngle!!, 0.001f)
    }

    @Test
    fun bake_writes_back_manual_overrides() {
        // ④ の pin: web の overrides (W/H フリップ・番号移動) が CSV の列に焼き込まれ、
        // アプリ (CsvLoader) と web (CsvCodec.build) の双方が復元できる
        val csv = "1,3.0,3.0,3.0,-1,-1\n2,3.0,2.5,2.5,1,1\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        WebOverrides.applyJson(
            trilist,
            """{"dims":[{"tri":1,"side":2,"v":3,"h":1}],"numbers":[{"tri":2,"x":9.0,"y":9.5}]}""",
        )
        val baked = CsvCodec.parse(CsvCodec.serialize(CsvCodec.bake(trilist, doc)))
        val r1 = baked.rows[0].chunks
        assertEquals("1", r1[13]) // dim.horizontal.c ← h override (side=2 は C 辺)
        assertEquals("3", r1[16]) // dim.vertical.c ← v override
        assertEquals("true", r1[21]) // C 辺の override は flag[2] に立つ
        val r2 = baked.rows[1].chunks
        assertEquals("true", r2[9]) // 番号サークル移動フラグ
        assertEquals(9.0f, r2[7].toFloat(), 0.001f)
        assertEquals(9.5f, r2[8].toFloat(), 0.001f)
        // 再構築しても override が生きている (CSV だけで往復できる = localStorage 非依存)
        val rebuilt = CsvCodec.build(baked)
        assertEquals(3, rebuilt.getBy(1).dim.vertical.c)
        assertEquals(9.0f, rebuilt.getBy(2).pointnumber.x.toFloat(), 0.001f)
    }
}
