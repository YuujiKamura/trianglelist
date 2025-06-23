package com.jpaver.trianglelist

fun Triangle.pointUnconnectedSide(point: com.example.trilib.PointXY, clockwise: Float): com.example.trilib.PointXY {
    if (nodeC == null) return point.mirroredAndScaledPoint(this.point[0], pointBC, clockwise)
    if (nodeB == null) return point.mirroredAndScaledPoint(pointAB, pointBC, clockwise)
    return point
}

fun Triangle.angleUnconnectedSide(): Float = if (nodeC == null) -angleMmCA else -angleMpAB

fun Triangle.setPointNumber(p: com.example.trilib.PointXY, isUser: Boolean = true) {
    pointnumber = pointNumber.setPointByUser(p, this, isUser)
} 