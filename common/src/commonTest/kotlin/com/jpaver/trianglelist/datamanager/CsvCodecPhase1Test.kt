package com.jpaver.trianglelist.datamanager

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B01-B05 の新規メソッドテスト (Android CSV codec 移行 Phase 1)。
 *
 * B01: extractHeader / bakeHeader
 * B02: buildDeductions(doc, viewscale) overload
 * B03: bakeDeductions(dedlist, doc, viewscale)
 * B04: applyListParams(doc, trilist, setTextSize)
 * B04a: CsvDoc.listScale named field + parse/serialize/bake round-trip
 * B05: build(doc, applyRecoverState)
 */
class CsvCodecPhase1Test {

    // ─────────────────────────────────────────────
    // B01: extractHeader / bakeHeader
    // ─────────────────────────────────────────────

    private val headerCsv =
        "koujiname, 工事A\n" +
        "rosenname, 路線B\n" +
        "gyousyaname, 業者C\n" +
        "zumennum, 1/1\n" +
        "1,3.0,3.0,3.0,-1,-1\n"

    @Test
    fun b01_extractHeader_parses_four_fields() {
        // (a) parse + extractHeader で 4 フィールドが正しく取れる
        val doc = CsvCodec.parse(headerCsv)
        val h = CsvCodec.extractHeader(doc)
        assertEquals("工事A", h.koujiname)
        assertEquals("路線B", h.rosenname)
        assertEquals("業者C", h.gyousyaname)
        assertEquals("1/1", h.zumennum)
    }

    @Test
    fun b01_bakeHeader_roundtrip() {
        // (b) bakeHeader + serialize + parse + extractHeader で値が round-trip
        val doc = CsvCodec.parse(headerCsv)
        val h = CsvCodec.extractHeader(doc)
        h.koujiname = "新工事Z"
        val doc2 = CsvCodec.bakeHeader(h, doc)
        val doc3 = CsvCodec.parse(CsvCodec.serialize(doc2))
        val h2 = CsvCodec.extractHeader(doc3)
        assertEquals("新工事Z", h2.koujiname)
        assertEquals("路線B", h2.rosenname)
    }

    @Test
    fun b01_bakeHeader_no_duplicate_when_preLines_already_has_headers() {
        // (c) preLines に同名行が既にある場合でも重複しない (filter で除去してから先頭挿入)
        val doc = CsvCodec.parse(headerCsv)
        val h = CsvCodec.extractHeader(doc)
        val doc2 = CsvCodec.bakeHeader(h, CsvCodec.bakeHeader(h, doc))
        // koujiname 行は 1 つだけ
        assertEquals(1, doc2.preLines.count { it.startsWith("koujiname") })
        assertEquals(4, doc2.preLines.size) // 4 ヘッダ行のみ (元 preLines も 4 行なので重複排除後も 4)
    }

    @Test
    fun b01_bakeHeader_bom_prefix_line_not_treated_as_header() {
        // (d) preLines 先頭が BOM 付きコメント行の場合、bakeHeader でヘッダが重複しない
        val csv = "﻿# comment\nkoujiname, 工事A\nrosenname, 路線B\ngyousyaname, 業者C\nzumennum, 1/1\n1,3.0,3.0,3.0,-1,-1\n"
        val doc = CsvCodec.parse(csv)
        val h = CsvCodec.extractHeader(doc)
        val doc2 = CsvCodec.bakeHeader(h, doc)
        // koujiname 行は 1 つだけ (BOM 行と 4 ヘッダ = 5 行)
        assertEquals(1, doc2.preLines.count { it.startsWith("koujiname") })
        // BOM 付きコメント行は残る
        assertTrue(doc2.preLines.any { it.startsWith("﻿") })
    }

    @Test
    fun b01_extractHeader_last_value_wins_on_duplicate() {
        // preLines に同名行が重複しても extractHeader は後勝ち
        val csv = "koujiname, 旧工事\nkoujiname, 新工事\n1,3.0,3.0,3.0,-1,-1\n"
        val doc = CsvCodec.parse(csv)
        val h = CsvCodec.extractHeader(doc)
        assertEquals("新工事", h.koujiname)
    }

