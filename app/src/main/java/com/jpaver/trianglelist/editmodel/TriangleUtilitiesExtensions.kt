package com.jpaver.trianglelist

import com.jpaver.trianglelist.editmodel.ConnParam
import com.jpaver.trianglelist.editmodel.ConnectionSide
import com.jpaver.trianglelist.editmodel.Line
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.angleBySide
import com.jpaver.trianglelist.editmodel.isValidLengthes
import com.jpaver.trianglelist.editmodel.pointBySide
import com.jpaver.trianglelist.editmodel.sideEnum

fun Triangle.toStrings(
    ispoints: Boolean = true,
    islength: Boolean = true,
    isnode: Boolean = true,
    isalign: Boolean = true
): String {
    val connectedTriangles = arrayOf(nodeA, nodeB, nodeC)
    val sb = StringBuilder()
    sb.append("Triangle ${mynumber}         hash:${System.identityHashCode(this)} valid:${isValidLengthes()}\n")

    connectedTriangles.forEachIndexed { i, tri ->
        if (tri != null && isnode) sb.append("connected node to$i hash:${System.identityHashCode(tri)} \n")
    }
    if (isnode) sb.append("connection parameters to0 side${cParam_.side} type${cParam_.type} lcr${cParam_.lcr} \n")
    if (ispoints) sb.append("points:${point[0]} ${pointAB} ${pointBC} \n")
    if (ispoints) sb.append("pointDim:${dimpoint.a} ${dimpoint.b} ${dimpoint.c} \n")
    if (islength) sb.append("length:${lengthA_} ${lengthB_} ${lengthC_} \n")
    if (isalign) sb.append("isPointnumber_user: ${pointNumber.flag.isMovedByUser}\n")
    if (isalign) sb.append("isPointnumber_auto: ${pointNumber.flag.isAutoAligned}\n")
    if (isalign) sb.append("pointnumber: ${pointnumber}\n\n")
    return sb.toString()
}

fun Triangle.getChainedLengthOrZero(triangle: Triangle?): Float = when (triangle) {
    null -> 0f
    nodeB -> lengthBforce
    nodeC -> lengthCforce
    else -> 0f
}

fun Triangle.getPointByCParam(cparam: ConnParam, prnt: Triangle?): com.example.trilib.PointXY? {
    if (prnt == null) return com.example.trilib.PointXY(0f, 0f)
    return getPointBySide(cparam.side)
}

fun Triangle.getPointByBackSide(i: Int): com.example.trilib.PointXY? = when (getSideByIndex(i)) {
    "B" -> pointAB_()
    "C" -> pointBC_()
    else -> null
}

fun Triangle.getParentPointByType(cParam: ConnParam): com.example.trilib.PointXY =
    getParentPointByType(cParam.side, cParam.type, cParam.lcr)

fun Triangle.getParentPointByType(pbc: Int, pct: Int, lcr: Int): com.example.trilib.PointXY {
    if (nodeA == null) return com.example.trilib.PointXY(0f, 0f)
    return when (pct) {
        1 -> getParentPointByLCR(pbc, lcr)
        2 -> getParentPointByLCR(pbc, lcr).crossOffset(nodeA!!.getPointByBackSide(pbc)!!, -1.0f)
        else -> getParentPointByLCR(pbc, lcr)
    }
}

fun Triangle.getParentPointByLCR(pbc: Int, lcr: Int): com.example.trilib.PointXY {
    if (nodeA == null) return com.example.trilib.PointXY(0f, 0f)
    return when (pbc) {
        1 -> when (lcr) {
            0 -> nodeA!!.pointAB.offset(nodeA!!.pointBC, length[0])
            1 -> getParentOffsetPointBySide(pbc)
            2 -> nodeA!!.pointBC.clone()
            else -> com.example.trilib.PointXY(0f, 0f)
        }
        2 -> when (lcr) {
            0 -> nodeA!!.pointBC.offset(nodeA!!.point[0], length[0])
            1 -> getParentOffsetPointBySide(pbc)
            2 -> nodeA!!.point[0].clone()
            else -> com.example.trilib.PointXY(0f, 0f)
        }
        else -> com.example.trilib.PointXY(0f, 0f)
    }
}

