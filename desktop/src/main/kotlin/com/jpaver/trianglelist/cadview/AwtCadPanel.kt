package com.jpaver.trianglelist.cadview

import com.jpaver.trianglelist.dxf.*
import com.jpaver.trianglelist.util.CanvasUtil
import java.awt.*
import java.awt.event.*
import java.awt.font.TextLayout
import java.awt.geom.*
import javax.swing.JPanel

/**
 * AWT Graphics2D ベースの DXF ビューワーパネル
 * JPanel を継承し、paintComponent で DXF エンティティを描画する
 */
class AwtCadPanel : JPanel() {

    private var parseResult: DxfParseResult? = null
    private var flippedData: DxfParseResult? = null

    // ビュー変換パラメータ
    private var scale: Double = 1.0
    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var isInitialized = false

    // ドラッグ用
    private var lastDragPoint: Point? = null

    companion object {
        // ACI カラーテーブル (0-255) — java.awt.Color 版（全インスタンスで共有）
        private val aciColorTable: Array<Color> = arrayOf(
        Color(0, 0, 0),          // 0: ByBlock
        Color(255, 0, 0),        // 1: Red
        Color(255, 255, 0),      // 2: Yellow
        Color(0, 255, 0),        // 3: Green
        Color(0, 255, 255),      // 4: Cyan
        Color(0, 0, 255),        // 5: Blue
        Color(255, 0, 255),      // 6: Magenta
        Color(0, 0, 0),          // 7: White→黒（白背景用）
        Color(128, 128, 128),    // 8: Gray
        Color(192, 192, 192),    // 9: Light Gray
        Color(255, 0, 0),        // 10
        Color(255, 127, 127),    // 11
        Color(204, 0, 0),        // 12
        Color(204, 102, 102),    // 13
        Color(153, 0, 0),        // 14
        Color(153, 76, 76),      // 15
        Color(127, 0, 0),        // 16
        Color(127, 63, 63),      // 17
        Color(76, 0, 0),         // 18
        Color(76, 38, 38),       // 19
        Color(255, 63, 0),       // 20
        Color(255, 159, 127),    // 21
        Color(204, 51, 0),       // 22
        Color(204, 127, 102),    // 23
        Color(153, 38, 0),       // 24
        Color(153, 95, 76),      // 25
        Color(127, 31, 0),       // 26
        Color(127, 79, 63),      // 27
        Color(76, 19, 0),        // 28
        Color(76, 47, 38),       // 29
        Color(255, 127, 0),      // 30
        Color(255, 191, 127),    // 31
        Color(204, 102, 0),      // 32
        Color(204, 153, 102),    // 33
        Color(153, 76, 0),       // 34
        Color(153, 114, 76),     // 35
        Color(127, 63, 0),       // 36
        Color(127, 95, 63),      // 37
        Color(76, 38, 0),        // 38
        Color(76, 57, 38),       // 39
        Color(255, 191, 0),      // 40
        Color(255, 223, 127),    // 41
        Color(204, 153, 0),      // 42
        Color(204, 178, 102),    // 43
        Color(153, 114, 0),      // 44
        Color(153, 133, 76),     // 45
        Color(127, 95, 0),       // 46
        Color(127, 111, 63),     // 47
        Color(76, 57, 0),        // 48
        Color(76, 66, 38),       // 49
        Color(255, 255, 0),      // 50
        Color(255, 255, 127),    // 51
        Color(204, 204, 0),      // 52
        Color(204, 204, 102),    // 53
        Color(153, 153, 0),      // 54
        Color(153, 153, 76),     // 55
        Color(127, 127, 0),      // 56
        Color(127, 127, 63),     // 57
        Color(76, 76, 0),        // 58
        Color(76, 76, 38),       // 59
        Color(191, 255, 0),      // 60
        Color(223, 255, 127),    // 61
        Color(153, 204, 0),      // 62
        Color(178, 204, 102),    // 63
        Color(114, 153, 0),      // 64
        Color(133, 153, 76),     // 65
        Color(95, 127, 0),       // 66
        Color(111, 127, 63),     // 67
        Color(57, 76, 0),        // 68
        Color(66, 76, 38),       // 69
        Color(127, 255, 0),      // 70
        Color(191, 255, 127),    // 71
        Color(102, 204, 0),      // 72
        Color(153, 204, 102),    // 73
        Color(76, 153, 0),       // 74
        Color(114, 153, 76),     // 75
        Color(63, 127, 0),       // 76
        Color(95, 127, 63),      // 77
        Color(38, 76, 0),        // 78
        Color(57, 76, 38),       // 79
        Color(63, 255, 0),       // 80
        Color(159, 255, 127),    // 81
        Color(51, 204, 0),       // 82
        Color(127, 204, 102),    // 83
        Color(38, 153, 0),       // 84
        Color(95, 153, 76),      // 85
        Color(31, 127, 0),       // 86
        Color(79, 127, 63),      // 87
        Color(19, 76, 0),        // 88
        Color(47, 76, 38),       // 89
        Color(0, 255, 0),        // 90
        Color(127, 255, 127),    // 91
        Color(0, 204, 0),        // 92
        Color(102, 204, 102),    // 93
        Color(0, 153, 0),        // 94
        Color(76, 153, 76),      // 95
        Color(0, 127, 0),        // 96
        Color(63, 127, 63),      // 97
        Color(0, 76, 0),         // 98
        Color(38, 76, 38),       // 99
        Color(0, 255, 63),       // 100
        Color(127, 255, 159),    // 101
        Color(0, 204, 51),       // 102
        Color(102, 204, 127),    // 103
        Color(0, 153, 38),       // 104
        Color(76, 153, 95),      // 105
        Color(0, 127, 31),       // 106
        Color(63, 127, 79),      // 107
        Color(0, 76, 19),        // 108
        Color(38, 76, 47),       // 109
        Color(0, 255, 127),      // 110
        Color(127, 255, 191),    // 111
        Color(0, 204, 102),      // 112
        Color(102, 204, 153),    // 113
        Color(0, 153, 76),       // 114
        Color(76, 153, 114),     // 115
        Color(0, 127, 63),       // 116
        Color(63, 127, 95),      // 117
        Color(0, 76, 38),        // 118
        Color(38, 76, 57),       // 119
        Color(0, 255, 191),      // 120
        Color(127, 255, 223),    // 121
        Color(0, 204, 153),      // 122
        Color(102, 204, 178),    // 123
        Color(0, 153, 114),      // 124
        Color(76, 153, 133),     // 125
        Color(0, 127, 95),       // 126
        Color(63, 127, 111),     // 127
        Color(0, 76, 57),        // 128
        Color(38, 76, 66),       // 129
        Color(0, 255, 255),      // 130
        Color(127, 255, 255),    // 131
        Color(0, 204, 204),      // 132
        Color(102, 204, 204),    // 133
        Color(0, 153, 153),      // 134
        Color(76, 153, 153),     // 135
        Color(0, 127, 127),      // 136
        Color(63, 127, 127),     // 137
        Color(0, 76, 76),        // 138
        Color(38, 76, 76),       // 139
        Color(0, 191, 255),      // 140
        Color(127, 223, 255),    // 141
        Color(0, 153, 204),      // 142
        Color(102, 178, 204),    // 143
        Color(0, 114, 153),      // 144
        Color(76, 133, 153),     // 145
        Color(0, 95, 127),       // 146
        Color(63, 111, 127),     // 147
        Color(0, 57, 76),        // 148
        Color(38, 66, 76),       // 149
        Color(0, 127, 255),      // 150
        Color(127, 191, 255),    // 151
        Color(0, 102, 204),      // 152
        Color(102, 153, 204),    // 153
        Color(0, 76, 153),       // 154
        Color(76, 114, 153),     // 155
        Color(0, 63, 127),       // 156
        Color(63, 95, 127),      // 157
        Color(0, 38, 76),        // 158
        Color(38, 57, 76),       // 159
        Color(0, 63, 255),       // 160
        Color(127, 159, 255),    // 161
        Color(0, 51, 204),       // 162
        Color(102, 127, 204),    // 163
        Color(0, 38, 153),       // 164
        Color(76, 95, 153),      // 165
        Color(0, 31, 127),       // 166
        Color(63, 79, 127),      // 167
        Color(0, 19, 76),        // 168
        Color(38, 47, 76),       // 169
        Color(0, 0, 255),        // 170
        Color(127, 127, 255),    // 171
        Color(0, 0, 204),        // 172
        Color(102, 102, 204),    // 173
        Color(0, 0, 153),        // 174
        Color(76, 76, 153),      // 175
        Color(0, 0, 127),        // 176
        Color(63, 63, 127),      // 177
        Color(0, 0, 76),         // 178
        Color(38, 38, 76),       // 179
        Color(63, 0, 255),       // 180
        Color(159, 127, 255),    // 181
        Color(51, 0, 204),       // 182
        Color(127, 102, 204),    // 183
        Color(38, 0, 153),       // 184
        Color(95, 76, 153),      // 185
        Color(31, 0, 127),       // 186
        Color(79, 63, 127),      // 187
        Color(19, 0, 76),        // 188
        Color(47, 38, 76),       // 189
        Color(127, 0, 255),      // 190
        Color(191, 127, 255),    // 191
        Color(102, 0, 204),      // 192
        Color(153, 102, 204),    // 193
        Color(76, 0, 153),       // 194
        Color(114, 76, 153),     // 195
        Color(63, 0, 127),       // 196
        Color(95, 63, 127),      // 197
        Color(38, 0, 76),        // 198
        Color(57, 38, 76),       // 199
        Color(191, 0, 255),      // 200
        Color(223, 127, 255),    // 201
        Color(153, 0, 204),      // 202
        Color(178, 102, 204),    // 203
        Color(114, 0, 153),      // 204
        Color(133, 76, 153),     // 205
        Color(95, 0, 127),       // 206
        Color(111, 63, 127),     // 207
        Color(57, 0, 76),        // 208
        Color(66, 38, 76),       // 209
        Color(255, 0, 255),      // 210
        Color(255, 127, 255),    // 211
        Color(204, 0, 204),      // 212
        Color(204, 102, 204),    // 213
        Color(153, 0, 153),      // 214
        Color(153, 76, 153),     // 215
        Color(127, 0, 127),      // 216
        Color(127, 63, 127),     // 217
        Color(76, 0, 76),        // 218
        Color(76, 38, 76),       // 219
        Color(255, 0, 191),      // 220
        Color(255, 127, 223),    // 221
        Color(204, 0, 153),      // 222
        Color(204, 102, 178),    // 223
        Color(153, 0, 114),      // 224
        Color(153, 76, 133),     // 225
        Color(127, 0, 95),       // 226
        Color(127, 63, 111),     // 227
        Color(76, 0, 57),        // 228
        Color(76, 38, 66),       // 229
        Color(255, 0, 127),      // 230
        Color(255, 127, 191),    // 231
        Color(204, 0, 102),      // 232
        Color(204, 102, 153),    // 233
        Color(153, 0, 76),       // 234
        Color(153, 76, 114),     // 235
        Color(127, 0, 63),       // 236
        Color(127, 63, 95),      // 237
        Color(76, 0, 38),        // 238
        Color(76, 38, 57),       // 239
        Color(255, 0, 63),       // 240
        Color(255, 127, 159),    // 241
        Color(204, 0, 51),       // 242
        Color(204, 102, 127),    // 243
        Color(153, 0, 38),       // 244
        Color(153, 76, 95),      // 245
        Color(127, 0, 31),       // 246
        Color(127, 63, 79),      // 247
        Color(76, 0, 19),        // 248
        Color(76, 38, 47),       // 249
        Color(51, 51, 51),       // 250
        Color(80, 80, 80),       // 251
        Color(105, 105, 105),    // 252
        Color(130, 130, 130),    // 253
        Color(190, 190, 190),    // 254
        Color(255, 255, 255),    // 255
        )
    }

