package com.jpaver.trianglelist

import org.junit.Test
import org.junit.Assert.assertEquals

class TextScaleCalculatorTest {
    private val calculator = TextScaleCalculator()
    private val DELTA = 0.0001f

    @Test
    fun `DXFファイルの場合、スケールに応じて適切なテキストスケールを返す`() {
        val testCases = mapOf(
            50f to 0.5f,    // 1/500
            40f to 0.4f,    // 1/400
            25f to 0.35f,   // 1/250
            20f to 0.35f,   // 1/200
            15f to 0.25f,   // 1/150
            10f to 0.25f,   // 1/100
            5f to 0.25f     // 1/50
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
            50f to 3f,    // 1/500
            40f to 4f,    // 1/400
            25f to 6f,    // 1/250
            20f to 8f,    // 1/200
            15f to 8f,    // 1/150
            10f to 8f     // 1/100
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
        assertEquals(
            "DXFで未定義のスケール",
            0.25f,
            calculator.getTextScale(35f, "dxf"),  // 1/350
            DELTA
        )

        assertEquals(
            "PDFで未定義のスケール",
            8f,
            calculator.getTextScale(8f, "pdf"),  // 1/80
            DELTA
        )
    }

    @Test
    fun `未知のファイルタイプの場合、デフォルト値を返す`() {
        assertEquals(
            "未知のファイルタイプ",
            5f,  // デフォルトのデフォルト値
            calculator.getTextScale(20f, "unknown"),
            DELTA
        )
    }
} 