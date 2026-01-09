package com.jpaver.trianglelist.editmodel

data class Line(val left: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f), val right: com.example.trilib.PointXY = com.example.trilib.PointXY(
    0f,
    0f
)
){
    fun getAngle():Float = left.calcAngleWithXAxis(right)
}
data class Line2(val a: Line = Line(), val b: Line = Line() )

data class Rectangle(
    val length: Float,
    var widthA: Float,
    var widthB: Float,
    var angle:Float=0f,
    var basepoint: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f),
    var nodeA: EditObject? = null,
    var side: Int=1
) : EditObject(){

    fun calcPoint(): Line2 {
        var baseline = Line( basepoint, basepoint.moveX(widthA,angle) )

        nodeA?.let {
            baseline = initByParent(it, side)
            basepoint = baseline.left
            angle = baseline.getAngle()
        }

        val leftB  = basepoint.crossOffset( baseline.right, length )
        val rightB = leftB.crossOffset( basepoint, widthB )

        return Line2( baseline, Line(leftB, rightB) )
    }
}