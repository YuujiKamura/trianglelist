package com.jpaver.trianglelist

import org.junit.Assert.assertEquals
import org.junit.Test

class TextScaleCalculatorTest {
    private val calculator = TextScaleCalculator()
    private val DELTA = 0.0001f

    @Test
    fun `DXFファイルの場合、スケールに応じて適切なテキストスケールを返す`() {
        val testCases = mapOf(
            TextScaleCalculator.S_500 to 0.45f,
            TextScaleCalculator.S_400 to 0.40f,
            TextScaleCalculator.S_300 to 0.35f,
            TextScaleCalculator.S_250 to 0.35f,
            TextScaleCalculator.S_200 to 0.30f,
            TextScaleCalculator.S_100 to 0.25f,
            TextScaleCalculator.S_50 to 0.25f
        )
        
        testCases.forEach { (scale, expectedTextScale) ->
            assertEquals(
                "1/${(scale * 10).toInt()} のスケールの場合",
                expectedTextScale,
                calculator.getTextScale(scale, "dxf"),
                DELTA
            )
        }
    }

    @Test
    fun `PDFファイルの場合、スケールに応じて適切なテキストスケールを返す`() {
        val testCases = mapOf(
            TextScaleCalculator.S_500 to 3f,
            TextScaleCalculator.S_400 to 5f,
            TextScaleCalculator.S_300 to 5f,
            TextScaleCalculator.S_250 to 6f,
            TextScaleCalculator.S_200 to 8f,
            TextScaleCalculator.S_150 to 8f,
            TextScaleCalculator.S_100 to 8f,
            TextScaleCalculator.S_50 to 8f
        )
        
        testCases.forEach { (scale, expectedTextScale) ->
            assertEquals(
                "1/${(scale * 10).toInt()} のスケールの場合",
                expectedTextScale,
                calculator.getTextScale(scale, "pdf"),
                DELTA
            )
        }
    }

    @Test
    fun `定義されていないスケールの場合、デフォルト値を返す`() {
        // DXFの場合
        assertEquals(
            "DXFで未定義のスケール",
            0.25f,
            calculator.getTextScale(35f, "dxf"),  // 1/350
            DELTA
        )

        // PDFの場合
        assertEquals(
            "PDFで未定義のスケール",
            8f,
            calculator.getTextScale(8f, "pdf"),  // 1/80
            DELTA
        )
    }

    @Test
    fun `未知のファイルタイプの場合、PDFのマッピングを使用する`() {
        assertEquals(
            "未知のファイルタイプ",
            8f,
            calculator.getTextScale(TextScaleCalculator.S_200, "unknown"),
            DELTA
        )
    }
}
