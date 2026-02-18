package com.jpaver.trianglelist.cadview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.jpaver.trianglelist.dxf.*
import com.jpaver.trianglelist.util.CanvasUtil
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.transform.Affine
import java.awt.BorderLayout
import javax.swing.JPanel
import kotlin.math.min

/**
 * C案: JavaFX Canvas (OpenJFX) ベースの DXF ビューワー
 *
 * OpenJFX の Canvas API を使い、Compose Desktop に SwingPanel + JFXPanel で埋め込む。
 * JavaFX の Prism レンダリングパイプラインによりハードウェア加速された
 * 高品質アンチエイリアス描画を提供する。
 *
 * 既存の DxfParser (DxfParseResult) をそのまま使用し、描画のみ JavaFX Canvas で行う。
 */

/** JavaFX runtime initialization guard */
private var javafxInitialized = false

private fun ensureJavaFXInitialized() {
    if (!javafxInitialized) {
        javafxInitialized = true
        // Creating a JFXPanel implicitly initializes the JavaFX toolkit
        JFXPanel()
    }
}

/** ACI (AutoCAD Color Index) -> JavaFX Color */
private fun aciToJfxColor(aci: Int): Color = when (aci) {
    0 -> Color.BLACK
    1 -> Color.RED
    2 -> Color.YELLOW
    3 -> Color.LIME
    4 -> Color.CYAN
    5 -> Color.BLUE
    6 -> Color.MAGENTA
    7 -> Color.WHITE
    8 -> Color.GRAY
    9 -> Color.LIGHTGRAY
    else -> {
        if (aci in 250..255) {
            val gray = (aci - 250) / 5.0
            Color.gray(gray.coerceIn(0.0, 1.0))
        } else {
            val hue = ((aci - 10) % 24) * 15.0
            val sat = if ((aci - 10) % 2 == 0) 1.0 else 0.5
            val bri = when (((aci - 10) / 2) % 5) {
                0 -> 1.0; 1 -> 0.8; 2 -> 0.6; 3 -> 0.5; else -> 0.3
            }
            Color.hsb(hue, sat, bri)
        }
    }
}

/**
 * Compose Desktop 用の JavaFX ベース DXF ビューワー Composable
 */
@Composable
fun JavaFXCadView(
    parseResult: DxfParseResult,
    modifier: Modifier = Modifier,
    debugMode: Boolean = false
) {
    val flippedData = remember(parseResult) { CanvasUtil.flipYAxis(parseResult) }

    SwingPanel(
        modifier = modifier.fillMaxSize(),
        factory = {
            ensureJavaFXInitialized()
            createViewerPanel(flippedData, debugMode)
        },
        update = { panel ->
            Platform.runLater {
                val jfxPanel = (panel.getComponent(0) as? JFXPanel) ?: return@runLater
                val newFlipped = CanvasUtil.flipYAxis(parseResult)
                setupScene(jfxPanel, newFlipped, debugMode)
            }
        }
    )
}

// ---- internal implementation ----

private fun createViewerPanel(data: DxfParseResult, debugMode: Boolean): JPanel {
    val panel = JPanel(BorderLayout())
    val jfxPanel = JFXPanel()
    panel.add(jfxPanel, BorderLayout.CENTER)
    Platform.runLater { setupScene(jfxPanel, data, debugMode) }
    return panel
}

/** Per-viewer mutable state, shared by callbacks within one scene. */
private class JfxViewState {
    var scale = 1.0
    var offsetX = 0.0
    var offsetY = 0.0
    var initialized = false
    var lastMouseX = 0.0
    var lastMouseY = 0.0
}

