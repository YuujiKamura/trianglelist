package com.jpaver.trianglelist.adapter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.jpaver.trianglelist.dxf.DxfText
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import kotlin.math.abs

/**
 * テキストジオメトリの精密テストクラス
 * テキストの位置、サイズ、境界計算の精度を様々な条件でテスト
 */
class TextGeometryTest {
    
    private lateinit var textRenderer: TextRenderer
    private lateinit var mockDrawScope: DrawScope
    private lateinit var mockTextMeasurer: TextMeasurer
    private lateinit var mockTextLayoutResult: TextLayoutResult
    
    @BeforeEach
    fun setup() {
        textRenderer = TextRenderer()
        mockDrawScope = mockk(relaxed = true)
        mockTextMeasurer = mockk()
        mockTextLayoutResult = mockk()
        
        // デフォルトのモック設定
        every { mockTextMeasurer.measure(any<String>(), any<TextStyle>()) } returns mockTextLayoutResult
        every { mockTextLayoutResult.size } returns IntSize(100, 20) // デフォルト: 100x20ピクセル
    }
    
    /**
     * 基本的なテキスト境界計算のテスト
     */
    @Test
    fun `test basic text bounds calculation`() {
        val text = DxfText(
            x = 100.0,
            y = 200.0,
            text = "TEST",
            height = 10.0,
            alignH = 0, // 左揃え
            alignV = 0  // ベースライン
        )
        
        val scale = 1.0f
        val bounds = textRenderer.calculateTextBounds(text, scale, mockTextMeasurer)
        
        assertEquals(4, bounds.size, "境界は4つの値を返すべき")
        
        val (minX, maxX, minY, maxY) = bounds
        assertTrue(minX < maxX, "minX < maxX であるべき")
        assertTrue(minY < maxY, "minY < maxY であるべき")
        
        // 期待値の計算
        val expectedWidth = 100f // mockの設定値
        val expectedHeight = 20f // mockの設定値
        val expectedMinX = 100f // 左揃えなのでX座標そのまま
        val expectedMaxX = expectedMinX + expectedWidth
        
        assertFloatEquals(expectedMinX, minX, 0.01f, "MinX should match expected value")
        assertFloatEquals(expectedMaxX, maxX, 0.01f, "MaxX should match expected value")
    }
    
    /**
     * 中央揃えテキストの境界計算テスト
     */
    @Test
    fun `test center aligned text bounds`() {
        val text = DxfText(
            x = 100.0,
            y = 200.0,
            text = "CENTER",
            height = 10.0,
            alignH = 1, // 中央揃え
            alignV = 2  // 垂直中央
        )
        
        val scale = 1.0f
        val bounds = textRenderer.calculateTextBounds(text, scale, mockTextMeasurer)
        
        val (minX, maxX, minY, maxY) = bounds
        val width = maxX - minX
        val height = maxY - minY
        
        // 中央揃えなので、基準点が中央になるはず
        val centerX = (minX + maxX) / 2
        val centerY = (minY + maxY) / 2
        
        assertFloatEquals(100f, centerX, 0.01f, "中央揃えのX中心位置が正しくない")
        assertFloatEquals(200f, centerY, 0.01f, "中央揃えのY中心位置が正しくない")
    }
    
    /**
     * 右揃えテキストの境界計算テスト
     */
    @Test
    fun `test right aligned text bounds`() {
        val text = DxfText(
            x = 100.0,
            y = 200.0,
            text = "RIGHT",
            height = 10.0,
            alignH = 2, // 右揃え
            alignV = 3  // 上揃え
        )
        
        val scale = 1.0f
        val bounds = textRenderer.calculateTextBounds(text, scale, mockTextMeasurer)
        
        val (minX, maxX, minY, maxY) = bounds
        
        // 右揃えなので、maxXが基準点と一致するはず
        assertFloatEquals(100f, maxX, 0.01f, "右揃えのX最大位置が基準点と一致しない")
        
        // 上揃えなので、minYが基準点と一致するはず
        assertFloatEquals(200f, minY, 0.01f, "上揃えのY最小位置が基準点と一致しない")
    }
    
