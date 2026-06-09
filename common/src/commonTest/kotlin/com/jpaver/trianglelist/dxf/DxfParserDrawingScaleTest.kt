package com.jpaver.trianglelist.dxf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

/**
 * DXF から drawingScale を抽出するテスト。
 * trianglelist の DrawingFileWriter.writeDrawingFrame は必ず
 * 「1/${st.toInt()} (A3)」形式の TEXT を埋め込むので、それを正規表現で抽出する。
 */
class DxfParserDrawingScaleTest {

    private val parser = DxfParser()

    /** 最小 DXF を組み立てる: 単一の TEXT entity に縮尺表記を入れる。 */
    private fun dxfWithScaleText(text: String): String = buildString {
        appendLine("0"); appendLine("SECTION")
        appendLine("2"); appendLine("ENTITIES")
        appendLine("0"); appendLine("TEXT")
        appendLine("8"); appendLine("0")
        appendLine("10"); appendLine("0.0")
        appendLine("20"); appendLine("0.0")
        appendLine("40"); appendLine("2.5")
        appendLine("1"); appendLine(text)
        appendLine("0"); appendLine("ENDSEC")
        appendLine("0"); appendLine("EOF")
    }

    @Test
    fun `1分の50 (A3) という TEXT から 50f を抽出`() {
        val result = parser.parse(dxfWithScaleText("1/50 (A3)"))
        assertNotNull(result.drawingScaleDenominator)
        assertEquals(50f, result.drawingScaleDenominator)
    }

    @Test
    fun `1分の150 (A3) という TEXT から 150f を抽出`() {
        val result = parser.parse(dxfWithScaleText("1/150 (A3)"))
        assertEquals(150f, result.drawingScaleDenominator)
    }

    @Test
    fun `1分の600 (A3) という TEXT から 600f を抽出`() {
        val result = parser.parse(dxfWithScaleText("1/600 (A3)"))
        assertEquals(600f, result.drawingScaleDenominator)
    }

    @Test
    fun `縮尺表記がない DXF では null を返す`() {
        val result = parser.parse(dxfWithScaleText("面 積 展 開 図"))
        assertNull(result.drawingScaleDenominator)
    }

    @Test
    fun `A3 がなくても 1分のNN 形式が最初の TEXT にあれば抽出する`() {
        // writeDrawingFrame 以外で同じ表記を出す path に備えて A3 サフィックスは任意
        val result = parser.parse(dxfWithScaleText("1/100"))
        assertEquals(100f, result.drawingScaleDenominator)
    }

    @Test
    fun `非縮尺の数字混じり TEXT は誤検出しない`() {
        // 例えば「面積 1.5 m2」のような表記は drawing scale ではない
        val result = parser.parse(dxfWithScaleText("面積 1.5 m2"))
        assertNull(result.drawingScaleDenominator)
    }
}
