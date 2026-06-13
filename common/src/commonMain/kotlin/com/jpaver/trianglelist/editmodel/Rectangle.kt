package com.jpaver.trianglelist.editmodel

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

    fun calcPoint(): Line2 {
        var baseline = Line( basepoint, basepoint.moveX(widthA,angle) )

        nodeA?.let {
            baseline = initByParent(it, side)
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

        val leftB  = topBase.crossOffset( baseline.right, length )
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
    fun getLine(side: Int): Line {
        val lp = calcPoint()
        return when (side) {
            0 -> Line(lp.a.left,  lp.a.right)   // A 底辺 (bl→br)
            1 -> Line(lp.a.left,  lp.b.left)    // B 延長/左脚 (bl→tl)
            2 -> Line(lp.b.left,  lp.b.right)   // C 上辺 (tl→tr)
            3 -> Line(lp.a.right, lp.b.right)   // D 右脚 (br→tr)
            else -> Line()
        }
    }
}