    /**
     * 様々なスケールでのテキスト境界テスト
     */
    @Test
    fun `test text bounds with different scales`() {
        val text = DxfText(
            x = 0.0,
            y = 0.0,
            text = "SCALE",
            height = 10.0,
            alignH = 0,
            alignV = 0
        )
        
        val scales = listOf(0.5f, 1.0f, 2.0f, 5.0f)
        
        scales.forEach { scale ->
            val bounds = textRenderer.calculateTextBounds(text, scale, mockTextMeasurer)
            val (minX, maxX, minY, maxY) = bounds
            val width = maxX - minX
            val height = maxY - minY
            
            assertTrue(width > 0, "スケール $scale でのテキスト幅が正の値でない")
            assertTrue(height > 0, "スケール $scale でのテキスト高さが正の値でない")
        }
    }
    
    /**
     * 異なるフォントサイズでのテキスト測定テスト
     */
    @Test
    fun `test text measurement with different font sizes`() {
        val fontSizes = listOf(8.0, 12.0, 16.0, 24.0, 36.0)
        
        fontSizes.forEach { fontSize ->
            // 各フォントサイズに対してモックの戻り値を設定
            val expectedWidth = (fontSize * 5).toInt() // 文字数5のテキストの想定幅
            val expectedHeight = fontSize.toInt()
            
            every { mockTextLayoutResult.size } returns IntSize(expectedWidth, expectedHeight)
            
            val text = DxfText(
                x = 0.0,
                y = 0.0,
                text = "ABCDE",
                height = fontSize,
                alignH = 0,
                alignV = 0
            )
            
            val bounds = textRenderer.calculateTextBounds(text, 1.0f, mockTextMeasurer)
            val (minX, maxX, minY, maxY) = bounds
            val width = maxX - minX
            val height = maxY - minY
            
            assertFloatEquals(expectedWidth.toFloat(), width, 0.01f, 
                "フォントサイズ $fontSize での幅が期待値と一致しない")
            assertFloatEquals(expectedHeight.toFloat(), height, 0.01f, 
                "フォントサイズ $fontSize での高さが期待値と一致しない")
        }
    }
    
    /**
     * テキストの精密な位置計算テスト
     */
    @Test
    fun `test precise text positioning`() {
        // 高精度の座標でテスト
        val preciseCoordinates = listOf(
            Pair(123.456, 789.012),
            Pair(0.001, 0.002),
            Pair(-50.75, 100.25),
            Pair(1000.999, -500.333)
        )
        
        preciseCoordinates.forEach { (x, y) ->
            val text = DxfText(
                x = x,
                y = y,
                text = "PRECISE",
                height = 10.0,
                alignH = 0, // 左揃え
                alignV = 3  // 上揃え
            )
            
            val bounds = textRenderer.calculateTextBounds(text, 1.0f, mockTextMeasurer)
            val (minX, maxX, minY, maxY) = bounds
            
            // 左上揃えなので、minXとminYが基準点と一致するはず
            assertFloatEquals(x.toFloat(), minX, 0.001f, 
                "精密座標 ($x, $y) でのX位置が正確でない")
            assertFloatEquals(y.toFloat(), minY, 0.001f, 
                "精密座標 ($x, $y) でのY位置が正確でない")
        }
    }
    
