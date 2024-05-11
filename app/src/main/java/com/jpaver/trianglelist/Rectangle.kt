package com.jpaver.trianglelist

data class Line( val left:PointXY= PointXY(0f,0f), val right:PointXY=PointXY(0f,0f) )

data class Rectangle(
    val length: Float,
    val widthA: Float,
    val widthB: Float,
    val angle:Float,
    val base: PointXY = PointXY(0f,0f),
) : EditObject(){
    fun calcPoint():Line{
        val leftB  = base.crossOffset( base.offset( base.plus(widthA,0f), widthA ), length )
        val rightB = leftB.crossOffset( base, widthB )
        val rotatedRightB = rightB.rotate( base, angle )
        return Line(leftB,rotatedRightB)
    }
}