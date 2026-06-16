package com.jpaver.trianglelist.editmodel

import com.jpaver.trianglelist.viewmodel.formattedString

data class Line(val left: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f), val right: com.example.trilib.PointXY = com.example.trilib.PointXY(
    0f,
    0f
)
){
    fun getAngle():Double = left.calcAngleWithXAxis(right)
}
data class Line2(val a: Line = Line(), val b: Line = Line() )

data class Rectangle(
    val length: Double,
    var widthA: Double,
    var widthB: Double,
    var angle:Double=0.0,
    var basepoint: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f),
    var nodeA: EditObject? = null,
    var side: Int=1,
    // 上辺アライメント (0=左寄せ / 1=中央 / 2=右寄せ)。0 = 従来の左寄せ固定 (後方互換、golden 不変)。
    // 上辺が底辺のどこに寄るか — web の起点(lcr 0左/1中/2右)を同値で流用する (trap-design.md 段3)
    var alignment: Int = 0
) : EditObject(){

    // 寸法アライメント — 三角形と同じ共通の式層 (DimensionLayout) に乗せるための、図形に
    // 貼り付かない純データ (Triangle の Dims が持つ DimAligns と同じ型を直接持つ)。
    // a=底辺A, b=延長/左脚B, c=上辺C (D右脚は寸法なし)。既定は三角形の既定と同値
    // (vertical=外1, horizontal=中央0)。将来 Dims を Triangle から切り離せば共有する (段B)。
    var dimVertical = DimAligns(1, 1, 1, 1)
    var dimHorizontal = DimAligns(0, 0, 0, 0)
    var dimHeight: Float = 0f
    var dimScale: Float = 1f

    fun calcPoint(): Line2 {
        var baseline = Line( basepoint, basepoint.moveX(widthA,angle) )
        var crossClockwise = -90.0

        nodeA?.let {
            var bl = initByParent(it, side)
            // 重なり防止 (Triangle の apexTowardInterior 相当): 
            // Rectangle の crossOffset は常に baseline の「左側」へ伸びる。
            // 親の重心 (centroid) が baseline の「左側」にある場合、親の内部に向かって
            // 伸びて重なってしまうため、垂直方向だけを反転して外側へ向ける。
            // baseline 自体を反転すると「子の底辺 = 親の接続辺」の向き込み契約が壊れ、
            // D 辺接続で子底辺が親 D 辺に乗らなくなる。
            val cp = it.centroid()
            val dx = bl.right.x - bl.left.x
            val dy = bl.right.y - bl.left.y
            val centSide = dx * (cp.y - bl.left.y) - dy * (cp.x - bl.left.x)
            if (centSide > 0.0) {
                crossClockwise = 90.0
            }
            baseline = bl
            basepoint = baseline.left
            angle = baseline.getAngle()
        }

        // 底辺の実長 (接続時は親辺長になる — その底辺に対して上辺を寄せる)。
        // 上辺(widthB)が底辺より短いぶんを align に応じて底辺方向にずらした点を、上辺左端の起点にする。
        // align=0 は offset 0.0 で basepoint と厳密同値 → leftB/rightB の式が元と一致しビット不変。
        val baseLen   = baseline.left.lengthTo( baseline.right )
        val baseAngle = baseline.getAngle()
        val alignShift = when (alignment) {
            1 -> (baseLen - widthB) / 2.0   // 中央: 左右対称
            2 -> baseLen - widthB           // 右寄せ: 上辺右端が底辺右端の真上
            else -> 0.0                     // 左寄せ (従来): 上辺左端が底辺左端の真上
        }
        val topBase = basepoint.offset( alignShift, baseAngle )

        val leftB  = topBase.crossOffset( baseline.right, length, crossClockwise )
        val rightB = leftB.crossOffset( topBase, widthB )

        return Line2( baseline, Line(leftB, rightB) )
    }

    /**
     * side で 4 辺を取り出す (Triangle.getLine(side) と同じ side→Line 契約)。
     * calcPoint() の頂点 lp.a=底辺(bl→br), lp.b=上辺(tl→tr) を組み替える。
     * 物理辺マッピング (WebPrimitiveRenderer.kt:184-207 の寸法ラベルが出典、trap-design.md 段4):
     *   0 = A 底辺   (bl→br = lp.a)            — 親と共有、子には出さない
     *   1 = B 延長/左脚 (bl→tl)               — レンダラが「延長B」と寸法表示する脚
     *   2 = C 上辺   (tl→tr = lp.b)
     *   3 = D 右脚   (br→tr)                  — 現状 4 本目として線だけ引かれる脚
     * calcPoint() は nodeA があると basepoint/angle を再構築する副作用を持つが、毎回 nodeA から
     * baseline を作り直すので複数回呼んでも同値 (R2 で確認済)。getLine から呼ぶのは安全。
     */
    override fun getLine(side: Int): Line {
        val lp = calcPoint()
        return when (side) {
            0 -> Line(lp.a.left,  lp.a.right)   // A 底辺 (bl→br)
            1 -> Line(lp.a.left,  lp.b.left)    // B 延長/左脚 (bl→tl)
            2 -> Line(lp.b.left,  lp.b.right)   // C 上辺 (tl→tr)
            3 -> Line(lp.a.right, lp.b.right)   // D 右脚 (br→tr)
            else -> Line()
        }
    }

    // EditObject の多態 (user 指針 2026-06-14)。混在リストは sideCount / vertices で kind 分岐なしに走る。
    override val sideCount: Int = 4
    override fun vertices(): List<com.example.trilib.PointXY> {
        val lp = calcPoint()
        return listOf(lp.a.left, lp.a.right, lp.b.right, lp.b.left)
    }

    /**
     * SoT 一本化 段3 寸法多態 (2026-06-15): 既存 WebPrimitiveRenderer.renderRectangle の寸法生成
     * (A底辺 / C上辺 / D右脚 / B垂線延長) を移植したもの。renderer 側の kind 分岐を消す。
     * 接続済み (nodeA != null) は A 底辺寸法を出さない (= 親辺と共有、親側が寸法を持つ思想)。
     * B 延長は底辺/上辺の短い方を起点に取り、台形内側に向けた垂線長 (rect.length)。
     * 注: meta(perp 起点) と guide(alignment != 0 のときの垂線描画) は renderer 側に残し、
     *     ここでは寸法 spec (text + place + 旗揚げ判定) のみ返す。
     */
    override fun emitDimensionSpecs(scale: Float): List<DimensionSpec> {
        val lp = calcPoint()
        val bl = lp.a.left; val br = lp.a.right; val tl = lp.b.left; val tr = lp.b.right
        val ds = dimScale.toDouble()
        val dh = dimHeight.toDouble()
        val specs = mutableListOf<DimensionSpec>()

        fun spec(side: Int, start: com.example.trilib.PointXY, end: com.example.trilib.PointXY, v: Int, h: Int): DimensionSpec {
            val place = com.jpaver.trianglelist.label.DimensionLayout.layout(end, start, v, h, ds, dh, 0.0)
            val len = (start.lengthTo(end) / scale).toFloat()
            return DimensionSpec(side, len.formattedString(2), place, start.calcDimAngle(end), h, v, h > 2)
        }

        if (nodeA == null) specs.add(spec(0, bl, br, dimVertical.a, dimHorizontal.a))
        specs.add(spec(2, tr, tl, dimVertical.c, dimHorizontal.c))
        specs.add(spec(3, br, tr, 1, 0))

        // B 延長 — 底辺/上辺の短い方を起点に、台形内側へ向く垂線
        val bottomShorter = widthA <= widthB
        val baseStart = if (bottomShorter) bl else tl
        val baseEnd = if (bottomShorter) br else tr
        val perpFoot = baseStart.crossOffset(baseEnd, length, if (bottomShorter) -90.0 else 90.0)
        val placeB = com.jpaver.trianglelist.label.DimensionLayout.layout(perpFoot, baseStart, dimVertical.b, dimHorizontal.b, ds, dh, 0.0)
        val extLen = (length / scale).toFloat()
        specs.add(DimensionSpec(1, extLen.formattedString(2), placeB, baseStart.calcDimAngle(perpFoot), dimHorizontal.b, dimVertical.b, dimHorizontal.b > 2))
        return specs
    }

    /**
     * 自身の 4 頂点で bounds を拡張 (union) して返す。
     * Triangle.expandBoundaries (TriangleExtensions.kt:152-160) と同じ Y 規約:
     *   left=minX, right=maxX, top=maxY, bottom=minY
     * union: left=min, top=max, right=max, bottom=min
     */
    fun expandBoundaries(listBound: com.jpaver.trianglelist.Bounds): com.jpaver.trianglelist.Bounds {
        val verts = vertices()
        if (verts.isEmpty()) return listBound

        var myLeft   = verts[0].x.toDouble()
        var myTop    = verts[0].y.toDouble()
        var myRight  = verts[0].x.toDouble()
        var myBottom = verts[0].y.toDouble()
        for (v in verts) {
            val vx = v.x.toDouble()
            val vy = v.y.toDouble()
            if (vx < myLeft)   myLeft   = vx
            if (vx > myRight)  myRight  = vx
            if (vy < myBottom) myBottom = vy
            if (vy > myTop)    myTop    = vy
        }

        return com.jpaver.trianglelist.Bounds(
            left   = minOf(myLeft,   listBound.left),
            top    = maxOf(myTop,    listBound.top),
            right  = maxOf(myRight,  listBound.right),
            bottom = minOf(myBottom, listBound.bottom),
        )
    }

    /**
     * center 周りに degrees 回転する (独立台形用)。
     * nodeA != null (親接続あり) の場合は no-op。
     * 親接続ありの Rectangle は calcPoint() 内の initByParent が basepoint/angle を
     * 親から再構築するため、ここで書き換えても次の calcPoint() で上書きされる。
     */
    fun rotateBy(center: com.example.trilib.PointXY, degrees: Float) {
        if (nodeA != null) return
        basepoint = basepoint.rotate(center, degrees.toDouble())
        angle += degrees
    }
}
