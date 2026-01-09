package com.jpaver.trianglelist.test

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpaver.trianglelist.adapter.TextRenderer
import com.jpaver.trianglelist.dxf.DxfText

/**
 * テキストジオメトリテスト用ウィジェット
 * 様々なアライメント、回転、スケールでテキストの配置精度をテストする
 */
@Composable
fun TextGeometryTestWidget() {
    var selectedTestCase by remember { mutableStateOf(0) }
    var showDebug by remember { mutableStateOf(true) }
    var scale by remember { mutableStateOf(1f) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // コントロールパネル
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "テキストジオメトリテスト",
                    style = MaterialTheme.typography.h5
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // テストケース選択
                Text("テストケース:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    testCases.forEachIndexed { index, testCase ->
                        Button(
                            onClick = { selectedTestCase = index },
                            colors = if (selectedTestCase == index) {
                                ButtonDefaults.buttonColors(backgroundColor = Color.Blue)
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Text(testCase.name, fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // オプション
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = showDebug,
                            onCheckedChange = { showDebug = it }
                        )
                        Text("デバッグ表示")
                    }
                    
                    Column {
                        Text("スケール: ${String.format("%.1f", scale)}")
                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            valueRange = 0.5f..3.0f,
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // テスト結果表示
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val currentTestCase = testCases[selectedTestCase]
                Text(
                    text = "テストケース: ${currentTestCase.name}",
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = currentTestCase.description,
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // テキスト描画キャンバス
                TestCanvas(
                    testCase = currentTestCase,
                    scale = scale,
                    debugMode = showDebug,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * テスト用キャンバス
 */
@Composable
private fun TestCanvas(
    testCase: TextGeometryTestCase,
    scale: Float,
    debugMode: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textRenderer = remember { TextRenderer() }
    
    Canvas(modifier = modifier) {
        // グリッド線を描画
        drawGrid()
        
        // 中心十字線を描画
        val centerX = size.width / 2
        val centerY = size.height / 2
        drawLine(
            color = Color.Gray,
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = 1f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1f
        )
        
        // テストケースのテキストを描画
        testCase.textElements.forEach { textElement ->
            val adjustedText = textElement.copy(
                x = textElement.x + centerX.toDouble(),
                y = textElement.y + centerY.toDouble()
            )
            
            textRenderer.drawText(
                drawScope = this,
                text = adjustedText,
                scale = scale,
                textMeasurer = textMeasurer,
                debugMode = debugMode
            )
        }
    }
}

/**
 * グリッド線を描画
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid() {
    val gridSpacing = 50f
    val color = Color.LightGray
    
    // 垂直線
    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 0.5f
        )
        x += gridSpacing
    }
    
    // 水平線
    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 0.5f
        )
        y += gridSpacing
    }
}

/**
 * テストケースデータクラス
 */
data class TextGeometryTestCase(
    val name: String,
    val description: String,
    val textElements: List<DxfText>
)

/**
 * 事前定義されたテストケース
 */
private val testCases = listOf(
    TextGeometryTestCase(
        name = "基本アライメント",
        description = "9点アライメントの基本テスト（左上、中央上、右上、左中央、中央、右中央、左下、中央下、右下）",
        textElements = listOf(
            // 上段
            DxfText(x = -100.0, y = -50.0, text = "左上", height = 16.0, alignH = 0, alignV = 3),
            DxfText(x = 0.0, y = -50.0, text = "中央上", height = 16.0, alignH = 1, alignV = 3),
            DxfText(x = 100.0, y = -50.0, text = "右上", height = 16.0, alignH = 2, alignV = 3),
            
            // 中段
            DxfText(x = -100.0, y = 0.0, text = "左中央", height = 16.0, alignH = 0, alignV = 2),
            DxfText(x = 0.0, y = 0.0, text = "中央", height = 16.0, alignH = 1, alignV = 2),
            DxfText(x = 100.0, y = 0.0, text = "右中央", height = 16.0, alignH = 2, alignV = 2),
            
            // 下段
            DxfText(x = -100.0, y = 50.0, text = "左下", height = 16.0, alignH = 0, alignV = 1),
            DxfText(x = 0.0, y = 50.0, text = "中央下", height = 16.0, alignH = 1, alignV = 1),
            DxfText(x = 100.0, y = 50.0, text = "右下", height = 16.0, alignH = 2, alignV = 1)
        )
    ),
    
    TextGeometryTestCase(
        name = "異なるサイズ",
        description = "様々なフォントサイズでのアライメント精度テスト",
        textElements = listOf(
            DxfText(x = 0.0, y = -60.0, text = "8pt", height = 8.0, alignH = 1, alignV = 2),
            DxfText(x = 0.0, y = -30.0, text = "12pt", height = 12.0, alignH = 1, alignV = 2),
            DxfText(x = 0.0, y = 0.0, text = "16pt", height = 16.0, alignH = 1, alignV = 2),
            DxfText(x = 0.0, y = 30.0, text = "24pt", height = 24.0, alignH = 1, alignV = 2),
            DxfText(x = 0.0, y = 70.0, text = "32pt", height = 32.0, alignH = 1, alignV = 2)
        )
    ),
    
    TextGeometryTestCase(
        name = "回転テスト",
        description = "回転したテキストのアライメント精度テスト",
        textElements = listOf(
            DxfText(x = 0.0, y = -50.0, text = "0°", height = 16.0, rotation = 0.0, alignH = 1, alignV = 2),
            DxfText(x = 50.0, y = 0.0, text = "45°", height = 16.0, rotation = 45.0, alignH = 1, alignV = 2),
            DxfText(x = 0.0, y = 50.0, text = "90°", height = 16.0, rotation = 90.0, alignH = 1, alignV = 2),
            DxfText(x = -50.0, y = 0.0, text = "135°", height = 16.0, rotation = 135.0, alignH = 1, alignV = 2)
        )
    ),
    
    TextGeometryTestCase(
        name = "ベースライン",
        description = "ベースライン（alignV=0）の精度テスト",
        textElements = listOf(
            DxfText(x = -100.0, y = 0.0, text = "Left", height = 16.0, alignH = 0, alignV = 0),
            DxfText(x = 0.0, y = 0.0, text = "Center", height = 16.0, alignH = 1, alignV = 0),
            DxfText(x = 100.0, y = 0.0, text = "Right", height = 16.0, alignH = 2, alignV = 0),
            // 比較用の水平線
            DxfText(x = -150.0, y = 0.0, text = "────────────────────────────", height = 1.0, alignH = 0, alignV = 0, color = 1)
        )
    ),
    
    TextGeometryTestCase(
        name = "長いテキスト",
        description = "長いテキストでのアライメント精度テスト",
        textElements = listOf(
            DxfText(x = 0.0, y = -30.0, text = "Very Long Text String for Testing", height = 16.0, alignH = 0, alignV = 2),
            DxfText(x = 0.0, y = 0.0, text = "Very Long Text String for Testing", height = 16.0, alignH = 1, alignV = 2),
            DxfText(x = 0.0, y = 30.0, text = "Very Long Text String for Testing", height = 16.0, alignH = 2, alignV = 2)
        )
    )
)
