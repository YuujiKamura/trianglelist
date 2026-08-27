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
    showLabelBoxes: Boolean = false,
    initialScale: Float? = null,
    initialOffset: Offset? = null,
    onViewStateChanged: ((Float, Offset) -> Unit)? = null
) {
    var scale by remember { mutableStateOf(initialScale ?: 1f) }
    var offset by remember { mutableStateOf(initialOffset ?: Offset.Zero) }
    var isInitialized by remember { mutableStateOf(initialScale != null && initialOffset != null) }
    val textMeasurer = rememberTextMeasurer()
    val renderer = remember { CADViewRenderer() }
    val overlayDensity = LocalDensity.current
    // LabelBox overlay (rev5 確定): 判定 (DxfOverlapAnalyzer.analyze) が見ている box と
    // 同一経路 textBoxes() で生成する。実測メトリクス (MS Gothic + キャップハイト=height、
    // 描画と同一) なので箱はグリフに構造的に密着する
    val labelMetrics = remember(textMeasurer, overlayDensity) {
        com.jpaver.trianglelist.adapter.MeasuredLabelMetrics(textMeasurer, overlayDensity)
    }
    val labelBoxes = remember(parseResult, labelMetrics) {
        com.jpaver.trianglelist.label.DxfOverlapAnalyzer.textBoxes(parseResult, metrics = labelMetrics)
    }
    // 枠の色分け: 「その寸法テキストが何と衝突しているか」(common OverlapReport が決める、
    // 判定と同じ analyze 経路)。box を描く経路と判定する経路が同一なので、
    // 画面の色 = 判定結果そのもの (2026-08-27 user 指示「正しく色分けした枠」)
    val collisionKinds = remember(parseResult, labelMetrics) {
        com.jpaver.trianglelist.label.DxfOverlapAnalyzer.analyze(parseResult, metrics = labelMetrics)
            .collisionKindByText
    }
    // overlay の Path は model 座標そのもの (canvas 側の transform が拡大/移動を担う) なので
    // 1 度作れば pan/zoom で作り直す必要がない。毎フレーム 200 個の Path を組むと
    // パンが目に見えて重くなる (2026-08-27 user「パン操作とか結構重いな」)。
    // 衝突ありを後ろに並べておく = 灰/青が衝突色の上に乗るのを防ぐ (描画順)
    val overlayPaths = remember(labelBoxes, collisionKinds) {
        labelBoxes
            .sortedBy { (id, _) -> if (collisionKinds[id] == null) 0 else 1 }
            .map { (id, box) ->
                val corners = box.corners()
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(corners[0].x.toFloat(), (-corners[0].y).toFloat())
                    for (i in 1 until corners.size) lineTo(corners[i].x.toFloat(), (-corners[i].y).toFloat())
                    close()
                }
                Triple(path, collisionColor(collisionKinds[id]), collisionKinds[id] != null)
            }
    }
    // 較正検証データ: renderer の実描画 layout 箱と判定 box の数値比較 (boxes on で 1 回出力)
    val overlayTextRenderer = remember { com.jpaver.trianglelist.adapter.TextRenderer() }
    val flippedTexts = remember(parseResult) {
        com.jpaver.trianglelist.util.CanvasUtil.flipYAxis(parseResult).texts
    }
    LaunchedEffect(showLabelBoxes, parseResult) {
        if (!showLabelBoxes) return@LaunchedEffect
        println("[boxes] density=${overlayDensity.density} fontScale=${overlayDensity.fontScale} capRatio=${com.jpaver.trianglelist.adapter.TextRenderer.capHeightRatio} msGothic=${com.jpaver.trianglelist.adapter.TextRenderer.msGothicTypeface != null}")
        labelBoxes.forEach { (id, box) ->
            // id は "text:<元 texts の index>:<内容>"。labelBoxes は番号サークル・空文字を
            // 落とした後の列なので、順番で引くと別のテキストと突き合わせてしまう
            // (ratioW=0.27 のような無意味な数字が出ていた原因)。index を id から取る
            val srcIndex = id.split(':').getOrNull(1)?.toIntOrNull() ?: return@forEach
            val t = flippedTexts.getOrNull(srcIndex) ?: return@forEach
            val b = overlayTextRenderer.calculateTextBounds(t, scale, textMeasurer, overlayDensity)
            val renderedW = b[1] - b[0]
            val renderedH = b[3] - b[2]
            val dxCenter = (b[0] + b[1]) / 2f - box.center.x
            val dyCenter = (b[2] + b[3]) / 2f - (-box.center.y)
            println(
                "[boxes] $id rot=%.0f boxW=%.1f rendW=%.1f ratioW=%.3f boxH=%.1f rendH=%.1f dx=%.1f dy=%.1f".format(
                    box.rotationDeg, box.widthMm, renderedW, renderedW / box.widthMm,
                    box.heightMm, renderedH, dxCenter, dyCenter
                )
            )
        }
    }

    // CP (zoom / pan / view) からの外部指定を実際のビューへ反映する。
    // initialScale / initialOffset は remember の初期値としてしか読まれていなかったため、
    // 「ok scale=...」と返るのに画面が動かない (= CP 越しの目視検証ができない) 状態だった。
    // 値が変わった時だけ追従させる ── 内部のドラッグ/ホイール操作は上書きしない
    LaunchedEffect(initialScale, initialOffset) {
        initialScale?.let { scale = it }
        initialOffset?.let { offset = it }
        if (initialScale != null || initialOffset != null) isInitialized = true
    }

    // ビューステートが変更されたら通知
    LaunchedEffect(scale, offset) {
        if (isInitialized) {
            onViewStateChanged?.invoke(scale, offset)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        // 全体フィット計算。初回composition と CP の「fit」(initial* = null) の両方から呼ぶ ──
        // 以前は初回 LaunchedEffect の中に埋まっていて、CP fit が「ok」を返すだけで
        // 画面が動かなかった (isInitialized が立ったままなので再計算されない)
        fun fitToView() {
            if (parseResult.lines.isEmpty() && parseResult.circles.isEmpty() &&
                parseResult.arcs.isEmpty() && parseResult.lwPolylines.isEmpty() && parseResult.texts.isEmpty()) return

            val canvasW = with(density) { maxWidth.toPx() }
            val canvasH = with(density) { maxHeight.toPx() }

            println("=== View Size Debug ===")
            println("maxWidth: $maxWidth, maxHeight: $maxHeight")
            println("canvasW: $canvasW, canvasH: $canvasH")
            println("density: ${density.density}")
            println("======================")

            // 新しい境界計算クラスを使用
            val (minX, maxX, minY, maxY) = renderer.calculateDrawingBounds(parseResult, textMeasurer, scale, density)

            val width = maxX - minX
            val height = maxY - minY
            if (width <= 0f || height <= 0f) return

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

        // 初回のみ (parseResult が変わってもホットリロード時は保持)
        LaunchedEffect(parseResult, maxWidth, maxHeight) {
            if (isInitialized) return@LaunchedEffect
            fitToView()
        }
        // CP 「fit」= initialScale/initialOffset を両方 null にする合図 → 全体フィットへ戻す
        LaunchedEffect(initialScale, initialOffset, maxWidth, maxHeight) {
            if (initialScale == null && initialOffset == null) fitToView()
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

            // LabelBox overlay: 既存描画の上に青枠で重ねる。canvas world は Y 反転済み
            // DXF 座標 (CanvasUtil.flipYAxis) なので、DXF 座標の corners を -y で写す
            if (showLabelBoxes) {
                for ((path, color, colliding) in overlayPaths) {
                    // 衝突ありは薄い塗りも敷く。線だけだと引きの倍率で枠が消えて
                    // 「衝突しているのに気づけない」(実データ 8.25 の目視で確認)
                    if (colliding) drawPath(path, color = color.copy(alpha = 0.22f))
                    drawPath(
                        path,
                        color = color,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = (if (colliding) 2.5f else 1.5f) / scale
                        )
                    )
                }
            }

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

/**
 * 衝突種別 → 枠の色。common の ObstacleKind (判定側の語彙) と 1:1 で対応させる ──
 * 画面の色が判定結果の翻訳であって、独自解釈を挟まないようにするため。
 *   赤 = 寸法値どうしが重なる (数字が読めない、最悪)
 *   紫 = 番号サークルに当たる
 *   青 = 衝突なし (判定 box の可視化のみ、従来の色)
 * 辺 (EDGE) はそもそも色分け対象に入らない (OverlapReport.collisionKindByText 参照 ──
 * 縦アライメントのパディングのせいで寸法値はほぼ必ず自分の辺に当たるため)。
 */
private fun collisionColor(kind: com.jpaver.trianglelist.label.ObstacleKind?): Color = when (kind) {
    com.jpaver.trianglelist.label.ObstacleKind.LABEL -> Color(0xFFE53935)
    com.jpaver.trianglelist.label.ObstacleKind.CIRCLE -> Color(0xFF8E24AA)
    else -> Color.Blue
}
