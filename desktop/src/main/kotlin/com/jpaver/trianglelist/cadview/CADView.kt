package com.jpaver.trianglelist.cadview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.min
import com.jpaver.trianglelist.parser.DxfHeader
import com.jpaver.trianglelist.parser.DxfParseResult
import com.jpaver.trianglelist.common.DxfColor

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CADView(
    parseResult: DxfParseResult,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        LaunchedEffect(parseResult, maxWidth, maxHeight) {
            if (parseResult.lines.isEmpty() && parseResult.circles.isEmpty() && parseResult.lwPolylines.isEmpty()) return@LaunchedEffect
            
            val canvasW = with(density) { maxWidth.toPx() }
            val canvasH = with(density) { maxHeight.toPx() }
            
            println("=== View Size Debug ===")
            println("maxWidth: $maxWidth, maxHeight: $maxHeight")
            println("canvasW: $canvasW, canvasH: $canvasH")
            println("density: ${density.density}")
            println("======================")
            
            // ヘッダーの図面範囲を優先的に使用
            val header = parseResult.header
            val (minX, maxX, minY, maxY) = if (header != null && 
                header.extMax.first > header.extMin.first && 
                header.extMax.second > header.extMin.second) {
                // ヘッダーの図面範囲を使用
                println("Using header drawing extents: ${header.extMin} to ${header.extMax}")
                listOf(
                    header.extMin.first.toFloat(),
                    header.extMax.first.toFloat(), 
                    header.extMin.second.toFloat(),
                    header.extMax.second.toFloat()
                )
            } else {
                // エンティティから図面範囲を計算
                val allPoints = mutableListOf<Offset>()
                
                // 線分の点を追加
                parseResult.lines.forEach { line ->
                    allPoints.add(Offset(line.x1.toFloat(), line.y1.toFloat()))
                    allPoints.add(Offset(line.x2.toFloat(), line.y2.toFloat()))
                }
                
                // 円の境界を追加
                parseResult.circles.forEach { circle ->
                    allPoints.add(Offset((circle.centerX - circle.radius).toFloat(), (circle.centerY - circle.radius).toFloat()))
                    allPoints.add(Offset((circle.centerX + circle.radius).toFloat(), (circle.centerY + circle.radius).toFloat()))
                }
                
                // ポリラインの頂点を追加
                parseResult.lwPolylines.forEach { polyline ->
                    polyline.vertices.forEach { (x, y) ->
                        allPoints.add(Offset(x.toFloat(), y.toFloat()))
                    }
                }
                
                println("Using calculated drawing extents from entities")
                if (allPoints.isEmpty()) {
                    listOf(0f, 100f, 0f, 100f)
                } else {
                    listOf(
                        allPoints.minOf { it.x },
                        allPoints.maxOf { it.x },
                        allPoints.minOf { it.y },
                        allPoints.maxOf { it.y }
                    )
                }
            }

            val width = maxX - minX
            val height = maxY - minY
            if (width <= 0f || height <= 0f) return@LaunchedEffect

            // 図面のサイズに合わせてスケールを計算（余裕を持たせる）
            val newScale = min(canvasW / width, canvasH / height) * 0.9f
            
            // 図面の中心座標を計算
            val drawingCenterX = (minX + maxX) / 2f
            val drawingCenterY = (minY + maxY) / 2f
            
            // 画面の中心座標を計算
            val screenCenterX = canvasW / 2f
            val screenCenterY = canvasH / 2f

            scale = newScale
            // オフセットを完全に0に固定
            offset = Offset(0f, 0f)
            
            println("Drawing bounds: ($minX, $minY) to ($maxX, $maxY)")
            println("Drawing size: ${width} x ${height}")
            println("Drawing center: ($drawingCenterX, $drawingCenterY)")
            println("Screen center: ($screenCenterX, $screenCenterY)")
            println("Scale: $newScale, Offset: $offset (FORCE ZERO)")
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
                    }
                }
                .onPointerEvent(PointerEventType.Scroll) {
                    val zoomFactor = if (it.changes.first().scrollDelta.y > 0) 0.8f else 1.25f
                    val newScale = (scale * zoomFactor).coerceIn(0.01f, 100f)
                    val pos = it.changes.first().position
                    offset = Offset(
                        offset.x - pos.x * (newScale / scale - 1f),
                        offset.y - pos.y * (newScale / scale - 1f)
                    )
                    scale = newScale
                }
        ) {
            drawContext.canvas.save()
            // Y軸反転＋図面の高さ分だけY方向に平行移動
            drawContext.transform.translate(offset.x, offset.y + size.height * scale)
            drawContext.transform.scale(scale, -scale) // Y flip

            // 描画原点(0,0)にクロスラインを描画
            val crossSize = 100f / scale  // スケールに応じたクロスサイズ
            drawLine(
                color = Color.Red,
                start = Offset(-crossSize, 0f),
                end = Offset(crossSize, 0f),
                strokeWidth = 2f / scale
            )
            drawLine(
                color = Color.Red,
                start = Offset(0f, -crossSize),
                end = Offset(0f, crossSize),
                strokeWidth = 2f / scale
            )

            // 線分を描画
            parseResult.lines.forEach { line ->
                val color = ColorConverter.aciToColor(line.color)
                drawLine(
                    color = color,
                    start = Offset(line.x1.toFloat(), line.y1.toFloat()),
                    end = Offset(line.x2.toFloat(), line.y2.toFloat()),
                    strokeWidth = 1f / scale
                )
            }
            
            // 円を描画
            parseResult.circles.forEach { circle ->
                val color = ColorConverter.aciToColor(circle.color)
                drawCircle(
                    color = color,
                    radius = circle.radius.toFloat(),
                    center = Offset(circle.centerX.toFloat(), circle.centerY.toFloat()),
                    style = Stroke(width = 1f / scale)
                )
            }
            
            // ポリラインを描画
            parseResult.lwPolylines.forEach { polyline ->
                if (polyline.vertices.size >= 2) {
                    val color = ColorConverter.aciToColor(polyline.color)
                    val path = androidx.compose.ui.graphics.Path()
                    val firstVertex = polyline.vertices.first()
                    path.moveTo(firstVertex.first.toFloat(), firstVertex.second.toFloat())
                    
                    polyline.vertices.drop(1).forEach { vertex ->
                        path.lineTo(vertex.first.toFloat(), vertex.second.toFloat())
                    }
                    
                    if (polyline.isClosed && polyline.vertices.size > 2) {
                        path.close()
                    }
                    
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 1f / scale)
                    )
                }
            }
            
            drawContext.canvas.restore()
        }
    }
}

 