package com.jpaver.trianglelist

import com.jpaver.trianglelist.editmodel.DimOnPath
import com.jpaver.trianglelist.editmodel.Triangle

private const val SIDE_SOKUTEN = 4

fun Triangle.setLengthStr() {
    strLengthA = lengthNotSized[0].formattedString(2)
    strLengthB = lengthNotSized[1].formattedString(2)
    strLengthC = lengthNotSized[2].formattedString(2)
}

/**
 * 寸法位置を再計算し、関連するポイントを更新する。
 */
internal fun Triangle.arrangeDims(isVertical: Boolean = false, isHorizontal: Boolean = true) {
    dim.arrangeDims(isVertical, isHorizontal)
    setDimPath(dim.height)
    setDimPoint()
}

fun Triangle.setDimPath(ts: Float = dimHeight) {
    dimHeight = ts
    dimOnPath[0] = DimOnPath(scaleFactor, pointAB, point[0], dim.vertical.a, dim.horizontal.a, ts)
    dimOnPath[1] = DimOnPath(scaleFactor, pointBC, pointAB, dim.vertical.b, dim.horizontal.b, ts)
    dimOnPath[2] = DimOnPath(scaleFactor, point[0], pointBC, dim.vertical.c, dim.horizontal.c, ts)
    pathS = DimOnPath(scaleFactor, pointAB, point[0], SIDE_SOKUTEN, dim.horizontal.s, ts)
}

fun Triangle.controlDimHorizontal(side: Int) {
    dim.controlHorizontal(side)
    setDimPath()
    setDimPoint()
}

fun Triangle.controlDimVertical(side: Int) {
    dim.controlVertical(side)
    setDimPath()
    setDimPoint()
}

fun Triangle.setDimAlignByChild() = dim.setAlignByChild()

fun Triangle.setDimAligns(sa: Int, sb: Int, sc: Int, ha: Int, hb: Int, hc: Int) =
    dim.setAligns(sa, sb, sc, ha, hb, hc)

fun Triangle.setDimPoint() {
    dimpoint.a = dimOnPath[0].dimpoint
    dimpoint.b = dimOnPath[1].dimpoint
    dimpoint.c = dimOnPath[2].dimpoint
    dimpoint.s = pathS.dimpoint
} 