    init {
        background = Color.WHITE
        isFocusable = true

        // パンのドラッグ
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                lastDragPoint = e.point
            }

            override fun mouseReleased(e: MouseEvent) {
                lastDragPoint = null
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                val last = lastDragPoint ?: return
                offsetX += (e.x - last.x).toDouble()
                offsetY += (e.y - last.y).toDouble()
                lastDragPoint = e.point
                repaint()
            }
        })

        // ホイールでズーム（カーソル中心）
        addMouseWheelListener { e ->
            val zoomFactor = if (e.wheelRotation > 0) 0.9 else 1.1
            val newScale = (scale * zoomFactor).coerceIn(0.0001, 1000.0)

            // カーソル位置のワールド座標を保持してズーム
            val mouseX = e.x.toDouble()
            val mouseY = e.y.toDouble()
            val worldX = (mouseX - offsetX) / scale
            val worldY = (mouseY - offsetY) / scale

            scale = newScale
            offsetX = mouseX - worldX * newScale
            offsetY = mouseY - worldY * newScale

            repaint()
        }
    }

    fun setParseResult(result: DxfParseResult?) {
        parseResult = result
        flippedData = result?.let { CanvasUtil.flipYAxis(it) }
        if (result != null && !isInitialized) {
            isInitialized = false // fitToView を paintComponent 内で実行
        }
        repaint()
    }

    private fun aciToAwtColor(aciIndex: Int): Color {
        return when {
            // 白背景なので ACI 7(白/黒) と 256(ByLayer) は黒で描画
            aciIndex == DxfConstants.Colors.WHITE_BLACK -> Color.BLACK
            aciIndex == DxfConstants.Colors.WHITE -> Color.BLACK
            aciIndex in 0..255 -> aciColorTable[aciIndex]
            else -> Color.BLACK
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D

        // 明示的に白背景を描画（SwingPanel が JPanel.background を無視する場合の対策）
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, width, height)

        // アンチエイリアス
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        val data = flippedData ?: return

        // 初回表示時にフィット
        if (!isInitialized) {
            fitToView(data)
            isInitialized = true
        }

        // ワールド→スクリーン変換
        val viewTransform = AffineTransform().apply {
            translate(offsetX, offsetY)
            scale(scale, scale)
        }
        g2d.transform(viewTransform)

        // グリッド描画
        drawGrid(g2d)

        // 原点十字
        drawOriginCross(g2d)

        // エンティティ描画
        drawLines(g2d, data.lines)
        drawCircles(g2d, data.circles)
        drawArcs(g2d, data.arcs)
        drawPolylines(g2d, data.lwPolylines)
        drawTexts(g2d, data.texts)
    }

    private fun fitToView(data: DxfParseResult) {
        val bounds = calculateBounds(data) ?: return
        val (minX, maxX, minY, maxY) = bounds
        val drawingWidth = maxX - minX
        val drawingHeight = maxY - minY
        if (drawingWidth <= 0 || drawingHeight <= 0) return

        val canvasW = width.toDouble()
        val canvasH = height.toDouble()
        if (canvasW <= 0 || canvasH <= 0) return

        scale = minOf(canvasW / drawingWidth, canvasH / drawingHeight) * 0.9

        val drawingCenterX = (minX + maxX) / 2.0
        val drawingCenterY = (minY + maxY) / 2.0
        offsetX = canvasW / 2.0 - drawingCenterX * scale
        offsetY = canvasH / 2.0 - drawingCenterY * scale
    }

    private fun calculateBounds(data: DxfParseResult): List<Double>? {
        // ヘッダーから取得
        data.header?.let { header ->
            val minX = header.extMin.first
            val minY = header.extMin.second
            val maxX = header.extMax.first
            val maxY = header.extMax.second
            if (maxX > minX && maxY > minY) {
                return listOf(minX, maxX, minY, maxY)
            }
        }

        // エンティティから計算
        val xs = mutableListOf<Double>()
        val ys = mutableListOf<Double>()

        data.lines.forEach { line ->
            xs += line.x1; xs += line.x2
            ys += line.y1; ys += line.y2
        }
        data.circles.forEach { circle ->
            xs += circle.centerX - circle.radius; xs += circle.centerX + circle.radius
            ys += circle.centerY - circle.radius; ys += circle.centerY + circle.radius
        }
        data.arcs.forEach { arc ->
            xs += arc.centerX - arc.radius; xs += arc.centerX + arc.radius
            ys += arc.centerY - arc.radius; ys += arc.centerY + arc.radius
        }
        data.lwPolylines.forEach { poly ->
            poly.vertices.forEach { (x, y) -> xs += x; ys += y }
        }
        data.texts.forEach { text ->
            xs += text.x; ys += text.y
        }

        if (xs.isEmpty() || ys.isEmpty()) return null
        return listOf(xs.min(), xs.max(), ys.min(), ys.max())
    }

    private fun drawGrid(g2d: Graphics2D) {
        val gridSpacing = 1000.0
        val gridColor = Color(200, 200, 200, 50)
        g2d.color = gridColor
        val strokeWidth = (1.0 / scale).toFloat()
        g2d.stroke = BasicStroke(strokeWidth)

        val worldMinX = -offsetX / scale
        val worldMinY = -offsetY / scale
        val worldMaxX = (width - offsetX) / scale
        val worldMaxY = (height - offsetY) / scale

        var gx = (worldMinX / gridSpacing).toLong() * gridSpacing
        while (gx <= worldMaxX) {
            g2d.draw(Line2D.Double(gx, worldMinY, gx, worldMaxY))
            gx += gridSpacing
        }
        var gy = (worldMinY / gridSpacing).toLong() * gridSpacing
        while (gy <= worldMaxY) {
            g2d.draw(Line2D.Double(worldMinX, gy, worldMaxX, gy))
            gy += gridSpacing
        }
    }

    private fun drawOriginCross(g2d: Graphics2D) {
        val crossSize = 100.0 / scale
        val strokeWidth = (2.0 / scale).toFloat()
        g2d.color = Color.RED
        g2d.stroke = BasicStroke(strokeWidth)
        g2d.draw(Line2D.Double(-crossSize, 0.0, crossSize, 0.0))
        g2d.draw(Line2D.Double(0.0, -crossSize, 0.0, crossSize))
    }

    private fun drawLines(g2d: Graphics2D, lines: List<DxfLine>) {
        val strokeWidth = (1.0 / scale).toFloat()
        g2d.stroke = BasicStroke(strokeWidth)
        lines.forEach { line ->
            g2d.color = aciToAwtColor(line.color)
            g2d.draw(Line2D.Double(line.x1, line.y1, line.x2, line.y2))
        }
    }

    private fun drawCircles(g2d: Graphics2D, circles: List<DxfCircle>) {
        val strokeWidth = (1.0 / scale).toFloat()
        g2d.stroke = BasicStroke(strokeWidth)
        circles.forEach { circle ->
            g2d.color = aciToAwtColor(circle.color)
            val d = circle.radius * 2.0
            g2d.draw(Ellipse2D.Double(
                circle.centerX - circle.radius,
                circle.centerY - circle.radius,
                d, d
            ))
        }
    }

    private fun drawArcs(g2d: Graphics2D, arcs: List<DxfArc>) {
        val strokeWidth = (1.0 / scale).toFloat()
        g2d.stroke = BasicStroke(strokeWidth)
        arcs.forEach { arc ->
            g2d.color = aciToAwtColor(arc.color)
            val d = arc.radius * 2.0

            // DXF: Y軸反転済みなので角度も反転
            val startAngle = -arc.startAngle
            val endAngle = -arc.endAngle
            var sweepAngle = endAngle - startAngle
            if (sweepAngle > 0) sweepAngle -= 360.0

            // Arc2D は開始角度と extent（スイープ角度）で指定
            g2d.draw(Arc2D.Double(
                arc.centerX - arc.radius,
                arc.centerY - arc.radius,
                d, d,
                startAngle, sweepAngle,
                Arc2D.OPEN
            ))
        }
    }

    private fun drawPolylines(g2d: Graphics2D, polylines: List<DxfLwPolyline>) {
        val strokeWidth = (1.0 / scale).toFloat()
        g2d.stroke = BasicStroke(strokeWidth)
        polylines.forEach { polyline ->
            if (polyline.vertices.size < 2) return@forEach
            g2d.color = aciToAwtColor(polyline.color)

            val path = GeneralPath()
            val first = polyline.vertices.first()
            path.moveTo(first.first.toFloat(), first.second.toFloat())

            polyline.vertices.drop(1).forEach { (x, y) ->
                path.lineTo(x.toFloat(), y.toFloat())
            }

            if (polyline.isClosed && polyline.vertices.size > 2) {
                path.closePath()
            }

            g2d.draw(path)
        }
    }

    private fun drawTexts(g2d: Graphics2D, texts: List<DxfText>) {
        texts.forEach { text ->
            g2d.color = aciToAwtColor(text.color)

            // DXF の height はワールド座標単位
            // AffineTransform が自動でスケールするのでそのまま使用
            val fontSize = text.height.toFloat()
            if (fontSize <= 0f) return@forEach

            val font = Font("SansSerif", Font.PLAIN, 1).deriveFont(fontSize)
            g2d.font = font
            val frc = g2d.fontRenderContext
            val textLayout = TextLayout(text.text, font, frc)
            val textWidth = textLayout.advance.toDouble()
            val ascent = textLayout.ascent.toDouble()
            val descent = textLayout.descent.toDouble()
            val textHeight = ascent + descent

            // アライメント計算
            // 水平: 0=left, 1=center, 2=right
            val alignedX = when (text.alignH) {
                1 -> text.x - textWidth / 2.0
                2 -> text.x - textWidth
                else -> text.x
            }

            // 垂直: 0=baseline, 1=bottom, 2=middle, 3=top
            // Graphics2D の drawString は baseline Y を取るので:
            // baseline → そのまま
            // bottom → ascent 分だけ上
            // middle → ascent - textHeight/2
            // top → 下がる（descenderの分 baseline を下げる）
            val baselineY = when (text.alignV) {
                0 -> text.y             // baseline: そのまま
                1 -> text.y - descent   // bottom: descent 分上がる
                2 -> text.y + ascent - textHeight / 2.0  // middle
                3 -> text.y + ascent    // top: ascent 分下がる
                else -> text.y
            }

            if (text.rotation != 0.0) {
                val savedTransform = g2d.transform
                g2d.rotate(
                    Math.toRadians(-text.rotation),
                    text.x, text.y
                )
                textLayout.draw(g2d, alignedX.toFloat(), baselineY.toFloat())
                g2d.transform = savedTransform
            } else {
                textLayout.draw(g2d, alignedX.toFloat(), baselineY.toFloat())
            }
        }
    }
}
