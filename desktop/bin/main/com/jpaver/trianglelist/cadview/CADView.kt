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

sealed class ColoredShape {
    data class Line(val x1: Double, val y1: Double, val x2: Double, val y2: Double, val color: Color) : ColoredShape()
    data class Circle(val centerX: Double, val centerY: Double, val radius: Double, val color: Color) : ColoredShape()
    data class Polyline(val vertices: List<Pair<Double, Double>>, val isClosed: Boolean, val color: Color) : ColoredShape()
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CADView(
    shapes: List<ColoredShape>,
    header: DxfHeader? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        LaunchedEffect(shapes, header, maxWidth, maxHeight) {
            if (shapes.isEmpty()) return@LaunchedEffect
            
            val canvasW = with(density) { maxWidth.toPx() }
            val canvasH = with(density) { maxHeight.toPx() }
            
            println("=== View Size Debug ===")
            println("maxWidth: $maxWidth, maxHeight: $maxHeight")
            println("canvasW: $canvasW, canvasH: $canvasH")
            println("density: ${density.density}")
            println("======================")
            
            // ヘッダーの図面範囲を優先的に使用
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
                val allPoints = shapes.flatMap { shape ->
                    when (shape) {
                        is ColoredShape.Line -> listOf(
                            Offset(shape.x1.toFloat(), shape.y1.toFloat()),
                            Offset(shape.x2.toFloat(), shape.y2.toFloat())
                        )
                        is ColoredShape.Circle -> listOf(
                            Offset((shape.centerX - shape.radius).toFloat(), (shape.centerY - shape.radius).toFloat()),
                            Offset((shape.centerX + shape.radius).toFloat(), (shape.centerY + shape.radius).toFloat())
                        )
                        is ColoredShape.Polyline -> shape.vertices.map { (x, y) ->
                            Offset(x.toFloat(), y.toFloat())
                        }
                    }
                }
                println("Using calculated drawing extents from entities")
                listOf(
                    allPoints.minOf { it.x },
                    allPoints.maxOf { it.x },
                    allPoints.minOf { it.y },
                    allPoints.maxOf { it.y }
                )
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
            drawContext.transform.translate(offset.x, offset.y + height * scale)
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

            shapes.forEach { shape ->
                when (shape) {
                    is ColoredShape.Line -> {
                        drawLine(
                            color = shape.color,
                            start = Offset(shape.x1.toFloat(), shape.y1.toFloat()),
                            end = Offset(shape.x2.toFloat(), shape.y2.toFloat()),
                            strokeWidth = 1f / scale
                        )
                    }
                    is ColoredShape.Circle -> {
                        drawCircle(
                            color = shape.color,
                            radius = shape.radius.toFloat(),
                            center = Offset(shape.centerX.toFloat(), shape.centerY.toFloat()),
                            style = Stroke(width = 1f / scale)
                        )
                    }
                    is ColoredShape.Polyline -> {
                        if (shape.vertices.size >= 2) {
                            val path = androidx.compose.ui.graphics.Path()
                            val firstVertex = shape.vertices.first()
                            path.moveTo(firstVertex.first.toFloat(), firstVertex.second.toFloat())
                            
                            shape.vertices.drop(1).forEach { vertex ->
                                path.lineTo(vertex.first.toFloat(), vertex.second.toFloat())
                            }
                            
                            if (shape.isClosed && shape.vertices.size > 2) {
                                path.close()
                            }
                            
                            drawPath(
                                path = path,
                                color = shape.color,
                                style = Stroke(width = 1f / scale)
                            )
                        }
                    }
                }
            }
            
            drawContext.canvas.restore()
        }
    }
}

fun aciToColor(aciColor: Int): Color {
    return when (aciColor) {
        0 -> Color.Black // ByBlock - 通常は親エンティティの色、ここでは黒
        1 -> Color.Red
        2 -> Color.Yellow
        3 -> Color.Green
        4 -> Color.Cyan
        5 -> Color.Blue
        6 -> Color.Magenta
        7 -> Color.Black // 白背景なので黒で表示（背景色の反転）
        8 -> Color.Gray
        9 -> Color.LightGray
        10 -> Color.Red
        11 -> Color(0xFFFF7F7F) // Light Red
        12 -> Color(0xFFFFFF7F) // Light Yellow
        13 -> Color(0xFF7FFF7F) // Light Green
        14 -> Color(0xFF7FFFFF) // Light Cyan
        15 -> Color(0xFF7F7FFF) // Light Blue
        16 -> Color(0xFFFF7FFF) // Light Magenta
        else -> Color.Black // 未定義色は黒
    }
} 