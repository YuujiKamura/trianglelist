package com.jpaver.trianglelist.util

import com.example.trilib.PointXY

data class InputParameter (var name: String = "",
                   var type: String = "",
                   var number: Int = 0,
                   var a: Float = 0f,
                   var b: Float = 0f,
                   var c: Float = 0f,
                   var pn: Int = 0,
                   var pl: Int = 0,
                   var point: PointXY = PointXY(
                       0f,
                       0f
                   ),
                   var pointflag: PointXY = PointXY(
                       0f,
                       0f
                   )
)