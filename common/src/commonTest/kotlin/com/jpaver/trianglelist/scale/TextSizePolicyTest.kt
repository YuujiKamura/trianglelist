package com.jpaver.trianglelist.scale

import kotlin.test.Test
import kotlin.test.assertEquals

class TextSizePolicyTest {
    private val delta = 0.0001f

    @Test
    fun `JIS 主流の paper mm 定数が階段の中の値`() {
        assertEquals(3.5f, TextSizePolicy.DIMENSION_PAPER_MM, delta)
        // 2026-08-12: desktop/sample/sample.dxf (user が最適と判断した既存表題欄) の実測値
        // (model 125.0mm ÷ 1/50 scale = paper 2.5mm、JIS ラダー最小段) に合わせて確定。
        assertEquals(2.5f, TextSizePolicy.FRAME_LABEL_PAPER_MM, delta)
        // user 指示「上部タイトルはこれに対して2倍」: 2.5mm の 2 倍 = 5.0mm。
        assertEquals(5.0f, TextSizePolicy.TITLE_PAPER_MM, delta)
    }

    @Test
    fun `1分の50 図面で寸法値 3_5 mm を model に換算すると 175 mm`() {
        val modelMm = TextSizePolicy.paperToModel(TextSizePolicy.DIMENSION_PAPER_MM, 50f)
        assertEquals(175f, modelMm, delta)
    }

    @Test
    fun `1分の150 図面で枠ラベル 2_5 mm を model に換算すると 375 mm`() {
        val modelMm = TextSizePolicy.paperToModel(TextSizePolicy.FRAME_LABEL_PAPER_MM, 150f)
        assertEquals(375f, modelMm, delta)
    }

    @Test
    fun `1分の600 図面で寸法値 3_5 mm を model に換算すると 2100 mm`() {
        val modelMm = TextSizePolicy.paperToModel(TextSizePolicy.DIMENSION_PAPER_MM, 600f)
        assertEquals(2100f, modelMm, delta)
    }

    @Test
    fun `paper to model と model to paper は互いに逆関数`() {
        val drawingScale = 150f
        val paperMm = 5.0f
        val modelMm = TextSizePolicy.paperToModel(paperMm, drawingScale)
        val paperMmBack = TextSizePolicy.modelToPaper(modelMm, drawingScale)
        assertEquals(paperMm, paperMmBack, delta)
    }

    @Test
    fun `drawingScale が 0 のとき modelToPaper は 0 を返す (zero division ガード)`() {
        assertEquals(0f, TextSizePolicy.modelToPaper(100f, 0f), delta)
    }

    @Test
    fun `現状 trianglelist の model textsize 0_25 を 1分の50 で逆算すると paper 0_005 mm`() {
        val paperMm = TextSizePolicy.modelToPaper(0.25f, 50f)
        assertEquals(0.005f, paperMm, delta)
    }

    @Test
    fun `現状 1分の600 で model 0_25 を逆算すると paper 0_00042 mm でほぼ消える`() {
        val paperMm = TextSizePolicy.modelToPaper(0.25f, 600f)
        assertEquals(0.000417f, paperMm, 0.00001f)
    }
}