fun Triangle.getNode(pbc: Int): Triangle = when (pbc) {
    0 -> nodeA ?: this
    1 -> nodeB ?: this
    2 -> nodeC ?: this
    -1 -> this
    else -> this
}

fun Triangle.getParentOffsetPointBySide(pbc: Int): com.example.trilib.PointXY {
    if (nodeA == null) return com.example.trilib.PointXY(0f, 0f)
    return when (pbc) {
        1 -> nodeA!!.pointAB.offset(nodeA!!.pointBC, nodeA!!.lengthB * 0.5f + length[0] * 0.5f)
        2 -> nodeA!!.pointBC.offset(nodeA!!.point[0], nodeA!!.lengthC * 0.5f + length[0] * 0.5f)
        else -> nodeA!!.getPointBySide(pbc)!!
    }
}

fun Triangle.myName_(): String = name

fun Triangle.getTapLength(tapP: com.example.trilib.PointXY, rangeRadius: Float): Int {
    setDimPoint()
    val range = rangeRadius * scaleFactor
    val result = when {
        tapP.nearBy(dimpoint.a, range) -> 0
        tapP.nearBy(dimpoint.b, range) -> 1
        tapP.nearBy(dimpoint.c, range) -> 2
        tapP.nearBy(pointnumber, range) -> 3
        tapP.nearBy(dimpoint.s, range) && name.isNotEmpty() -> 4
        else -> -1
    }
    lastTapSide_ = result
    return result
}

fun Triangle.pointCenter_(): com.example.trilib.PointXY = com.example.trilib.PointXY(pointcenter)

fun collision(): Boolean = true

fun Triangle.setBoundaryBox() {
    val lb = pointAB.min(pointBC)
    myBP_.left = lb.x
    myBP_.bottom = lb.y
    val rt = pointAB.max(pointBC)
    myBP_.right = rt.x
    myBP_.top = rt.y
}

fun Triangle.getLengthByIndex(i: Int): Float = when (i) {
    1 -> length[1]
    2 -> length[2]
    else -> 0f
}

fun Triangle.getForceLength(i: Int): Float = when (i) {
    1 -> lengthNotSized[1]
    2 -> lengthNotSized[2]
    else -> 0f
}

fun Triangle.getLine(side: Int): Line = when (side) {
    0 -> Line(point[0], pointAB)
    1 -> Line(pointAB, pointBC)
    2 -> Line(pointBC, point[0])
    else -> Line()
}

fun Triangle.getPointBySide(i: Int): com.example.trilib.PointXY? = pointBySide(i)
fun Triangle.getAngleBySide(i: Int): Float = angleBySide(i)

fun getSideByIndex(i: Int): String = when (i) {
    1, 3, 4, 7, 9 -> "B"
    2, 5, 6, 8, 10 -> "C"
    0 -> "not connected"
    else -> "not connected"
}

val Triangle.parentSide: Int
    get() = when (sideEnum) {
        ConnectionSide.B, ConnectionSide.BR, ConnectionSide.BL, ConnectionSide.BC, ConnectionSide.FB -> 1
        ConnectionSide.C, ConnectionSide.CR, ConnectionSide.CL, ConnectionSide.CC, ConnectionSide.FC -> 2
        else -> 0
    }

fun getPbc(pbc: Int): Int = when (ConnectionSide.fromCode(pbc)) {
    ConnectionSide.B, ConnectionSide.BR, ConnectionSide.BL, ConnectionSide.BC, ConnectionSide.FB -> 1
    ConnectionSide.C, ConnectionSide.CR, ConnectionSide.CL, ConnectionSide.CC, ConnectionSide.FC -> 2
    else -> 0
} 