package com.jpaver.trianglelist.cadview

import com.jpaver.trianglelist.dxf.*
import com.jpaver.trianglelist.util.CanvasUtil
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.GL_MULTISAMPLE
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.stb.STBTTAlignedQuad
import org.lwjgl.stb.STBTTBakedChar
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.NULL
import java.io.File

/**
 * LWJGL + OpenGL による DXF ビューワー (B案)
 * GLFW ウィンドウ + OpenGL 2D 正射影描画
 */
class OpenGLCadViewer {

    private var window: Long = NULL

    // ビュー変換 (パン・ズーム)
    private var panX = 0.0
    private var panY = 0.0
    private var zoom = 1.0

    // ドラッグ状態
    private var dragging = false
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    // ウィンドウサイズ
    private var windowWidth = 1280
    private var windowHeight = 720

    // フォントテクスチャ
    private var fontTextureId = 0
    private var fontBakedChars: STBTTBakedChar.Buffer? = null
    private var fontBitmapWidth = 512
    private var fontBitmapHeight = 512
    private var fontPixelHeight = 32f
    private var fontLoaded = false

    // ACI カラーテーブル
    private val aciColors: Array<FloatArray> = buildAciColorTable()

    /**
     * ビューワーを起動し、DXFデータを表示する
     */
    fun show(parseResult: DxfParseResult, title: String = "OpenGL CAD Viewer") {
        val data = CanvasUtil.flipYAxis(parseResult)
        initGlfw()
        createWindow(title)
        initOpenGL()
        loadFont()
        fitToDrawing(data)

        while (!glfwWindowShouldClose(window)) {
            render(data)
            glfwSwapBuffers(window)
            glfwPollEvents()
        }
        cleanup()
    }

    // --- 初期化 ---

    private fun initGlfw() {
        GLFWErrorCallback.createPrint(System.err).set()
        if (!glfwInit()) throw IllegalStateException("GLFW init failed")
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        glfwWindowHint(GLFW_SAMPLES, 4)
    }

    private fun createWindow(title: String) {
        window = glfwCreateWindow(windowWidth, windowHeight, title, NULL, NULL)
        if (window == NULL) throw RuntimeException("GLFW window creation failed")

        glfwSetMouseButtonCallback(window) { _, button, action, _ ->
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = action == GLFW_PRESS
                if (dragging) {
                    val xBuf = DoubleArray(1)
                    val yBuf = DoubleArray(1)
                    glfwGetCursorPos(window, xBuf, yBuf)
                    lastMouseX = xBuf[0]
                    lastMouseY = yBuf[0]
                }
            }
        }

        glfwSetCursorPosCallback(window) { _, xPos, yPos ->
            if (dragging) {
                panX += (xPos - lastMouseX) / zoom
                panY -= (yPos - lastMouseY) / zoom
                lastMouseX = xPos
                lastMouseY = yPos
            }
        }

        glfwSetScrollCallback(window) { _, _, yOffset ->
            zoom *= if (yOffset > 0) 1.1 else 1.0 / 1.1
        }

        glfwSetFramebufferSizeCallback(window) { _, w, h ->
            windowWidth = w
            windowHeight = h
            glViewport(0, 0, w, h)
        }