private fun setupScene(jfxPanel: JFXPanel, data: DxfParseResult, debugMode: Boolean) {
    val pane = Pane()
    pane.style = "-fx-background-color: #1E1E1E;"

    val canvas = Canvas()
    pane.children.add(canvas)
    canvas.widthProperty().bind(pane.widthProperty())
    canvas.heightProperty().bind(pane.heightProperty())

    val vs = JfxViewState()

    fun redraw() {
        val gc = canvas.graphicsContext2D
        val w = canvas.width
        val h = canvas.height
        if (w <= 0.0 || h <= 0.0) return

        // clear
        gc.save()
        gc.setTransform(Affine())
        gc.fill = Color.web("#1E1E1E")
        gc.fillRect(0.0, 0.0, w, h)
        gc.restore()

        // auto-fit on first draw
        if (!vs.initialized) {
            val bounds = boundsOf(data)
            if (bounds != null) {
                val (minX, maxX, minY, maxY) = bounds
                val dw = maxX - minX
                val dh = maxY - minY
                if (dw > 0 && dh > 0) {
                    vs.scale = min(w / dw, h / dh) * 0.9
                    vs.offsetX = w / 2.0 - (minX + maxX) / 2.0 * vs.scale
                    vs.offsetY = h / 2.0 - (minY + maxY) / 2.0 * vs.scale
                }
            }
            vs.initialized = true
        }

        gc.save()
        gc.setTransform(Affine(vs.scale, 0.0, vs.offsetX, 0.0, vs.scale, vs.offsetY))

        drawGrid(gc, w, h, vs)
        drawOriginCross(gc, vs.scale)
        drawEntities(gc, data, vs.scale)

        gc.restore()

        if (debugMode) drawOverlay(gc, data, vs.scale, w)
    }

    canvas.widthProperty().addListener { _, _, _ -> redraw() }
    canvas.heightProperty().addListener { _, _, _ -> redraw() }

    canvas.setOnMousePressed { e -> vs.lastMouseX = e.x; vs.lastMouseY = e.y }
    canvas.setOnMouseDragged { e ->
        vs.offsetX += e.x - vs.lastMouseX
        vs.offsetY += e.y - vs.lastMouseY
        vs.lastMouseX = e.x
        vs.lastMouseY = e.y
        redraw()
    }

    canvas.addEventHandler(ScrollEvent.SCROLL) { e ->
        val factor = if (e.deltaY < 0) 0.9 else 1.1
        val newScale = (vs.scale * factor).coerceIn(0.0001, 100.0)
        val cx = canvas.width / 2.0
        val cy = canvas.height / 2.0
        val wx = (cx - vs.offsetX) / vs.scale
        val wy = (cy - vs.offsetY) / vs.scale
        vs.offsetX = cx - wx * newScale
        vs.offsetY = cy - wy * newScale
        vs.scale = newScale
        redraw()
    }

    jfxPanel.scene = Scene(pane)
    Platform.runLater { redraw() }
}

// ---- bounds ----

private fun boundsOf(data: DxfParseResult): List<Double>? {
    val hdr = data.header
    if (hdr != null && hdr.extMax.first > hdr.extMin.first && hdr.extMax.second > hdr.extMin.second)
        return listOf(hdr.extMin.first, hdr.extMax.first, hdr.extMin.second, hdr.extMax.second)

    val xs = mutableListOf<Double>()
    val ys = mutableListOf<Double>()
    data.lines.forEach { xs += it.x1; xs += it.x2; ys += it.y1; ys += it.y2 }
    data.circles.forEach { xs += it.centerX - it.radius; xs += it.centerX + it.radius; ys += it.centerY - it.radius; ys += it.centerY + it.radius }
    data.arcs.forEach { xs += it.centerX - it.radius; xs += it.centerX + it.radius; ys += it.centerY - it.radius; ys += it.centerY + it.radius }
    data.lwPolylines.forEach { p -> p.vertices.forEach { (x, y) -> xs += x; ys += y } }
    data.texts.forEach { xs += it.x; ys += it.y }
    return if (xs.isEmpty()) null else listOf(xs.min(), xs.max(), ys.min(), ys.max())
}

// ---- drawing helpers ----

private fun drawGrid(gc: GraphicsContext, canvasW: Double, canvasH: Double, vs: JfxViewState) {
    val spacing = 1000.0
    gc.stroke = Color.gray(0.5, 0.3)
    gc.lineWidth = 1.0 / vs.scale

    val wMinX = -vs.offsetX / vs.scale
    val wMinY = -vs.offsetY / vs.scale
    val wMaxX = (canvasW - vs.offsetX) / vs.scale
    val wMaxY = (canvasH - vs.offsetY) / vs.scale

    var gx = (wMinX / spacing).toLong() * spacing
    while (gx <= wMaxX) { gc.strokeLine(gx, wMinY, gx, wMaxY); gx += spacing }
    var gy = (wMinY / spacing).toLong() * spacing
    while (gy <= wMaxY) { gc.strokeLine(wMinX, gy, wMaxX, gy); gy += spacing }
}