    // ─────────────────────────────────────────────
    // B02: buildDeductions(doc, viewscale)
    // ─────────────────────────────────────────────

    private val dedCsv =
        "1,3.0,3.0,3.0,-1,-1\n" +
        "Deduction,1,仕切弁,0.23,0.0,1,Circle,0.0,1.0,2.0,1.5,2.5,0.0\n" +
        "Deduction,2,集水桝,0.8,0.6,0,Box,0.0,-1.0,-2.0,0.0,0.0,15.5\n"

    @Test
    fun b02_buildDeductions_viewscale1_equals_no_viewscale() {
        // (a) viewscale=1f で 1 引数版と完全等価
        val doc = CsvCodec.parse(dedCsv)
        val d1 = CsvCodec.buildDeductions(doc)
        val d2 = CsvCodec.buildDeductions(doc, 1f)
        assertEquals(d1.size(), d2.size())
        for (i in 1..d1.size()) {
            assertEquals(d1.get(i).point.x, d2.get(i).point.x, 1e-6)
            assertEquals(d1.get(i).point.y, d2.get(i).point.y, 1e-6)
            assertEquals(d1.get(i).pointFlag.x, d2.get(i).pointFlag.x, 1e-6)
            assertEquals(d1.get(i).pointFlag.y, d2.get(i).pointFlag.y, 1e-6)
        }
    }

    @Test
    fun b02_buildDeductions_viewscale_half_scales_point() {
        // (b) viewscale=0.5 で point.x が 1 引数版の半分
        val doc = CsvCodec.parse(dedCsv)
        val d1 = CsvCodec.buildDeductions(doc, 1f)
        val d05 = CsvCodec.buildDeductions(doc, 0.5f)
        assertEquals(d1.get(1).point.x * 0.5, d05.get(1).point.x, 1e-6)
        assertEquals(d1.get(1).point.y * 0.5, d05.get(1).point.y, 1e-6)
    }

    @Test
    fun b02_buildDeductions_viewscale_double_scales_point() {
        // (c) viewscale=2.0 で point.x が 1 引数版の倍
        val doc = CsvCodec.parse(dedCsv)
        val d1 = CsvCodec.buildDeductions(doc, 1f)
        val d2 = CsvCodec.buildDeductions(doc, 2f)
        assertEquals(d1.get(1).point.x * 2.0, d2.get(1).point.x, 1e-6)
        assertEquals(d1.get(1).point.y * 2.0, d2.get(1).point.y, 1e-6)
    }

    // ─────────────────────────────────────────────
    // B03: bakeDeductions(dedlist, doc, viewscale)
    // ─────────────────────────────────────────────

    @Test
    fun b03_bakeDeductions_roundtrip_viewscale1() {
        // (a) parse → buildDeductions(vs) → bakeDeductions(vs) → serialize → parse → buildDeductions が等価
        val doc = CsvCodec.parse(dedCsv)
        val vs = 1f
        val dedlist = CsvCodec.buildDeductions(doc, vs)
        val baked = CsvCodec.bakeDeductions(dedlist, doc, vs)
        val doc2 = CsvCodec.parse(CsvCodec.serialize(baked))
        val dedlist2 = CsvCodec.buildDeductions(doc2, vs)
        assertEquals(dedlist.size(), dedlist2.size())
        for (i in 1..dedlist.size()) {
            assertEquals(dedlist.get(i).point.x, dedlist2.get(i).point.x, 1e-4)
            assertEquals(dedlist.get(i).point.y, dedlist2.get(i).point.y, 1e-4)
            assertEquals(dedlist.get(i).shapeAngle, dedlist2.get(i).shapeAngle, 1e-6)
        }
    }

    @Test
    fun b03_bakeDeductions_empty_dedlist_clears_dedRows() {
        // (b) 空 dedlist で bakeDeductions すると doc.dedRows が空に
        val doc = CsvCodec.parse(dedCsv)
        val emptyDed = com.jpaver.trianglelist.editmodel.DeductionList()
        val baked = CsvCodec.bakeDeductions(emptyDed, doc, 1f)
        assertEquals(0, baked.dedRows.size)
    }

