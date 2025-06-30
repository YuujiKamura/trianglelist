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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.min
import com.jpaver.trianglelist.dxf.DxfHeader
import com.jpaver.trianglelist.dxf.DxfParseResult
import com.jpaver.trianglelist.dxf.DxfColor
import com.jpaver.trianglelist.adapter.CADViewRenderer

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CADView(
    parseResult: DxfParseResult,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val textMeasurer = rememberTextMeasurer()
    val renderer = remember { CADViewRenderer() }
    
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        LaunchedEffect(parseResult, maxWidth, maxHeight) {
            if (parseResult.lines.isEmpty() && parseResult.circles.isEmpty() && 
                parseResult.lwPolylines.isEmpty() && parseResult.texts.isEmpty()) return@LaunchedEffect
            
            val canvasW = with(density) { maxWidth.toPx() }
            val canvasH = with(density) { maxHeight.toPx() }
            
            println("=== View Size Debug ===")
            println("maxWidth: $maxWidth, maxHeight: $maxHeight")
            println("canvasW: $canvasW, canvasH: $canvasH")
            println("density: ${density.density}")
            println("======================")
            
            // 新しい境界計算クラスを使用
            val (minX, maxX, minY, maxY) = renderer.calculateDrawingBounds(parseResult, textMeasurer, scale)

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

            // 新しい描画統合クラスを使用してすべてのエンティティを描画
            renderer.drawAllEntities(this, parseResult, scale, textMeasurer)
            
            drawContext.canvas.restore()
        }
    }
}

 