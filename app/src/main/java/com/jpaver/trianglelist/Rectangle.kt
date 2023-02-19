package com.jpaver.trianglelist

class Rectangle(
    var length: Float = 0f,
    var widthA: Float = 0f,
    var widthB: Float = 0f,
    var point0: PointXY = PointXY(
        0f,
        0f
    ),
    var angle: Float = 0f,
    var center: Int = 0
) : EditObject(){
}