package com.jpaver.trianglelist

data class Line( val left:PointXY= PointXY(0f,0f), val right:PointXY=PointXY(0f,0f) )
data class Rect( val a:Line=Line(), val b:Line=Line() )

data class Rectangle(
    val length: Float,
    val widthA: Float,
    val widthB: Float,
    val angle:Float,
    val basepoint: PointXY = PointXY(0f,0f),
) : EditObject(){
    fun calcPoint():Rect{
        val rightA = basepoint.plus(widthA,0f).rotate( basepoint, angle )
        val leftB  = basepoint.crossOffset( rightA, length )
        val rightB = leftB.crossOffset( basepoint, widthB )

        return Rect( Line(basepoint,rightA), Line(leftB, rightB) )
    }
}