    @Test
    fun b03_bakeDeductions_roundtrip_viewscale_half() {
        // (c) viewscale=0.5 と 2.0 で bake → 内部 chunks 値が逆方向に倍率変化
        val doc = CsvCodec.parse(dedCsv)
        val dedlist05 = CsvCodec.buildDeductions(doc, 0.5f)
        val dedlist20 = CsvCodec.buildDeductions(doc, 2.0f)
        val baked05 = CsvCodec.bakeDeductions(dedlist05, doc, 0.5f)
        val baked20 = CsvCodec.bakeDeductions(dedlist20, doc, 2.0f)
        // 元の doc.dedRows の point 値 (chunks[8]) と baked 後の値が等しい (逆変換で元に戻る)
        val origPx = doc.dedRows[0].chunks[8].toDouble()
        val baked05Px = baked05.dedRows[0].chunks[8].toDouble()
        val baked20Px = baked20.dedRows[0].chunks[8].toDouble()
        assertEquals(origPx, baked05Px, 1e-4)
        assertEquals(origPx, baked20Px, 1e-4)
    }

    // ─────────────────────────────────────────────
    // B04: applyListParams
    // ─────────────────────────────────────────────

    @Test
    fun b04_applyListParams_sets_textSize() {
        // (a) doc.textSize=42f で setTextSize コールバックが 42f を受ける
        val doc = CsvCodec.parse("1,3.0,3.0,3.0,-1,-1\nTextSize, 42.0\n")
        var received = -1f
        CsvCodec.applyListParams(doc, TriangleList()) { received = it }
        assertEquals(42.0f, received, 0.001f)
    }

    @Test
    fun b04_applyListParams_sets_listScale_from_named_field() {
        // (b) doc.listScale があれば trilist.scale に反映
        val doc = CsvCodec.parse("1,3.0,3.0,3.0,-1,-1\nListScale, 1.5\n")
        assertEquals(1.5f, doc.listScale!!, 0.001f) // B04a: named field に昇格済み
        val trilist = TriangleList()
        CsvCodec.applyListParams(doc, trilist) {}
        assertEquals(1.5f, trilist.scale, 0.001f)
    }

    @Test
    fun b04_applyListParams_noop_when_no_params() {
        // (c) TextSize/ListScale がなければ no-op
        val doc = CsvCodec.parse("1,3.0,3.0,3.0,-1,-1\n")
        val trilist = TriangleList()
        val initScale = trilist.scale
        var callbackCalled = false
        CsvCodec.applyListParams(doc, trilist) { callbackCalled = true }
        assertEquals(initScale, trilist.scale, 0.001f)
        assertEquals(false, callbackCalled)
    }

    @Test
    fun b04_applyListParams_fallback_postLines_listScale() {
        // B04aでpostLinesにListScaleが残っていても後方互換でscaleを読む
        // (postLinesに古い形式が残るケース: 別経路で構築したCsvDoc)
        val doc = CsvCodec.parse("1,3.0,3.0,3.0,-1,-1\n").copy(
            postLines = listOf("ListScale, 2.0"),
            listScale = null
        )
        val trilist = TriangleList()
        CsvCodec.applyListParams(doc, trilist) {}
        assertEquals(2.0f, trilist.scale, 0.001f)
    }

    // ─────────────────────────────────────────────
    // B04a: CsvDoc.listScale named field
    // ─────────────────────────────────────────────

    @Test
    fun b04a_parse_promotes_listScale_to_named_field() {
        // (a) parse → doc.listScale が CSV 内の ListScale 値と一致
        val csv = "1,3.0,3.0,3.0,-1,-1\nListAngle, 10.0\nListScale, 1.5\n"
        val doc = CsvCodec.parse(csv)
        assertEquals(1.5f, doc.listScale!!, 0.001f)
        // postLines に ListScale が残存しない
        assertTrue(doc.postLines.none { it.trimStart().startsWith("ListScale") })
    }