    /**
     * 全アライメント組み合わせのテスト
     */
    @Test
    fun `test all alignment combinations`() {
        val alignHValues = listOf(0, 1, 2) // 左, 中央, 右
        val alignVValues = listOf(0, 1, 2, 3) // ベースライン, 下, 中央, 上
        val baseX = 100.0
        val baseY = 200.0
        
        alignHValues.forEach { alignH ->
            alignVValues.forEach { alignV ->
                val text = DxfText(
                    x = baseX,
                    y = baseY,
                    text = "ALIGN",
                    height = 20.0,
                    alignH = alignH,
                    alignV = alignV
                )
                
                val bounds = textRenderer.calculateTextBounds(text, 1.0f, mockTextMeasurer)
                val (minX, maxX, minY, maxY) = bounds
                
                // 境界が有効な範囲にあることを確認
                assertTrue(minX < maxX, "alignH=$alignH, alignV=$alignV で minX >= maxX")
                assertTrue(minY < maxY, "alignH=$alignH, alignV=$alignV で minY >= maxY")
                
                // アライメントに応じた位置の検証
                when (alignH) {
                    0 -> assertFloatEquals(baseX.toFloat(), minX, 0.01f, "左揃えの位置が正しくない")
                    1 -> {
                        val centerX = (minX + maxX) / 2
                        assertFloatEquals(baseX.toFloat(), centerX, 0.01f, "中央揃えの位置が正しくない")
                    }
                    2 -> assertFloatEquals(baseX.toFloat(), maxX, 0.01f, "右揃えの位置が正しくない")
                }
            }
        }
    }
    
    /**
     * ゼロサイズテキストの処理テスト
     */
    @Test
    fun `test zero size text handling`() {
        every { mockTextLayoutResult.size } returns IntSize(0, 0)
        
        val text = DxfText(
            x = 100.0,
            y = 200.0,
            text = "",
            height = 10.0,
            alignH = 1,
            alignV = 2
        )
        
        val bounds = textRenderer.calculateTextBounds(text, 1.0f, mockTextMeasurer)
        val (minX, maxX, minY, maxY) = bounds
        
        // ゼロサイズでも有効な境界を返すべき
        assertEquals(minX, maxX, 0.01f, "ゼロ幅テキストの境界が正しくない")
        assertEquals(minY, maxY, 0.01f, "ゼロ高さテキストの境界が正しくない")
    }
    
    /**
     * 非常に大きなテキストの処理テスト
     */
    @Test
    fun `test very large text handling`() {
        every { mockTextLayoutResult.size } returns IntSize(10000, 1000)
        
        val text = DxfText(
            x = 0.0,
            y = 0.0,
            text = "VERY_LARGE_TEXT_".repeat(100),
            height = 100.0,
            alignH = 1,
            alignV = 2
        )
        
        val bounds = textRenderer.calculateTextBounds(text, 1.0f, mockTextMeasurer)
        val (minX, maxX, minY, maxY) = bounds
        
        assertTrue(maxX - minX == 10000f, "大きなテキストの幅が正しく計算されていない")
        assertTrue(maxY - minY == 1000f, "大きなテキストの高さが正しく計算されていない")
    }
    
    /**
     * マルチバイト文字のテスト
     */
    @Test
    fun `test multibyte character text`() {
        every { mockTextLayoutResult.size } returns IntSize(200, 24) // 日本語文字の想定サイズ
        
        val text = DxfText(
            x = 50.0,
            y = 75.0,
            text = "日本語テスト",
            height = 12.0,
            alignH = 1,
            alignV = 2
        )
        
        val bounds = textRenderer.calculateTextBounds(text, 1.0f, mockTextMeasurer)
        val (minX, maxX, minY, maxY) = bounds
        
        assertTrue(maxX - minX > 0, "マルチバイト文字の幅が正の値でない")
        assertTrue(maxY - minY > 0, "マルチバイト文字の高さが正の値でない")
        
        // 中央揃えのテスト
        val centerX = (minX + maxX) / 2
        val centerY = (minY + maxY) / 2
        assertFloatEquals(50f, centerX, 0.01f, "マルチバイト文字の中央揃えX位置が正しくない")
        assertFloatEquals(75f, centerY, 0.01f, "マルチバイト文字の中央揃えY位置が正しくない")
    }
    
    /**
     * フロート値の精密比較ヘルパー
     */
    private fun assertFloatEquals(expected: Float, actual: Float, delta: Float, message: String) {
        assertTrue(abs(expected - actual) <= delta, 
            "$message - Expected: $expected, Actual: $actual, Delta: ${abs(expected - actual)}")
    }
}
