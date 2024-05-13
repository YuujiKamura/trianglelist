package com.jpaver.trianglelist

data class Line( val left:PointXY= PointXY(0f,0f), val right:PointXY=PointXY(0f,0f) ){
    fun getAngle():Float = left.calcAngleWithXAxis(right)
}
data class Line2(val a:Line=Line(), val b:Line=Line() )

data class Rectangle(
    val length: Float,
    var widthA: Float,
    var widthB: Float,
    var angle:Float=0f,
    var basepoint: PointXY = PointXY(0f,0f),
    var nodeA: EditObject? = null,
    var side: Int=1
) : EditObject(){

    fun initByParent(parent: EditObject){
        var baseline = Line()
        when{
            ( parent is Rectangle ) -> {
                baseline = parent.calcPoint().b
            }
            ( parent is Triangle ) -> {
                baseline = parent.getLine(side)
            }
        }

        basepoint = baseline.left
        angle = baseline.getAngle()
        parent.setNode2(this,side)
    }

    fun calcPoint():Line2{
        nodeA?.let { initByParent(it) }

        val rightA = basepoint.plus(widthA,0f).rotate( basepoint, angle )
        val leftB  = basepoint.crossOffset( rightA, length )
        val rightB = leftB.crossOffset( basepoint, widthB )

        return Line2( Line(basepoint,rightA), Line(leftB, rightB) )
    }
}