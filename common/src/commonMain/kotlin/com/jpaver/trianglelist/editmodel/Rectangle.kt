package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import com.jpaver.trianglelist.Bounds
import com.jpaver.trianglelist.viewmodel.formattedString
import kotlin.math.PI
import kotlin.math.cos

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

    var widthA: Double = widthA
        get() = nodeA?.let { initByParent(it, side).length } ?: field
        set(value) { field = value; geoCache = null }

    var widthB: Double = widthB; set(value) { field = value; geoCache = null }

    var angle: Double = angle
        get() = nodeA?.let { initByParent(it, side).getAngle() } ?: field
        set(value) { field = value; geoCache = null }

    var basepoint: PointXY = basepoint
        get() = nodeA?.let { initByParent(it, side).left } ?: field
        set(value) { field = value; geoCache = null }

    var nodeA: CycleShape? = nodeA; set(value) { field = value; geoCache = null }
    var side: Int = side; set(value) { field = value; geoCache = null }
    var alignment: Int = alignment; set(value) { field = value; geoCache = null }

    var dimVertical = DimAligns(1, 1, 1)
    var dimHorizontal = DimAligns(0, 0, 0, 0)
    var dimHeight = 0f
    var dimScale = 1f

    data class RectangleGeometry(
        val pointDA: PointXY, val pointAB: PointXY, val pointBC: PointXY, val pointCD: PointXY,
        val midA: PointXY, val midC: PointXY, val angle: Double,
        val spine: Line, val rightAngleMark: Line2, val guideLine: Line?
    )

    private var geoCache: RectangleGeometry? = null

    /** 幾何形状の再計算。副作用なし。 */
    private fun updateGeometry(): RectangleGeometry {
        val ptDA: PointXY
        val ptAB: PointXY
        val curAngle: Double
        val effectiveWidthA = widthA

        val parent = nodeA
        if (parent != null) {
            // 親接続時は、ゲッターから返る basepoint=DA, angle=(DA->ABの方向) をそのまま使用する
            ptDA = basepoint
            curAngle = angle
            ptAB = ptDA.moveX(effectiveWidthA, curAngle)
        } else {
            // 独立時のみ、 angle の向きによって起点と終点の左右割当を切り替える
            val rad = angle / 180.0 * PI
            if (cos(rad) >= 0.0) {
                // 右向き (angle = 0 など) : 起点 = 左下 (ptAB), 終点 = 右下 (ptDA)
                ptAB = basepoint
                ptDA = basepoint.moveX(effectiveWidthA, angle)
                curAngle = angle + 180.0 // DA -> AB は逆方向
            } else {
                // 左向き (angle = 180 など) : 起点 = 右下 (ptDA), 終点 = 左下 (ptAB)
                ptDA = basepoint
                ptAB = basepoint.moveX(effectiveWidthA, angle)
                curAngle = angle
            }
        }

        // 2. 成長方向 (垂直方向) の単位ベクトルを決定
        val rawPerp = ptDA.crossOffset(ptAB, height, 90.0) - ptDA
        val perp = if (nodeA != null && rawPerp.innerProduct(nodeA!!.outwardPerpUnit(side)) < 0.0) {
            ptDA.crossOffset(ptAB, height, -90.0) - ptDA
        } else {
            rawPerp
        }

        // 3. 上辺の頂点を決定 (BC -> CD)
        val xShift = when (alignment) {
            1 -> (effectiveWidthA + widthB) / 2.0 // 中央寄せ
            2 -> widthB                          // 右寄せ
            else -> effectiveWidthA              // 左寄せ
        }
        val ptBC = ptDA.offset(ptAB, xShift) + perp
        val ptCD = ptBC + ptDA.vectorTo(ptAB).normalize().scale(-widthB)

        // 4. 垂線 (spine)
        val midA = ptDA.calcMidPoint(ptAB)
        val midC = ptBC.calcMidPoint(ptCD)
        val spine = when (alignment) {
            1 -> {
                val shift = ptDA.vectorTo(ptAB).normalize().scale(minOf(effectiveWidthA, widthB) * 0.3)
                Line(midA + shift, midC + shift)
            }
            2 -> Line(ptDA, ptCD)
            else -> Line(ptAB, ptBC)
        }

        // 5. 直角マーカー
        val sqSize = minOf(effectiveWidthA, widthB, height) * 0.05
        val up = spine.left.vectorTo(spine.right).normalize()
        val center = PointXY((ptDA.x + ptAB.x + ptBC.x + ptCD.x) * 0.25, (ptDA.y + ptAB.y + ptBC.y + ptCD.y) * 0.25)
        val toCenter = spine.left.vectorTo(center)
        val rightDir = PointXY(up.y, -up.x)
        val inDir = if (toCenter.innerProduct(rightDir) > 0.0) rightDir else PointXY(-up.y, up.x)
        
        val p1 = spine.left + inDir.scale(sqSize)
        val p3 = spine.left + up.scale(sqSize)
        val p2 = p1 + up.scale(sqSize)
        val rightAngleMark = Line2(Line(p1, p2), Line(p2, p3))

        return RectangleGeometry(
            pointDA = ptDA, pointAB = ptAB, pointBC = ptBC, pointCD = ptCD,
            midA = midA, midC = midC, angle = curAngle,
            spine = spine, rightAngleMark = rightAngleMark,
            guideLine = if (alignment != 0) spine else null
        )
    }

    private fun geo() = geoCache ?: updateGeometry().also { geoCache = it }

    /** 座標のペア (底辺 Line(bl,br) と 上辺 Line(tl,tr)) を返す。 互換性維持のためのメソッド。 */
    fun calcPoint(): Line2 {
        val g = geo()
        return Line2(Line(g.pointAB, g.pointDA), Line(g.pointBC, g.pointCD))
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
            0 -> Line(g.pointDA, g.pointAB) // A 底辺 (DA -> AB)
            1 -> Line(g.pointAB, g.pointBC) // B 左脚 (AB -> BC)
            2 -> Line(g.pointBC, g.pointCD) // C 上辺 (BC -> CD)
            3 -> Line(g.pointCD, g.pointDA) // D 右脚 (CD -> DA)
            else -> Line()
        }
    }

    override val sideCount: Int = 4
    
    /** 頂点列 (CW 巡回)。 br 起点。 */
    override fun vertices(): List<PointXY> {
        val g = geo()
        return listOf(g.pointDA, g.pointAB, g.pointBC, g.pointCD) // CW
    }

    /** 環閉合 (isClosed) を成立させる巡回順の Line リスト。 A -> B -> C -> D */
    override fun edges(): List<Line> = listOf(getLine(0), getLine(1), getLine(2), getLine(3))

    override fun emitDimensionSpecs(scale: Float): List<DimensionSpec> {
        val g = geo()
        val ds = dimScale.toDouble()
        val dh = dimHeight.toDouble()
        val specs = mutableListOf<DimensionSpec>()

        fun spec(side: Int, line: Line, v: Int, h: Int): DimensionSpec {
            val place = com.jpaver.trianglelist.label.DimensionLayout.layout(line.right, line.left, v, h, ds, dh, 0.0)
            val len = (line.left.lengthTo(line.right) / scale).toFloat()
            return DimensionSpec(side, len.formattedString(2), place, line.left.calcDimAngle(line.right), h, v, h > 2)
        }

        if (nodeA == null) specs.add(spec(0, Line(g.pointDA, g.pointAB), dimVertical.a, dimHorizontal.a))
        specs.add(spec(2, Line(g.pointBC, g.pointCD), dimVertical.c, dimHorizontal.c))

        val spine = getSpine()
        val placeB = com.jpaver.trianglelist.label.DimensionLayout.layout(spine.right, spine.left, dimVertical.b, dimHorizontal.b, ds, dh, 0.0)
        val extLen = (height / scale).toFloat()
        specs.add(DimensionSpec(1, extLen.formattedString(2), placeB, spine.left.calcDimAngle(spine.right), dimHorizontal.b, dimVertical.b, dimHorizontal.b > 2))
        return specs
    }

    override fun centroid(): PointXY {
        val g = geo()
        return PointXY((g.pointDA.x + g.pointAB.x + g.pointBC.x + g.pointCD.x) * 0.25, (g.pointDA.y + g.pointAB.y + g.pointBC.y + g.pointCD.y) * 0.25)
    }

    override fun applyDimTextSize(size: Float) {
        dimHeight = size
        geoCache = null
    }

    fun expandBoundaries(bounds: Bounds): Bounds {
        val g = geo()
        val vs = listOf(g.pointDA, g.pointAB, g.pointBC, g.pointCD)
        var left = bounds.left; var right = bounds.right
        var top = bounds.top; var bottom = bounds.bottom
        for (v in vs) {
            left = minOf(left, v.x); right = maxOf(right, v.x)
            top = maxOf(top, v.y); bottom = minOf(bottom, v.y)
        }
        return Bounds(left, top, right, bottom)
    }

    fun rotateBy(center: PointXY, degrees: Float) {
        if (nodeA != null) return
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
