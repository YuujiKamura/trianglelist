package com.jpaver.trianglelist

data class Line( val left:PointXY= PointXY(0f,0f), val right:PointXY=PointXY(0f,0f) ){
    fun calcPoint(length: Float):Line{
        return Line( left, left.offset( PointXY(length,0f), length ) )
    }
}

data class Rectangle(
    var length: Float = 0f,
    var widthA: Float = 0f,
    var widthB: Float = 0f,
    var base: Line = Line().calcPoint(widthA),
) : EditObject(){
    fun calcPoint():Line{
        val leftB  = base.left.crossOffset( base.right, length )
        val rightB = leftB.crossOffset( base.left, widthB )
        return Line(leftB,rightB)
    }
}