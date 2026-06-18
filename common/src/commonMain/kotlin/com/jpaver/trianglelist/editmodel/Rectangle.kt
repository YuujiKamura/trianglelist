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

    var dimVertical = DimAligns(1, 1, 1, 1)
    var dimHorizontal = DimAligns(0, 0, 0, 0)
    var dimHeight: Float = 0f
    var dimScale: Float = 1f

    private data class RectangleGeometry(
        val bl: PointXY, val br: PointXY, val tr: PointXY, val tl: PointXY,
        val midA: PointXY, val midC: PointXY,
        val angle: Double,
        // 「描画にいる数字」は全部ここで持つ ── 呼び出し側に向き計算をさせない
        // (yuuji 2026-06-17 指針:「必要な数字は全部エンティティクラス自身がメンバでそろえていれば、ふつうに想起する」)
        val spine: Line,                  // 垂線 (alignment 込み・中央揃えの短辺×30% shift 込み)
        val rightAngleMark: Line2,        // 直角マーカー (spine 起点・内向き 2 本)
        val guideLine: Line?              // alignment=0 では null (B 辺と spine が重なる)
    )

    private var geoCache: RectangleGeometry? = null

    fun calcPoint(): Line2 {
        var baseL = basepoint
        var baseR = basepoint.moveX(widthA, angle)
        var curAngle = angle

        nodeA?.let {
            val bl = initByParent(it, side)
            baseL = bl.left
            baseR = bl.right
            curAngle = bl.getAngle()
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

        // 外向き perpendicular ベクトル: 親有り Rect は親図形の outwardPerpUnit (= 親の環閉合した
        // ラインの集合の signedArea から動的算出) を使う。 独立 Rect (親無し) は親が無いので
        // 「外向き」 概念が無く、 自分の baseline の perpCCW 方向 (= 数学 +y、 画面下) を初期向きと
        // して採る ── これは「外向き規定」 ではなく独立 Rect の初期巡回方向の選択。
        // user 指針 (2026-06-18):「環閉合したデータ群を揃えて初めて周回向きと内側外側が決まる」、
        // 「ラインの集合から外向きを教えてくれる関数を書くことは可能」。 ±90 という数値 symbol は
        // 撤廃。
        val bl = baseL
        val br = baseR
        val dx = (baseR.x - baseL.x).toDouble()
        val dy = (baseR.y - baseL.y).toDouble()
        val safeLen = if (baseLen > 0.0) baseLen else 1.0   // 0 除算ガード (degenerate baseline)
        val outward = nodeA?.outwardPerpUnit(side) ?: PointXY(
            (-dy / safeLen).toFloat(), (dx / safeLen).toFloat()
        )
        val tl = PointXY(
            (topBaseStart.x + outward.x * length).toFloat(),
            (topBaseStart.y + outward.y * length).toFloat()
        )
        val tr = PointXY(
            (tl.x + (dx / safeLen) * widthB).toFloat(),
            (tl.y + (dy / safeLen) * widthB).toFloat()
        )

        val midA = bl.calcMidPoint(br)
        val midC = tl.calcMidPoint(tr)

        // 垂線 spine: alignment 別の起点・終点 (中央揃えは番号サークルを避けて短辺×30% shift)
        val spine = when (alignment) {
            1    -> {
                val baseDir = bl.vectorTo(br).normalize()
                val shift = baseDir.scale(minOf(widthA, widthB) * 0.3)
                Line(midA + shift, midC + shift)
            }
            2    -> Line(br, tr)
            else -> Line(bl, tl)
        }

        // 直角マーカー: spine 起点と上辺方向は確定。内向きは「上辺方向に直交する 2 候補のうち、
        //   centroid 側を向く方」を採る (alignment・親の有無 / crossClockwise の符号によらず常に内向き、
        //   2026-06-18 yuuji 指摘「右揃え/左揃え時に外に飛び出る」の修正)。
        val sqSize = minOf(widthA, widthB, length) * 0.05
        val upDir = spine.left.vectorTo(spine.right).normalize()
        val perpCcw = PointXY(-upDir.y, upDir.x)              // CCW90 of upDir
        val perpCw  = PointXY( upDir.y, -upDir.x)             // CW90  of upDir
        val rectCenter = PointXY(
            ((bl.x + br.x + tr.x + tl.x) * 0.25f),
            ((bl.y + br.y + tr.y + tl.y) * 0.25f)
        )
        val toCenter = spine.left.vectorTo(rectCenter)
        val inDir =
            if (toCenter.x * perpCcw.x + toCenter.y * perpCcw.y > 0f) perpCcw
            else perpCw
        val p1 = spine.left + inDir.scale(sqSize)
        val p3 = spine.left + upDir.scale(sqSize)
        val p2 = p1 + upDir.scale(sqSize)
        val rightAngleMark = Line2(Line(p1, p2), Line(p2, p3))

        val guideLine = if (alignment != 0) spine else null

        geoCache = RectangleGeometry(
            bl, br, tr, tl, midA, midC, curAngle,
            spine, rightAngleMark, guideLine
        )

        return Line2(Line(bl, br), Line(tl, tr))
    }

    private fun geo() = geoCache ?: calcPoint().let { geoCache!! }

    /** 垂線 (spine)。中央揃え時の番号サークル回避 shift 込み、起点は底辺との交点。 */
    fun getSpine(): Line = geo().spine

    /** 直角マーカー (spine と底辺の直交を示す小正方形の 2 辺)。Line2 として「描画する 2 本」を返すだけ。 */
    fun getRightAngleMark(): Line2 = geo().rightAngleMark

    /** 垂線そのもののガイド線。alignment=0 (左寄せ) は B 辺と重なるので null。 */
    fun getGuideLine(): Line? = geo().guideLine

    /** 描画レンダラ向けメタ識別子: 垂線の起点が底辺側 (bl) か上辺側 (tl) か。 */
    val perpFrom: String get() = if (widthA <= widthB) "bl" else "tl"

    override fun getLine(side: Int): Line {
        val g = geo()
        // side index = 環閉合順 (= 一周巡回順) で連続: side 0 終 br = side 1 起 br ✓、 1 終 tr =
        // 2 起 tr ✓、 2 終 tl = 3 起 tl ✓、 3 終 bl = 0 起 bl ✓。 user 指針 (2026-06-18):
        // 「環閉合したデータ群を揃えて初めて周回向きと内側外側が決まる」、「図形によって
        // 周回方向を逆にする意味がない、 これこそ基底クラスで定める性質で、 全ての連結図形が
        // 同一方向に周回する前提をまず定める」。 Triangle.getLine の side 0/1/2 と並ぶ統一規約。
        // 旧側面意味 (1=B 左脚, 2=C 上辺, 3=D 右脚) を環閉合順に再割当 (1=D 右脚, 2=C 上辺,
        // 3=B 左脚)。
        return when (side) {
            0 -> Line(g.bl, g.br) // A 底辺
            1 -> Line(g.br, g.tr) // D 右脚 (環閉合順 2 番目)
            2 -> Line(g.tr, g.tl) // C 上辺 (環閉合順 3 番目)
            3 -> Line(g.tl, g.bl) // B 左脚 (環閉合順 4 番目)
            else -> Line()
        }
    }

    override val sideCount: Int = 4
    override fun vertices(): List<PointXY> {
        val g = geo()
        return listOf(g.bl, g.br, g.tr, g.tl)
    }

    /**
     * Rectangle の新 side 規約 (環閉合順 1=D 右脚, 2=C 上辺, 3=B 左脚) に node スロット結線を対応:
     *   side 1 (D 右脚) → node.d、 side 2 (C 上辺) → node.c、 side 3 (B 左脚) → node.b
     * 親物理意味 (D/C/B) とスロット名 (d/c/b) の対応は維持、 side index だけ環閉合順に再割当。
     */
    override fun setNode2(target: CycleShape, side: Int, side2: Int) {
        when (side) {
            0 -> { target.setNode2(this, side2); node.a = target }
            1 -> { target.node.a = this; node.d = target }  // D 右脚
            2 -> { target.node.a = this; node.c = target }  // C 上辺
            3 -> { target.node.a = this; node.b = target }  // B 左脚
        }
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

    // 段階0 の textSize 配布: Rectangle は dimHeight を保持するだけ (描画時に直接参照)。
    override fun applyDimTextSize(size: Float) { dimHeight = size }

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