    @Test
    fun b04a_listScale_roundtrip_via_serialize_parse() {
        // (b) parse → bake(trilist, doc) → serialize → parse で listScale が round-trip
        val csv = "1,3.0,3.0,3.0,-1,-1\nListAngle, 0.0\nListScale, 2.0\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val baked = CsvCodec.serialize(CsvCodec.bake(trilist, doc))
        val doc2 = CsvCodec.parse(baked)
        // trilist.scale はデフォルト 1f なので bake 後は 1f になる
        assertEquals(1.0f, doc2.listScale!!, 0.001f)
    }

    @Test
    fun b04a_bake_writes_trilist_scale_to_listScale() {
        // (c) trilist.scale を変更 → bake → serialize → parse で新 scale が反映
        val csv = "1,3.0,3.0,3.0,-1,-1\nListScale, 1.0\n"
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        trilist.setScale(PointXY(0f, 0f), 3.0f)
        val baked = CsvCodec.serialize(CsvCodec.bake(trilist, doc))
        val doc2 = CsvCodec.parse(baked)
        assertEquals(3.0f, doc2.listScale!!, 0.001f)
    }

    @Test
    fun b04a_listScale_null_when_no_listscale_row() {
        // (d) ListScale 行が無い CSV で doc.listScale == null、postLines に残存しない
        val csv = "1,3.0,3.0,3.0,-1,-1\nListAngle, 0.0\n"
        val doc = CsvCodec.parse(csv)
        assertNull(doc.listScale)
        assertTrue(doc.postLines.none { it.trimStart().startsWith("ListScale") })
    }

    // ─────────────────────────────────────────────
    // B05: build(doc, applyRecoverState)
    // ─────────────────────────────────────────────

    private val buildCsv =
        "1,3.0,3.0,3.0,-1,-1\n" +
        "2,3.0,2.5,2.5,1,1\n" +
        "ListAngle, 30\n"

    @Test
    fun b05_build_default_equals_apply_recover_state_true() {
        // (a) build(doc) (default) と build(doc, true) が完全等価
        val doc = CsvCodec.parse(buildCsv)
        val t1 = CsvCodec.build(doc)
        val t2 = CsvCodec.build(doc, applyRecoverState = true)
        assertEquals(t1.size(), t2.size())
        for (i in 1..t1.size()) {
            assertEquals(t1.getBy(i).angle, t2.getBy(i).angle, 1e-4f)
            assertEquals(t1.getBy(i).pointCA.x, t2.getBy(i).pointCA.x, 1e-4)
            assertEquals(t1.getBy(i).pointCA.y, t2.getBy(i).pointCA.y, 1e-4)
        }
    }

    @Test
    fun b05_build_false_sets_angle_only_no_recover() {
        // (b) build(doc, false) は trilist.angle のみ doc.listAngle で書き換え、recoverState は呼ばない
        val doc = CsvCodec.parse(buildCsv)
        val tFalse = CsvCodec.build(doc, applyRecoverState = false)
        // angle は listAngle が入っている
        assertEquals(30f, tFalse.angle, 0.001f)
        // recoverState が呼ばれていない → 三角形の絶対座標がrecoverState前の状態
        // recoverState 済の trilist と座標が異なることを確認
        val tTrue = CsvCodec.build(doc, applyRecoverState = true)
        // recoverState 後は angle が -150f 等に変わるはず (30 - 180 = -150)
        // 少なくとも angle が異なる
        val angFalse = tFalse.getBy(1).angle
        val angTrue = tTrue.getBy(1).angle
        // recoverState 呼ぶと Triangle.angle が変わるのでどちらかが 0 以外になる
        assertTrue(angFalse != angTrue || tFalse.getBy(1).pointCA.x != tTrue.getBy(1).pointCA.x)
    }

    @Test
    fun b05_build_true_does_not_break_existing_golden_tests() {
        // (c) 既存の build(doc) 呼び出しと build(doc, true) が等価 — golden 回帰なし
        val csv = "1,3.0,3.0,3.0,-1,-1\n2,3.0,2.5,2.5,1,1\nListAngle, 10\n"
        val doc = CsvCodec.parse(csv)
        val t1 = CsvCodec.build(doc)
        val t2 = CsvCodec.build(doc, applyRecoverState = true)
        assertEquals(t1.angle, t2.angle, 1e-4f)
        assertEquals(t1.size(), t2.size())
    }
}
