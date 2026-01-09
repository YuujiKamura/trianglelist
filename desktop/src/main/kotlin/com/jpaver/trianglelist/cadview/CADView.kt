package com.jpaver.trianglelist.cadview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import com.jpaver.trianglelist.dxf.DxfParseResult
import com.jpaver.trianglelist.adapter.CADViewRenderer
import androidx.compose.ui.graphics.Matrix

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CADView(
    parseResult: DxfParseResult,
    modifier: Modifier = Modifier,
    debugMode: Boolean = false,
    initialScale: Float? = null,
    initialOffset: Offset? = null,
    onViewStateChanged: ((Float, Offset) -> Unit)? = null
) {
    var scale by remember { mutableStateOf(initialScale ?: 1f) }
    var offset by remember { mutableStateOf(initialOffset ?: Offset.Zero) }
    var isInitialized by remember { mutableStateOf(initialScale != null && initialOffset != null) }
    val textMeasurer = rememberTextMeasurer()
    val renderer = remember { CADViewRenderer() }

    // ビューステートが変更されたら通知
    LaunchedEffect(scale, offset) {
        if (isInitialized) {
            onViewStateChanged?.invoke(scale, offset)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        // parseResultが変わっても、初回のみスケール・オフセットを計算（ホットリロード時は保持）
        LaunchedEffect(parseResult, maxWidth, maxHeight) {
            if (isInitialized) return@LaunchedEffect
            if (parseResult.lines.isEmpty() && parseResult.circles.isEmpty() &&
                parseResult.arcs.isEmpty() && parseResult.lwPolylines.isEmpty() && parseResult.texts.isEmpty()) return@LaunchedEffect

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
            offset = Offset(
                screenCenterX - drawingCenterX * newScale,
                screenCenterY - drawingCenterY * newScale
            )

            println("Drawing bounds: ($minX, $minY) to ($maxX, $maxY)")
            println("Drawing size: ${width} x ${height}")
            println("Drawing center: ($drawingCenterX, $drawingCenterY)")
            println("Screen center: ($screenCenterX, $screenCenterY)")
            println("Scale: $newScale, Offset: $offset")

            isInitialized = true
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
                    val zoomFactor = if (it.changes.first().scrollDelta.y > 0) 0.9f else 1.1f
                    val newScale = (scale * zoomFactor).coerceIn(0.0001f, 100f)

                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    val worldX = (centerX - offset.x) / scale
                    val worldY = (centerY - offset.y) / scale

                    val newOffset = Offset(
                        centerX - worldX * newScale,
                        centerY - worldY * newScale
                    )

                    println("=== Zoom Debug ===")
                    println("size: ${size.width} x ${size.height}")
                    println("center: ($centerX, $centerY)")
                    println("offset: $offset → $newOffset")
                    println("scale: $scale → $newScale")
                    println("worldPivot: ($worldX, $worldY)")
                    println("==================")

                    offset = newOffset
                    scale = newScale
                }
        ) {
            drawContext.canvas.save()
            // 単一の変換行列: screenPos = worldPos * scale + offset
            // translate → scale の順で適用（行列は Translate * Scale となる）
            val viewMatrix = Matrix().apply {
                translate(offset.x, offset.y)
                scale(scale, scale)
            }
            drawContext.transform.transform(viewMatrix)

            // 1000mm固定グリッド（ワールド座標）
            val gridSpacing = 1000f  // 1000mm = 1m
            val gridColor = Color.LightGray.copy(alpha = 0.3f)
            // 可視範囲をワールド座標で計算
            val worldMinX = -offset.x / scale
            val worldMinY = -offset.y / scale
            val worldMaxX = (size.width - offset.x) / scale
            val worldMaxY = (size.height - offset.y) / scale
            // グリッド線の範囲
            val startX = (worldMinX / gridSpacing).toInt() * gridSpacing
            val startY = (worldMinY / gridSpacing).toInt() * gridSpacing
            var gx = startX
            while (gx <= worldMaxX) {
                drawLine(gridColor, Offset(gx, worldMinY), Offset(gx, worldMaxY), 1f / scale)
                gx += gridSpacing
            }
            var gy = startY
            while (gy <= worldMaxY) {
                drawLine(gridColor, Offset(worldMinX, gy), Offset(worldMaxX, gy), 1f / scale)
                gy += gridSpacing
            }

            val crossSize = 100f / scale
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

            renderer.drawAllEntities(this, parseResult, scale, textMeasurer, debugMode)

            drawContext.canvas.restore()
        }

        if (debugMode) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize()
            ) {
                androidx.compose.material.Text(
                    text = "デバッグモード ON\nテキスト数: " + parseResult.texts.size + "\nスケール: " + "%.2f".format(scale),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}
