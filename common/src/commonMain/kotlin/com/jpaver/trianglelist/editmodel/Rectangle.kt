package com.jpaver.trianglelist.editmodel

import com.jpaver.trianglelist.viewmodel.formattedString
import com.example.trilib.PointXY

data class Line(val left: PointXY = PointXY(0f, 0f), val right: PointXY = PointXY(0f, 0f)){
    fun getAngle():Double = left.calcAngleWithXAxis(right)
}
data class Line2(val a: Line = Line(), val b: Line = Line() )

class Rectangle(
    val length: Double,
    widthA: Double,
    widthB: Double,
    angle: Double = 0.0,
    basepoint: PointXY = PointXY(0f, 0f),
    nodeA: EditObject? = null,
    side: Int = 1,
    alignment: Int = 0
) : EditObject() {

    var widthA: Double = widthA; set(value) { field = value; geoCache = null }
    var widthB: Double = widthB; set(value) { field = value; geoCache = null }
    var angle: Double = angle; set(value) { field = value; geoCache = null }
    var basepoint: PointXY = basepoint; set(value) { field = value; geoCache = null }
    var nodeA: EditObject? = nodeA; set(value) { field = value; geoCache = null }
    var side: Int = side; set(value) { field = value; geoCache = null }
    var alignment: Int = alignment; set(value) { field = value; geoCache = null }

    var dimVertical = DimAligns(1, 1, 1, 1)
    var dimHorizontal = DimAligns(0, 0, 0, 0)
    var dimHeight: Float = 0f
    var dimScale: Float = 1f

    private data class RectangleGeometry(
        val bl: PointXY, val br: PointXY, val tr: PointXY, val tl: PointXY,
        val midA: PointXY, val midC: PointXY,
        val angle: Double
    )

    private var geoCache: RectangleGeometry? = null

    fun calcPoint(): Line2 {
        var baseL = basepoint
        var baseR = basepoint.moveX(widthA, angle)
        var curAngle = angle
        var crossClockwise = -90.0  // 独立: 親重心が無いので -90 (右側へ展開)

        nodeA?.let {
            // initByParent は親辺を反転 (forward.right, forward.left) して返す。
            // 反転後の baseline に対して +90 (反時計回り) で外向き = 親の重心と反対側へ展開。
            val bl = initByParent(it, side)
            baseL = bl.left
            baseR = bl.right
            curAngle = bl.getAngle()
            crossClockwise = 90.0

            basepoint = baseL
            angle = curAngle
        }

        val baseLen = baseL.lengthTo(baseR)
        val alignShift = when (alignment) {
            1 -> (baseLen - widthB) / 2.0
            2 -> baseLen - widthB
            else -> 0.0
        }
        val topBaseStart = baseL.offset(baseR, alignShift)

        val bl = baseL
        val br = baseR
        val tl = topBaseStart.crossOffset(baseR, length, crossClockwise)
        val tr = tl.crossOffset(topBaseStart, widthB, crossClockwise)

        val midA = bl.calcMidPoint(br)
        val midC = tl.calcMidPoint(tr)

        geoCache = RectangleGeometry(bl, br, tr, tl, midA, midC, curAngle)

        return Line2(Line(bl, br), Line(tl, tr))
    }

    private fun geo() = geoCache ?: calcPoint().let { geoCache!! }

    fun getSpine(): Line {
        val g = geo()
        return when (alignment) {
            1    -> Line(g.midA, g.midC)
            2    -> Line(g.br, g.tr)
            else -> Line(g.bl, g.tl)
        }
    }

    override fun getLine(side: Int): Line {
        val g = geo()
        return when (side) {
            0 -> Line(g.bl, g.br) // A 底辺 (bl→br)
            1 -> Line(g.bl, g.tl) // B 左脚/高さ (bl→tl)
            2 -> Line(g.tl, g.tr) // C 上辺 (tl→tr)
            3 -> Line(g.tr, g.br) // D 右脚 (tr→br)
            else -> Line()
        }
    }

    override val sideCount: Int = 4
    override fun vertices(): List<PointXY> {
        val g = geo()
        return listOf(g.bl, g.br, g.tr, g.tl)
    }

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

        if (nodeA == null) specs.add(spec(0, Line(g.bl, g.br), dimVertical.a, dimHorizontal.a))
        specs.add(spec(2, Line(g.tr, g.tl), dimVertical.c, dimHorizontal.c))

        val spine = getSpine()
        val placeB = com.jpaver.trianglelist.label.DimensionLayout.layout(spine.right, spine.left, dimVertical.b, dimHorizontal.b, ds, dh, 0.0)
        val extLen = (length / scale).toFloat()
        specs.add(DimensionSpec(1, extLen.formattedString(2), placeB, spine.left.calcDimAngle(spine.right), dimHorizontal.b, dimVertical.b, dimHorizontal.b > 2))
        // 斜辺 (D 右脚 / 中央揃え時の左脚 B) は寸法を出さない (底辺・上辺・垂線のみ、過去指摘 2026-06-17)

        return specs
    }

    override fun centroid(): PointXY {
        val g = geo()
        return PointXY(((g.bl.x + g.br.x + g.tr.x + g.tl.x) / 4).toFloat(), ((g.bl.y + g.br.y + g.tr.y + g.tl.y) / 4).toFloat())
    }

    fun expandBoundaries(listBound: com.jpaver.trianglelist.Bounds): com.jpaver.trianglelist.Bounds {
        val g = geo()
        val vs = listOf(g.bl, g.br, g.tr, g.tl)

        var myLeft   = vs[0].x
        var myTop    = vs[0].y
        var myRight  = vs[0].x
        var myBottom = vs[0].y
        for (v in vs) {
            if (v.x < myLeft)   myLeft   = v.x
            if (v.x > myRight)  myRight  = v.x
            if (v.y < myBottom) myBottom = v.y
            if (v.y > myTop)    myTop    = v.y
        }

        return com.jpaver.trianglelist.Bounds(
            left   = minOf(myLeft,   listBound.left),
            top    = maxOf(myTop,    listBound.top),
            right  = maxOf(myRight,  listBound.right),
            bottom = minOf(myBottom, listBound.bottom),
        )
    }

    fun rotateBy(center: PointXY, degrees: Float) {
        if (nodeA != null) return
        basepoint = basepoint.rotate(center, degrees.toDouble())
        angle += degrees
    }
}