private fun drawOriginCross(gc: GraphicsContext, scale: Double) {
    val sz = 100.0 / scale
    gc.stroke = Color.RED; gc.lineWidth = 2.0 / scale
    gc.strokeLine(-sz, 0.0, sz, 0.0)
    gc.strokeLine(0.0, -sz, 0.0, sz)
}

private fun drawEntities(gc: GraphicsContext, data: DxfParseResult, scale: Double) {
    val lw = 1.0 / scale

    // LINE
    data.lines.forEach { l ->
        gc.stroke = aciToJfxColor(l.color); gc.lineWidth = lw
        gc.strokeLine(l.x1, l.y1, l.x2, l.y2)
    }

    // CIRCLE
    data.circles.forEach { c ->
        gc.stroke = aciToJfxColor(c.color); gc.lineWidth = lw
        gc.strokeOval(c.centerX - c.radius, c.centerY - c.radius, c.radius * 2, c.radius * 2)
    }

    // ARC
    data.arcs.forEach { a ->
        gc.stroke = aciToJfxColor(a.color); gc.lineWidth = lw
        val start = -a.startAngle
        val end = -a.endAngle
        var sweep = end - start
        if (sweep > 0) sweep -= 360.0
        gc.strokeArc(
            a.centerX - a.radius, a.centerY - a.radius,
            a.radius * 2, a.radius * 2,
            start, sweep,
            javafx.scene.shape.ArcType.OPEN
        )
    }

    // LWPOLYLINE
    data.lwPolylines.forEach { p ->
        if (p.vertices.size >= 2) {
            gc.stroke = aciToJfxColor(p.color); gc.lineWidth = lw
            gc.beginPath()
            gc.moveTo(p.vertices[0].first, p.vertices[0].second)
            for (i in 1 until p.vertices.size) gc.lineTo(p.vertices[i].first, p.vertices[i].second)
            if (p.isClosed && p.vertices.size > 2) gc.closePath()
            gc.stroke()
        }
    }

    // TEXT
    data.texts.forEach { t ->
        gc.save()
        gc.fill = aciToJfxColor(t.color)
        gc.font = Font.font("SansSerif", t.height)

        val tw = t.text.length * t.height * 0.6 // rough estimate
        val x = when (t.alignH) { 1 -> t.x - tw / 2; 2 -> t.x - tw; else -> t.x }
        val y = when (t.alignV) { 2 -> t.y + t.height / 2; 3 -> t.y + t.height; else -> t.y }

        if (t.rotation != 0.0) {
            gc.translate(t.x, t.y); gc.rotate(-t.rotation); gc.translate(-t.x, -t.y)
        }
        gc.fillText(t.text, x, y)
        gc.restore()
    }
}

private fun drawOverlay(gc: GraphicsContext, data: DxfParseResult, scale: Double, canvasW: Double) {
    gc.save()
    gc.setTransform(Affine())
    gc.fill = Color.rgb(0, 0, 0, 0.7)
    gc.fillRect(canvasW - 250.0, 10.0, 240.0, 100.0)
    gc.fill = Color.WHITE
    gc.font = Font.font("SansSerif", 12.0)
    val x0 = canvasW - 240.0
    gc.fillText("C: JavaFX Canvas (OpenJFX) Viewer", x0, 30.0)
    gc.fillText("Lines: ${data.lines.size}  Circles: ${data.circles.size}", x0, 48.0)
    gc.fillText("Arcs: ${data.arcs.size}  Polylines: ${data.lwPolylines.size}", x0, 66.0)
    gc.fillText("Texts: ${data.texts.size}  Scale: ${"%.4f".format(scale)}", x0, 84.0)
    gc.fillText("Renderer: Prism (HW accelerated)", x0, 100.0)
    gc.restore()
}
