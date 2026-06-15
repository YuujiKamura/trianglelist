package com.jpaver.trianglelist.datamanager

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B11: viewscale != 1 で Deduction 座標が round-trip 等価。
 *
 * buildDeductions(doc, viewscale) → bakeDeductions(dedlist, doc, viewscale) →
 * serialize → parse → buildDeductions(doc2, viewscale) で座標が ε=1e-3 内で一致する。
 *
 * common module の pure Kotlin テスト (Robolectric 不要)。
 * B02/B03 の実装済み 2 引数版 API を直接呼ぶ。
 */
class CsvDeductionViewscaleTest {

    private val dedCsv =
        "1,3.0,3.0,3.0,-1,-1\n" +
        "Deduction,1,仕切弁,0.23,0.0,1,Circle,0.0,1.0,2.0,1.5,2.5,0.0\n" +
        "Deduction,2,集水桝,0.8,0.6,0,Box,0.0,-1.0,-2.0,0.0,0.0,15.5\n"

    @Test
    fun viewscale_half_deduction_roundtrip() = roundtrip(0.5f)

    @Test
    fun viewscale_one_deduction_roundtrip() = roundtrip(1.0f)

    @Test
    fun viewscale_double_deduction_roundtrip() = roundtrip(2.0f)

    private fun roundtrip(viewscale: Float) {
        val doc1 = CsvCodec.parse(dedCsv)

        // build → bake → serialize → parse → build の round-trip
        val ded1 = CsvCodec.buildDeductions(doc1, viewscale)
        val doc2 = CsvCodec.bakeDeductions(ded1, doc1, viewscale)
        val doc3 = CsvCodec.parse(CsvCodec.serialize(doc2))
        val ded2 = CsvCodec.buildDeductions(doc3, viewscale)

        assertEquals(ded1.size(), ded2.size(), "viewscale=$viewscale: dedlist size")
        for (i in 1..ded1.size()) {
            val a = ded1.get(i)
            val b = ded2.get(i)
            assertEquals(a.point.x, b.point.x, 1e-3, "viewscale=$viewscale: ded[$i].point.x")
            assertEquals(a.point.y, b.point.y, 1e-3, "viewscale=$viewscale: ded[$i].point.y")
            assertEquals(a.pointFlag.x, b.pointFlag.x, 1e-3, "viewscale=$viewscale: ded[$i].pointFlag.x")
            assertEquals(a.pointFlag.y, b.pointFlag.y, 1e-3, "viewscale=$viewscale: ded[$i].pointFlag.y")
        }
    }
}
