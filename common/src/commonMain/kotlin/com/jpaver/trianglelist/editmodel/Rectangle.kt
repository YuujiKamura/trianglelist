package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import com.jpaver.trianglelist.Bounds
import com.jpaver.trianglelist.viewmodel.formattedString

class Rectangle(
    val height: Double,  // 垂線方向の延長 (旧 length)
    widthA: Double,
    widthB: Double,
    angle: Double = 0.0,
    basepoint: PointXY = PointXY(0f, 0f),
    nodeA: CycleShape? = null,
    side: Int = 1,
    alignment: Int = 0
) : CycleShape() {

    var widthA: Double = widthA; set(value) { field = value; geoCache = null }
    var widthB: Double = widthB; set(value) { field = value; geoCache = null }
    var angle: Double = angle; set(value) { field = value; geoCache = null }
    var basepoint: PointXY = basepoint; set(value) { field = value; geoCache = null }
    var nodeA: CycleShape? = nodeA; set(value) { field = value; geoCache = null }
    var side: Int = side; set(value) { field = value; geoCache = null }
    var alignment: Int = alignment; set(value) { field = value; geoCache = null }

    var dimVertical = DimAligns(1, 1, 1)
    var dimHorizontal = DimAligns(0, 0, 0, 0)
    var dimHeight = 0f
    var dimScale = 1f

    data class RectangleGeometry(
        val pointBR: PointXY, val pointBL: PointXY, val pointTL: PointXY, val pointTR: PointXY,
        val midA: PointXY, val midC: PointXY, val angle: Double,
        val spine: Line, val rightAngleMark: Line2, val guideLine: Line?
    )

    private var geoCache: RectangleGeometry? = null

    /** 幾何形状の再計算。 br (basepoint) 起点・時計回り (CW) 規約。 */
    private fun updateGeometry(): RectangleGeometry {
        // 1. 基点 br と 角度の確定
        var br = basepoint
        var curAngle = angle
        
        nodeA?.let {
            val initLine = initByParent(it, side)
            // initByParent は親辺(CW)を反転した Line(forward.right, forward.left) を返す。
            // 子 baseline の物理的な起点 (br) としてそのまま受ける。
            br = initLine.left
            curAngle = initLine.getAngle()
            basepoint = br
            angle = curAngle
            widthA = initLine.length
        }
        // 2. 底辺の終点 bl を確定 (br -> bl)
        val bl = br.moveX(widthA, curAngle)

        // 3. 成長方向 (perp) の確定
        // br -> bl の進行方向の「右手 (+90度)」が図形の内側。
        // 独立時 (angle=180): br から左に歩き、右手に成長 = y軸プラス (上) 向き。
        var perp = br.crossOffset(bl, height, 90.0) - br
        
        nodeA?.let { p ->
            val outward = p.outwardPerpUnit(side)
            // 親接続時、現在の成長方向が親の外向きベクトルと逆なら反転。
            if (perp.innerProduct(outward) < 0.0) {
                perp = br.crossOffset(bl, height, -90.0) - br
            }
        }

        // 4. 上辺 tl -> tr の配置 (alignment 考慮)
        // br(0) -> bl(widthA) の軸上で、tl の位置を決定する。
        val xShift = when (alignment) {
            1 -> (widthA + widthB) / 2.0 // 中央寄せ
            2 -> widthB                  // 右寄せ (tr が br の真上)
            else -> widthA               // 左寄せ (tl が bl の真上)
        }
        val tl = br.offset(bl, xShift) + perp
        val tr = tl + br.vectorTo(bl).normalize().scale(-widthB) // bl->br 方向へ widthB

        val midA = br.calcMidPoint(bl)
        val midC = tl.calcMidPoint(tr)

        // 5. 垂線 (spine)
        val spine = when (alignment) {
            1 -> {
                val shift = br.vectorTo(bl).normalize().scale(minOf(widthA, widthB) * 0.3)
                Line(midA + shift, midC + shift)
            }
            2 -> Line(br, tr)
            else -> Line(bl, tl)
        }

        // 6. 直角マーカー
        val sqSize = minOf(widthA, widthB, height) * 0.05
        val up = spine.left.vectorTo(spine.right).normalize()
        val center = PointXY((br.x + bl.x + tl.x + tr.x) * 0.25, (br.y + bl.y + tl.y + tr.y) * 0.25)
        val toCenter = spine.left.vectorTo(center)
        val rightDir = PointXY(up.y, -up.x)
        val inDir = if (toCenter.innerProduct(rightDir) > 0.0) rightDir else PointXY(-up.y, up.x)
        
        val p1 = spine.left + inDir.scale(sqSize)
        val p3 = spine.left + up.scale(sqSize)
        val p2 = p1 + up.scale(sqSize)
        val rightAngleMark = Line2(Line(p1, p2), Line(p2, p3))

        return RectangleGeometry(br, bl, tl, tr, midA, midC, curAngle, spine, rightAngleMark, if (alignment != 0) spine else null)
            .also { geoCache = it }
    }

    private fun geo() = geoCache ?: updateGeometry()

    /** 座標のペア (底辺 Line(bl,br) と 上辺 Line(tl,tr)) を返す。 互換性維持のためのメソッド。 */
    fun calcPoint(): Line2 {
        val g = geo()
        // 外部 (DrawingFileWriter) は bl, br 順を期待しているため、幾何学的な bl, br を渡す
        return Line2(Line(g.pointBL, g.pointBR), Line(g.pointTL, g.pointTR))
    }

    fun getSpine(): Line = geo().spine
    fun getRightAngleMark(): Line2 = geo().rightAngleMark
    fun getGuideLine(): Line? = geo().guideLine

    var perpFrom: String = "B"
        get() = if (alignment == 2) "D" else "B"

    override fun getLine(side: Int): Line {
        val g = geo()
        // CW 巡回順の辺 (A -> B -> C -> D)
        return when (side) {
            0 -> Line(g.pointBR, g.pointBL) // A 底辺 (br -> bl)
            1 -> Line(g.pointBL, g.pointTL) // B 左脚 (bl -> tl)
            2 -> Line(g.pointTL, g.pointTR) // C 上辺 (tl -> tr)
            3 -> Line(g.pointTR, g.pointBR) // D 右脚 (tr -> br)
            else -> Line()
        }
    }

    override val sideCount: Int = 4
    
    /** 頂点列 (CW 巡回)。 br 起点。 */
    override fun vertices(): List<PointXY> {
        val g = geo()
        return listOf(g.pointBR, g.pointBL, g.pointTL, g.pointTR) // CW
    }

    /** 環閉合 (isClosed) を成立させる巡回順の Line リスト。 A -> B -> C -> D */
    override fun edges(): List<Line> = listOf(getLine(0), getLine(1), getLine(2), getLine(3))

    override fun emitDimensionSpecs(scale: Float): List<DimensionSpec> {
        val g = geo()
        val ds = dimScale.toDouble()
        val dh = dimHeight.toDouble()
        val specs = mutableListOf<DimensionSpec>()

        fun spec(side: Int, line: Line, v: Int, h: Int): DimensionSpec {
            // layout は「右手」を内側と判定。 CW 辺では右手=内側なので、そのまま渡す。
            val place = com.jpaver.trianglelist.label.DimensionLayout.layout(line.right, line.left, v, h, ds, dh, 0.0)
            val len = (line.left.lengthTo(line.right) / scale).toFloat()
            return DimensionSpec(side, len.formattedString(2), place, line.left.calcDimAngle(line.right), h, v, h > 2)
        }

        if (nodeA == null) specs.add(spec(0, Line(g.pointBR, g.pointBL), dimVertical.a, dimHorizontal.a))
        specs.add(spec(2, Line(g.pointTL, g.pointTR), dimVertical.c, dimHorizontal.c))

        val spine = getSpine()
        val placeB = com.jpaver.trianglelist.label.DimensionLayout.layout(spine.right, spine.left, dimVertical.b, dimHorizontal.b, ds, dh, 0.0)
        val extLen = (height / scale).toFloat()
        specs.add(DimensionSpec(1, extLen.formattedString(2), placeB, spine.left.calcDimAngle(spine.right), dimHorizontal.b, dimVertical.b, dimHorizontal.b > 2))
        return specs
    }

    override fun centroid(): PointXY {
        val g = geo()
        return PointXY((g.pointBR.x + g.pointBL.x + g.pointTL.x + g.pointTR.x) * 0.25, (g.pointBR.y + g.pointBL.y + g.pointTL.y + g.pointTR.y) * 0.25)
    }

    override fun applyDimTextSize(size: Float) {
        dimHeight = size
        geoCache = null
    }

    fun expandBoundaries(bounds: Bounds): Bounds {
        val g = geo()
        val vs = listOf(g.pointBR, g.pointBL, g.pointTL, g.pointTR)
        var left = bounds.left; var right = bounds.right
        var top = bounds.top; var bottom = bounds.bottom
        for (v in vs) {
            left = minOf(left, v.x); right = maxOf(right, v.x)
            top = maxOf(top, v.y); bottom = minOf(bottom, v.y)
        }
        return Bounds(left, top, right, bottom)
    }

    fun rotateBy(center: PointXY, degrees: Float) {
        basepoint = basepoint.rotate(center, degrees.toDouble())
        angle += degrees.toDouble()
        geoCache = null
    }
}

class Line(val left: PointXY = PointXY(0f, 0f), val right: PointXY = PointXY(0f, 0f)) {
    fun getAngle(): Double = left.calcAngleWithXAxis(right)
    val length = left.lengthTo(right)
}

data class Line2(val a: Line, val b: Line)