        glfwSetKeyCallback(window) { _, key, _, action, _ ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true)
            }
        }

        glfwMakeContextCurrent(window)
        glfwSwapInterval(1) // VSync
        glfwShowWindow(window)
    }

    private fun initOpenGL() {
        GL.createCapabilities()
        glEnable(GL_MULTISAMPLE)
        glEnable(GL_LINE_SMOOTH)
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
    }

    private fun loadFont() {
        val fontPaths = listOf(
            "C:/Windows/Fonts/msgothic.ttc",
            "C:/Windows/Fonts/meiryo.ttc",
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/consola.ttf"
        )
        val fontFile = fontPaths.map { File(it) }.firstOrNull { it.exists() }
        if (fontFile == null) {
            println("Warning: no font found, text rendering disabled")
            return
        }

        try {
            val fontData = fontFile.readBytes()
            val fontBuffer = MemoryUtil.memAlloc(fontData.size)
            fontBuffer.put(fontData).flip()

            fontBakedChars = STBTTBakedChar.malloc(96) // ASCII 32-127
            val bitmap = MemoryUtil.memAlloc(fontBitmapWidth * fontBitmapHeight)
            stbtt_BakeFontBitmap(fontBuffer, fontPixelHeight, bitmap, fontBitmapWidth, fontBitmapHeight, 32, fontBakedChars!!)

            fontTextureId = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, fontTextureId)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, fontBitmapWidth, fontBitmapHeight, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

            MemoryUtil.memFree(bitmap)
            MemoryUtil.memFree(fontBuffer)
            fontLoaded = true
            println("Font loaded: ${fontFile.name}")
        } catch (e: Exception) {
            println("Warning: font load error: ${e.message}")
        }
    }

    private fun fitToDrawing(data: DxfParseResult) {
        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE

        fun expand(x: Double, y: Double) {
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }

        data.lines.forEach { expand(it.x1, it.y1); expand(it.x2, it.y2) }
        data.circles.forEach {
            expand(it.centerX - it.radius, it.centerY - it.radius)
            expand(it.centerX + it.radius, it.centerY + it.radius)
        }
        data.arcs.forEach {
            expand(it.centerX - it.radius, it.centerY - it.radius)
            expand(it.centerX + it.radius, it.centerY + it.radius)
        }
        data.lwPolylines.forEach { p -> p.vertices.forEach { (x, y) -> expand(x, y) } }
        data.texts.forEach { expand(it.x, it.y) }

        if (minX > maxX) return
        val w = maxX - minX
        val h = maxY - minY
        if (w <= 0 || h <= 0) return
        zoom = minOf(windowWidth / w * 0.9, windowHeight / h * 0.9)
        panX = -(minX + maxX) / 2.0
        panY = -(minY + maxY) / 2.0
    }

    // --- 描画 ---

    private fun render(data: DxfParseResult) {
        glClear(GL_COLOR_BUFFER_BIT)

        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        val halfW = windowWidth / 2.0 / zoom
        val halfH = windowHeight / 2.0 / zoom
        glOrtho(-halfW - panX, halfW - panX, -halfH - panY, halfH - panY, -1.0, 1.0)

        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()

        drawGrid(halfW, halfH)
        drawOriginCross()
        drawLines(data.lines)
        drawCircles(data.circles)
        drawArcs(data.arcs)
        drawPolylines(data.lwPolylines)
        drawTexts(data.texts)
    }

    private fun drawGrid(halfW: Double, halfH: Double) {
        val gs = 1000.0
        glColor4f(0.3f, 0.3f, 0.3f, 0.3f)
        glLineWidth(1f)

        val sx = ((-halfW - panX) / gs).toLong() * gs.toLong()
        val ex = ((halfW - panX) / gs).toLong() * gs.toLong()
        val sy = ((-halfH - panY) / gs).toLong() * gs.toLong()
        val ey = ((halfH - panY) / gs).toLong() * gs.toLong()

        glBegin(GL_LINES)
        var gx = sx.toDouble()
        while (gx <= ex.toDouble()) {
            glVertex2d(gx, -halfH - panY)
            glVertex2d(gx, halfH - panY)
            gx += gs
        }
        var gy = sy.toDouble()
        while (gy <= ey.toDouble()) {
            glVertex2d(-halfW - panX, gy)
            glVertex2d(halfW - panX, gy)
            gy += gs
        }
        glEnd()
    }

    private fun drawOriginCross() {
        val cs = 100.0 / zoom
        glLineWidth(2f)
        glColor3f(1f, 0f, 0f)
        glBegin(GL_LINES); glVertex2d(-cs, 0.0); glVertex2d(cs, 0.0); glEnd()
        glColor3f(0f, 1f, 0f)
        glBegin(GL_LINES); glVertex2d(0.0, -cs); glVertex2d(0.0, cs); glEnd()
    }

    private fun drawLines(lines: List<DxfLine>) {
        glLineWidth(1f)
        glBegin(GL_LINES)
        for (l in lines) {
            setAciColor(l.color)
            glVertex2d(l.x1, l.y1)
            glVertex2d(l.x2, l.y2)
        }
        glEnd()
    }

    private fun drawCircles(circles: List<DxfCircle>) {
        glLineWidth(1f)
        for (c in circles) {
            setAciColor(c.color)
            glBegin(GL_LINE_LOOP)
            for (i in 0 until 64) {
                val a = 2.0 * Math.PI * i / 64
                glVertex2d(c.centerX + c.radius * Math.cos(a), c.centerY + c.radius * Math.sin(a))
            }
            glEnd()
        }
    }

    private fun drawArcs(arcs: List<DxfArc>) {
        glLineWidth(1f)
        for (arc in arcs) {
            setAciColor(arc.color)
            // DXF角度は度数法、Y軸反転後なので角度を反転
            val sr = Math.toRadians(-arc.startAngle)
            val er = Math.toRadians(-arc.endAngle)
            var sweep = er - sr
            if (sweep > 0) sweep -= 2.0 * Math.PI
            val n = maxOf(8, (Math.abs(sweep) / (2.0 * Math.PI) * 64).toInt())
            glBegin(GL_LINE_STRIP)
            for (i in 0..n) {
                val a = sr + sweep * i.toDouble() / n
                glVertex2d(arc.centerX + arc.radius * Math.cos(a), arc.centerY + arc.radius * Math.sin(a))
            }
            glEnd()
        }
    }

    private fun drawPolylines(polylines: List<DxfLwPolyline>) {
        glLineWidth(1f)
        for (p in polylines) {
            if (p.vertices.size < 2) continue
            setAciColor(p.color)
            glBegin(if (p.isClosed) GL_LINE_LOOP else GL_LINE_STRIP)
            for ((x, y) in p.vertices) glVertex2d(x, y)
            glEnd()
        }
    }

    private fun drawTexts(texts: List<DxfText>) {
        if (!fontLoaded) return
        glEnable(GL_TEXTURE_2D)
        glBindTexture(GL_TEXTURE_2D, fontTextureId)
        for (t in texts) {
            setAciColor(t.color)
            drawSingleText(t)
        }
        glDisable(GL_TEXTURE_2D)
    }

    private fun drawSingleText(text: DxfText) {
        val chars = fontBakedChars ?: return
        val ts = text.height / fontPixelHeight

        MemoryStack.stackPush().use { stack ->
            val xBuf = stack.floats(0f)
            val yBuf = stack.floats(0f)
            val q = STBTTAlignedQuad.malloc(stack)

            // テキスト幅を測定
            var totalWidth = 0f
            for (ch in text.text) {
                val cp = ch.code
                if (cp in 32..127) totalWidth += chars[cp - 32].xadvance()
            }
            totalWidth *= ts.toFloat()
            val totalHeight = fontPixelHeight * ts.toFloat()

            // アライメントオフセット
            val ax: Double = when (text.alignH) {
                1 -> -totalWidth.toDouble() / 2.0
                2 -> -totalWidth.toDouble()
                else -> 0.0
            }
            val ay: Double = when (text.alignV) {
                2 -> totalHeight.toDouble() / 2.0
                3 -> totalHeight.toDouble()
                else -> 0.0
            }

            glPushMatrix()
            glTranslated(text.x, text.y, 0.0)
            if (text.rotation != 0.0) glRotated(-text.rotation, 0.0, 0.0, 1.0)
            glTranslated(ax, ay, 0.0)
            glScaled(ts, -ts, 1.0) // Y反転 (STBはY下向きが正)

            xBuf.put(0, 0f)
            yBuf.put(0, 0f)
            for (ch in text.text) {
                val cp = ch.code
                if (cp < 32 || cp >= 128) continue
                stbtt_GetBakedQuad(chars, fontBitmapWidth, fontBitmapHeight, cp - 32, xBuf, yBuf, q, true)
                glBegin(GL_QUADS)
                glTexCoord2f(q.s0(), q.t0()); glVertex2f(q.x0(), q.y0())
                glTexCoord2f(q.s1(), q.t0()); glVertex2f(q.x1(), q.y0())
                glTexCoord2f(q.s1(), q.t1()); glVertex2f(q.x1(), q.y1())
                glTexCoord2f(q.s0(), q.t1()); glVertex2f(q.x0(), q.y1())
                glEnd()
            }
            glPopMatrix()
        }
    }

    // --- カラー ---

    private fun setAciColor(aci: Int) {
        val c = aciColors[aci.coerceIn(0, 255)]
        glColor3f(c[0], c[1], c[2])
    }

    // --- クリーンアップ ---

    private fun cleanup() {
        fontBakedChars?.free()
        if (fontTextureId != 0) glDeleteTextures(fontTextureId)
        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val fp = args.firstOrNull { !it.startsWith("-") && it.endsWith(".dxf", ignoreCase = true) }
            if (fp == null) { println("Usage: OpenGLCadViewer <file.dxf>"); return }
            val f = File(fp)
            if (!f.exists()) { println("File not found: $fp"); return }
            val r = DxfParser().parse(f.readText())
            println("Loaded: ${r.lines.size} lines, ${r.circles.size} circles, ${r.arcs.size} arcs, ${r.lwPolylines.size} polylines, ${r.texts.size} texts")
            OpenGLCadViewer().show(r, "OpenGL CAD Viewer - ${f.name}")
        }
    }
}

