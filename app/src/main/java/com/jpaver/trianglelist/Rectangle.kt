package com.jpaver.trianglelist

data class Line( val left:PointXY= PointXY(0f,0f), val right:PointXY=PointXY(0f,0f) )
data class Rect( val a:Line=Line(), val b:Line=Line() )

data class Rectangle(
    val length: Float,
    var widthA: Float,
    var widthB: Float,
    var angle:Float=0f,
    var basepoint: PointXY = PointXY(0f,0f),
    var nodeA: Rectangle? = null,
    var side: Int=1
) : EditObject(){

    fun calcPoint( parent:Rectangle?=nodeA ):Rect{
        if(parent!=null){
            basepoint = parent.calcPoint().b.left
            widthA = parent.widthB
            angle = parent.angle
            parent.setNode2(this,side)
        }

        val rightA = basepoint.plus(widthA,0f).rotate( basepoint, angle )
        val leftB  = basepoint.crossOffset( rightA, length )
        val rightB = leftB.crossOffset( basepoint, widthB )

        return Rect( Line(basepoint,rightA), Line(leftB, rightB) )
    }
}