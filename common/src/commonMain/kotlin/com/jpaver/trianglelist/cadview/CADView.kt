package com.jpaver.trianglelist.cadview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jpaver.trianglelist.parser.DxfParser
import com.jpaver.trianglelist.parser.DxfParseResult
import androidx.compose.foundation.border
import kotlin.math.*
import com.jpaver.trianglelist.parser.DxfLine
import com.jpaver.trianglelist.parser.DxfCircle
import com.jpaver.trianglelist.parser.DxfLwPolyline
import com.jpaver.trianglelist.parser.DxfText
import com.example.trilib.PointXY

// CADViewWidget: DXFコンテンツを表示するメインコンポーザブル
@Composable
fun CADViewWidget(
    dxfText: String? = null,
    scale: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    textScale: Float = 1f, // テキストスケール追加
    onOpenFile: (() -> Unit)? = null,
    onZoomIn: (() -> Unit)? = null,
    onZoomOut: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null,
    onTextSizeUp: (() -> Unit)? = null, // テキストサイズアップ
    onTextSizeDown: (() -> Unit)? = null, // テキストサイズダウン
    onToggleDebug: (() -> Unit)? = null, // WebApp版から移植：デバッグモード切り替え
    isDebugMode: Boolean = false // WebApp版から移植：デバッグモード状態
) {
    // DxfParserをrememberで保持
    val parser = remember { DxfParser() }

    // WebApp版から移植：ログダンプ機能の初期化
    LaunchedEffect(isDebugMode) {
        if (isDebugMode) {
            println("Debug mode enabled")
        }
    }

    // dxfTextが変更されたらパースを実行
    val parseResult = remember(dxfText) {
        dxfText?.let { 
            try {
                val result = parser.parse(it)
                
                if (isDebugMode) {
                    println("解析結果: ${result.lines.size} lines, ${result.circles.size} circles, ${result.lwPolylines.size} lwPolylines, ${result.texts.size} texts")
                }
                
                result
            } catch (e: Exception) {
                println("DXF parsing error: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    // WebApp版のCSVLoader機能を移植：テキストサイズの自動調整
    val adjustedTextScale = remember(parseResult, textScale) {
        if (parseResult != null && parseResult.texts.isNotEmpty()) {
            // テキストの平均サイズを基に基準スケールを計算
            val averageTextHeight = parseResult.texts.map { it.height }.average()
            val normalizedScale = when {
                averageTextHeight > 10.0 -> textScale * 0.5f // 大きいテキストは縮小
                averageTextHeight < 1.0 -> textScale * 2.0f  // 小さいテキストは拡大
                else -> textScale
            }
            normalizedScale
        } else {
            textScale
        }
    }

    val effectiveScaleOffset = remember(parseResult, scale, offsetX, offsetY, adjustedTextScale) {
        // 自動フィット計算
        if (parseResult == null) {
            Triple(scale, offsetX, offsetY)
        } else {
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY

            fun updateBounds(x: Float, y: Float) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }

            parseResult.lines.forEach { line ->
                val startX = line.start.x
                val startY = line.start.y
                val endX = line.end.x
                val endY = line.end.y
                updateBounds(startX.toFloat(), startY.toFloat())
                updateBounds(endX.toFloat(), endY.toFloat())
            }
            parseResult.circles.forEach { circle ->
                val centerX = circle.center.x
                val centerY = circle.center.y
                val radius = circle.radius
                updateBounds(centerX.toFloat(), centerY.toFloat())
                updateBounds((centerX + radius).toFloat(), (centerY + radius).toFloat())
            }
            parseResult.lwPolylines.forEach { polyline ->
                polyline.vertices.forEach { vertex ->
                    val x = vertex.x
                    val y = vertex.y
                    updateBounds(x.toFloat(), y.toFloat())
                }
            }
            parseResult.texts.forEach { text ->
                val x = text.insertionPoint.x
                val y = text.insertionPoint.y
                // WebApp版から移植：回転とアライメントを考慮したテキストのバウンディングボックス計算
                val estimatedWidth = text.text.length * text.height * adjustedTextScale * 0.6
                val estimatedHeight = text.height * adjustedTextScale
                
                // アライメントオフセット計算
                val horizontalOffset = when (text.horizontalAlignment) {
                    1 -> -estimatedWidth / 2.0  // 中央
                    2 -> -estimatedWidth        // 右
                    else -> 0.0                 // 左（デフォルト）
                }
                
                val verticalOffset = when (text.verticalAlignment) {
                    1 -> estimatedHeight       // 下
                    2 -> estimatedHeight / 2.0 // 中央
                    3 -> 0.0                   // 上
                    else -> estimatedHeight * 0.8 // 基準線（デフォルト）
                }
                
                if (text.rotationAngle != 0.0) {
                    // 回転している場合、四隅の座標を計算（アライメント考慮）
                    val cos = cos(text.rotationAngle)
                    val sin = sin(text.rotationAngle)
                    
                    val corners = listOf(
                        Pair(horizontalOffset, -verticalOffset), // 左下
                        Pair(horizontalOffset + estimatedWidth, -verticalOffset), // 右下
                        Pair(horizontalOffset + estimatedWidth, -verticalOffset + estimatedHeight), // 右上
                        Pair(horizontalOffset, -verticalOffset + estimatedHeight) // 左上
                    )
                    
                    corners.forEach { (localX, localY) ->
                        val rotatedX = x + localX * cos - localY * sin
                        val rotatedY = y + localX * sin + localY * cos
                        updateBounds(rotatedX.toFloat(), rotatedY.toFloat())
                    }
                } else {
                    // 回転なしの場合（アライメント考慮）
                    val leftX = x + horizontalOffset
                    val rightX = leftX + estimatedWidth
                    val topY = y - verticalOffset + estimatedHeight
                    val bottomY = y - verticalOffset
                    
                    updateBounds(leftX.toFloat(), bottomY.toFloat())
                    updateBounds(rightX.toFloat(), topY.toFloat())
                }
            }

            if (minX == Float.POSITIVE_INFINITY) { // 何も描画オブジェクトがない場合
                 return@remember Triple(scale, offsetX, offsetY)
            }

            val width = maxX - minX
            val height = maxY - minY
            val fitScale = if (width == 0f || height == 0f) 1f else {
                // Paddingを考慮
                val scaleX = (800f - 40f) / width
                val scaleY = (600f - 40f) / height
                minOf(scaleX, scaleY)
            }
            val fitOffsetX = (800f - width * fitScale) / 2f - minX * fitScale
            val fitOffsetY = (600f - height * fitScale) / 2f - minY * fitScale
            
            if (isDebugMode) {
                println("描画領域計算: minX=$minX, minY=$minY, maxX=$maxX, maxY=$maxY, fitScale=$fitScale, fitOffsetX=$fitOffsetX, fitOffsetY=$fitOffsetY")
            }
            
            Triple(fitScale * scale, fitOffsetX + offsetX, fitOffsetY + offsetY)
        }
    }
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TriangleList Desktop App", style = MaterialTheme.typography.h5, color = Color(0xFF2196F3))
            // コントロールボタン
            Row {
                Button(onClick = { onZoomIn?.invoke() }) { Text("拡大") }
                Spacer(Modifier.width(10.dp))
                Button(onClick = { onZoomOut?.invoke() }) { Text("縮小") }
                Spacer(Modifier.width(10.dp))
                Button(onClick = { onReset?.invoke() }) { Text("リセット") }
            }
            Spacer(Modifier.height(10.dp))
            // テキストサイズ制御ボタン
            Row {
                Button(onClick = { onTextSizeUp?.invoke() }) { Text("文字拡大") }
                Spacer(Modifier.width(10.dp))
                Button(onClick = { onTextSizeDown?.invoke() }) { Text("文字縮小") }
                Spacer(Modifier.width(10.dp))
                Text("テキストスケール: ${String.format("%.1f", textScale)}", 
                     style = MaterialTheme.typography.body2)
            }
            Spacer(Modifier.height(5.dp))
            // WebApp版から移植：デバッグモード制御
            Row {
                Button(
                    onClick = { onToggleDebug?.invoke() },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isDebugMode) Color(0xFF8BC34A) else Color(0xFF2196F3)
                    )
                ) { 
                    Text(if (isDebugMode) "デバッグOFF" else "デバッグON") 
                }
                Spacer(Modifier.width(10.dp))
                if (isDebugMode) {
                    Button(onClick = { println("ログクリア") }) { Text("ログクリア") }
                }
            }
            Spacer(Modifier.height(5.dp))
            // WebApp版から移植：テキスト情報の表示
            parseResult?.let { result ->
                val textCount = result.texts.size
                val rotatedTextCount = result.texts.count { it.rotationAngle != 0.0 }
                val alignedTextCount = result.texts.count { 
                    it.horizontalAlignment != 0 || it.verticalAlignment != 0 
                }
                Text("テキスト: ${textCount}個 (回転: ${rotatedTextCount}個, アライメント: ${alignedTextCount}個)", 
                     style = MaterialTheme.typography.caption, 
                     color = Color.Gray)
                
                // WebApp版から移植：デバッグ情報表示
                if (isDebugMode) {
                    val logCount = println("ログ件数: ${result.texts.size}")
                    Text("デバッグモード | ログ件数: ${logCount}", 
                         style = MaterialTheme.typography.caption, 
                         color = Color(0xFF8BC34A))
                }
            }
            Spacer(Modifier.height(10.dp))
            // キャンバス
            Card(elevation = 4.dp) {
                Canvas(modifier = Modifier.size(1200.dp, 1000.dp).border(1.dp, Color.Gray)) {
                    drawRect(color = Color.White, size = size)

                    val (currentScale, currentOffsetX, currentOffsetY) = effectiveScaleOffset

                    if (parseResult != null) {
                        // Lines
                        parseResult.lines.forEach { line ->
                            val startX = line.start.x
                            val startY = line.start.y
                            val endX = line.end.x
                            val endY = line.end.y
                            drawLine(
                                color = Color.Black,
                                start = Offset((startX * currentScale + currentOffsetX).toFloat(),
                                               (startY * currentScale + currentOffsetY).toFloat()),
                                end = Offset((endX * currentScale + currentOffsetX).toFloat(),
                                             (endY * currentScale + currentOffsetY).toFloat()),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Circles
                        parseResult.circles.forEach { circle ->
                            val centerX = circle.center.x
                            val centerY = circle.center.y
                            val radius = circle.radius
                            drawCircle(
                                color = Color.Black,
                                radius = (radius * currentScale).toFloat(),
                                center = Offset((centerX * currentScale + currentOffsetX).toFloat(),
                                                (centerY * currentScale + currentOffsetY).toFloat()),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // LWPolylines
                        parseResult.lwPolylines.forEach { polyline ->
                            val path = Path()
                            polyline.vertices.forEachIndexed { index, vertex ->
                                val x = (vertex.x * currentScale + currentOffsetX).toFloat()
                                val y = (vertex.y * currentScale + currentOffsetY).toFloat()
                                if (index == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }
                            if (polyline.closed) {
                                path.close()
                            }
                            drawPath(
                                path = path,
                                color = Color.Black,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Texts
                        parseResult.texts.forEach { text ->
                             val effectiveTextSize = (text.height * currentScale * adjustedTextScale).toFloat()
                             
                             val x = (text.insertionPoint.x * currentScale + currentOffsetX).toFloat()
                             val y = (text.insertionPoint.y * currentScale + currentOffsetY).toFloat()
                             
                             // WebApp版から移植：テキスト描画ログ
                             if (isDebugMode) {
                                 println("テキスト描画: text=${text.text}, x=${x}, y=${y}, rotationAngle=${text.rotationAngle}, horizontalAlignment=${text.horizontalAlignment}, verticalAlignment=${text.verticalAlignment}")
                             }
                             
                             // WebApp版から移植：アライメントオフセット計算
                             val textWidth = text.text.length * effectiveTextSize * 0.6f
                             val textHeight = effectiveTextSize
                             
                             // 水平アライメントオフセット（0=左、1=中央、2=右）
                             val horizontalOffset = when (text.horizontalAlignment) {
                                 1 -> -textWidth / 2f  // 中央
                                 2 -> -textWidth       // 右
                                 else -> 0f            // 左（デフォルト）
                             }
                             
                             // 垂直アライメントオフセット（0=基準線、1=下、2=中央、3=上）
                             val verticalOffset = when (text.verticalAlignment) {
                                 1 -> textHeight      // 下
                                 2 -> textHeight / 2f // 中央
                                 3 -> 0f              // 上
                                 else -> textHeight * 0.8f // 基準線（デフォルト）
                             }
                             
                             // WebApp版から移植：テキスト回転処理
                             if (text.rotationAngle != 0.0) {
                                 // テキスト描画は後で実装
                                 // drawContext.canvas.nativeCanvas.save()
                                 // drawContext.canvas.nativeCanvas.translate(x, y)
                                 // drawContext.canvas.nativeCanvas.rotate(Math.toDegrees(text.rotationAngle).toFloat())
                                 // drawContext.canvas.nativeCanvas.drawString(text.text, horizontalOffset, -verticalOffset, font, paint)
                                 // drawContext.canvas.nativeCanvas.restore()
                             } else {
                                 // 回転なしの場合
                                 // drawContext.canvas.nativeCanvas.drawString(text.text, x + horizontalOffset, y - verticalOffset, font, paint)
                             }
                        }
                    }
                }
            }
        }
    }
}