// --- ACI カラーテーブル構築 ---

private fun buildAciColorTable(): Array<FloatArray> {
    val t = Array(256) { floatArrayOf(1f, 1f, 1f) }
    t[0] = floatArrayOf(1f, 1f, 1f)       // ByBlock -> white on dark bg
    t[1] = floatArrayOf(1f, 0f, 0f)       // Red
    t[2] = floatArrayOf(1f, 1f, 0f)       // Yellow
    t[3] = floatArrayOf(0f, 1f, 0f)       // Green
    t[4] = floatArrayOf(0f, 1f, 1f)       // Cyan
    t[5] = floatArrayOf(0f, 0f, 1f)       // Blue
    t[6] = floatArrayOf(1f, 0f, 1f)       // Magenta
    t[7] = floatArrayOf(1f, 1f, 1f)       // White
    t[8] = floatArrayOf(0.5f, 0.5f, 0.5f) // Gray
    t[9] = floatArrayOf(0.75f, 0.75f, 0.75f) // Light gray

    // 10-249: color wheel (24 hue groups x 10 shades)
    for (hg in 0 until 24) {
        val bi = 10 + hg * 10
        val h = hg.toFloat() / 24f
        val (r, g, b) = hsvToRgb(h, 1f, 1f)
        t[bi] = floatArrayOf(r, g, b)
        val (r1, g1, b1) = hsvToRgb(h, 0.5f, 1f)
        t[bi + 1] = floatArrayOf(r1, g1, b1)
        for (s in 0 until 4) {
            val v = 1f - s * 0.2f
            val (rs, gs, bs) = hsvToRgb(h, 1f, v)
            t[bi + 2 + s * 2] = floatArrayOf(rs, gs, bs)
            val (rd, gd, bd) = hsvToRgb(h, 0.5f, v)
            t[bi + 3 + s * 2] = floatArrayOf(rd, gd, bd)
        }
    }

    // 250-255: grayscale
    t[250] = floatArrayOf(0.2f, 0.2f, 0.2f)
    t[251] = floatArrayOf(0.31f, 0.31f, 0.31f)
    t[252] = floatArrayOf(0.41f, 0.41f, 0.41f)
    t[253] = floatArrayOf(0.51f, 0.51f, 0.51f)
    t[254] = floatArrayOf(0.75f, 0.75f, 0.75f)
    t[255] = floatArrayOf(1f, 1f, 1f)
    return t
}

private fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Float, Float, Float> {
    val i = (h * 6).toInt()
    val f = h * 6 - i
    val p = v * (1 - s)
    val q = v * (1 - f * s)
    val t = v * (1 - (1 - f) * s)
    return when (i % 6) {
        0 -> Triple(v, t, p)
        1 -> Triple(q, v, p)
        2 -> Triple(p, v, t)
        3 -> Triple(p, q, v)
        4 -> Triple(t, p, v)
        5 -> Triple(v, p, q)
        else -> Triple(v, v, v)
    }
}
