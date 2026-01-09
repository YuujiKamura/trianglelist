package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import com.jpaver.trianglelist.Bounds
import com.jpaver.trianglelist.arrangeDims
import com.jpaver.trianglelist.setBoundaryBox
import kotlin.math.*

/**
 * Extension functions extracted from the original Triangle implementation.
 * これらは Triangle 本体から分離し、ファイル分割するための一時措置。
 */

fun Triangle.calcPoints(basepoint: PointXY = this.point[0], _angle: Float = this.angle, isArrangeDims: Boolean = false) {
    pointAB = basepoint.offset(length[0], _angle)
    pointBC = calculatePointBC(basepoint)
    calculateInternalAngles()
    calculatePointCenter()
    arrangeDims(isArrangeDims)
    if (!pointNumber.flag.isMovedByUser) pointnumber = pointcenter
    setBoundaryBox()
}

internal fun Triangle.calculatePointBC(basepoint: PointXY): PointXY {
    val theta = atan2((basepoint.y - pointAB.y).toDouble(), (basepoint.x - pointAB.x).toDouble())
    val powA = length[0].pow(2.0f).toDouble()
    val powB = length[1].pow(2.0f).toDouble()
    val powC = length[2].pow(2.0f).toDouble()
    val alpha = acos((powA + powB - powC) / (2 * length[0] * length[1]))
    val angle = theta + alpha
    val offsetX = (length[1] * cos(angle)).toFloat()
    val offsetY = (length[1] * sin(angle)).toFloat()
    return pointAB.plus(offsetX, offsetY)
}

internal fun Triangle.calculateInternalAngles() {
    angleAB = calculateInternalAngle(point[0], pointAB, pointBC).toFloat()
    angleBC = calculateInternalAngle(pointAB, pointBC, point[0]).toFloat()
    angleCA = calculateInternalAngle(pointBC, point[0], pointAB).toFloat()
}

fun Triangle.getVertexAngles(): Triple<Float, Float, Float> {
    calculateInternalAngles()
    return Triple(angleCA, angleAB, angleBC)
}

internal fun calculateInternalAngle(p1: PointXY, p2: PointXY, p3: PointXY): Double {
    val v1 = p1.subtract(p2)
    val v2 = p3.subtract(p2)
    val angleRadian = acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()))
    return angleRadian * 180 / Math.PI
}

internal fun Triangle.calculatePointCenter(): PointXY {
    val averageX = (pointAB.x + pointBC.x + point[0].x) / 3
    val averageY = (pointAB.y + pointBC.y + point[0].y) / 3
    pointcenter = PointXY(averageX, averageY)
    return pointcenter
}

fun Triangle.scale(basepoint: PointXY, scaleFactor: Float, isArrangeDims: Boolean = false) {
    this.scaleFactor *= scaleFactor
    point[0].change_scale(basepoint, scaleFactor)
    length[0] *= scaleFactor
    length[1] *= scaleFactor
    length[2] *= scaleFactor
    pointcenter.change_scale(basepoint, scaleFactor)
    pointnumber.change_scale(basepoint, scaleFactor)
    calcPoints(point[0], angle, isArrangeDims)
}

fun Triangle.move(vector: PointXY) {
    pointAB = pointAB.plus(vector)
    pointBC = pointBC.plus(vector)
    point[0] = point[0].plus(vector)
    pointcenter = pointcenter.plus(vector)
    pointnumber = pointnumber.plus(vector)
    dimpoint.a.add(vector)
    dimpoint.b.add(vector)
    dimpoint.c.add(vector)
    dimpoint.s.add(vector)
    myBP_.left += vector.x
    myBP_.right += vector.x
    myBP_.top += vector.y
    myBP_.bottom += vector.x
    dimOnPath[0].move(vector)
    dimOnPath[1].move(vector)
    dimOnPath[2].move(vector)
    pathS.move(vector)
}

fun Triangle.rotate(basepoint: PointXY, addDegree: Float) {
    angleInLocal_ += addDegree
    rotate_body(basepoint, addDegree)
    pointnumber = pointnumber.rotate(basepoint, addDegree)
}

fun Triangle.calcPoints(ref: Triangle?, refside: Int) {
    setNode(ref, refside)
    val plist: Array<PointXY?>
    val llist: FloatArray
    val powlist: FloatArray
    val angleLocal: Float
    when (refside) {
        0 -> {
            angleLocal = this.angle
            plist = arrayOf(point[0], pointAB, pointBC)
            llist = floatArrayOf(length[0], length[1], length[2])
            powlist = floatArrayOf(length[0].pow(2.0f), length[1].pow(2.0f), length[2].pow(2.0f))
        }
        1 -> {
            angleLocal = nodeB!!.angle + 180f
            plist = arrayOf(nodeB!!.pointAB, pointBC, point[0])
            llist = floatArrayOf(length[1], length[2], length[0])
            powlist = floatArrayOf(length[1].pow(2.0f), length[2].pow(2.0f), length[0].pow(2.0f))
            pointAB = plist[0]!!
        }
        2 -> {
            angleLocal = nodeC!!.angle + 180f
            plist = arrayOf(nodeC!!.pointAB, point[0], pointAB)
            llist = floatArrayOf(length[2], length[0], length[1])
            powlist = floatArrayOf(length[2].pow(2.0f), length[0].pow(2.0f), length[1].pow(2.0f))
            pointBC = plist[0]!!
        }
        else -> throw IllegalStateException("Unexpected value: $refside")
    }
    // set plist[1]
    plist[1]!![ (plist[0]!!.x + llist[0] * cos(Math.toRadians(angleLocal.toDouble()))).toFloat() ] =
        (plist[0]!!.y + llist[0] * sin(Math.toRadians(angleLocal.toDouble()))).toFloat()

    val theta = atan2((plist[0]!!.y - plist[1]!!.y).toDouble(), (plist[0]!!.x - plist[1]!!.x).toDouble())
    val alpha = acos((powlist[0] + powlist[1] - powlist[2]) / (2 * llist[0] * llist[1]))

    plist[2]!![ (plist[1]!!.x + llist[1] * cos(theta + alpha)).toFloat() ] =
        (plist[1]!!.y + llist[1] * sin(theta + alpha)).toFloat()

    calculateInternalAngles()
    if (refside == 1) this.angle = nodeB!!.angle - angleCA
    if (refside == 2) this.angle = nodeC!!.angle + angleCA
}

// --------------------------------------------------
// 以下は Triangle 本体から移設したユーティリティ実装
// --------------------------------------------------

fun Triangle.removeNode(target: Triangle) {
    if (nodeA === target) nodeA = null
    if (nodeB === target) nodeB = null
    if (nodeC === target) nodeC = null
}

fun Triangle.expandBoundaries(listBound: Bounds): Bounds {
    setBoundaryBox()
    val newB = Bounds(myBP_.left, myBP_.top, myBP_.right, myBP_.bottom)
    if (myBP_.bottom > listBound.bottom) newB.bottom = listBound.bottom
    if (myBP_.top   < listBound.top)    newB.top    = listBound.top
    if (myBP_.left  > listBound.left)   newB.left   = listBound.left
    if (myBP_.right < listBound.right)  newB.right  = listBound.right
    return newB
}

// ---------- 回転系 ----------
fun Triangle.control_rotate(basepoint: PointXY, addDegree: Float) {
    angleInLocal_ += addDegree
    rotate_body(basepoint, addDegree)
    pointnumber = pointnumber.rotate(basepoint, addDegree)
}

fun Triangle.recover_rotate(basepoint: PointXY, addDegree: Float) {
    angleInLocal_ = addDegree
    rotate_body(basepoint, addDegree)
    if (!pointNumber.flag.isMovedByUser) pointnumber = pointnumber.rotate(basepoint, addDegree)
}

fun Triangle.rotate_body(basepoint: PointXY, addDegree: Float) {
    point[0] = point[0].rotate(basepoint, addDegree)
    angle += addDegree
    calcPoints(point[0], angle)
}

fun Triangle.rotateLengthBy(side: Int) {
    var pf: Float
    var temp: Int
    var pp: PointXY
    if (side == 1) { // B to A
        pf = length[0]
        length[0] = length[1]
        length[1] = length[2]
        length[2] = pf
        pf = lengthNotSized[0]
        lengthNotSized[0] = lengthNotSized[1]
        lengthNotSized[1] = lengthNotSized[2]
        lengthNotSized[2] = pf
        pp = point[0].clone()
        point[0] = pointAB
        pointAB = pointBC
        pointBC = pp.clone()
        pf = angleCA
        angleCA = angleAB
        angleAB = angleBC
        angleBC = pf
        angle = angleMmCA - angleAB
        if (angle < 0) angle += 360f
        if (angle > 360) angle -= 360f
        pp = dimpoint.a.clone()
        dimpoint.a = dimpoint.b
        dimpoint.b = dimpoint.c
        dimpoint.c = pp.clone()
        temp = dim.vertical.a
        dim.vertical.a = dim.vertical.b
        dim.vertical.b = dim.vertical.c
        dim.vertical.a = temp
        temp = dimHorizontalA
        dimHorizontalA = dimHorizontalB
        dimHorizontalB = dimHorizontalC
        dimHorizontalC = temp
    }
    if (side == 2) { // C to A
        pf = length[0]
        length[0] = length[2]
        length[2] = length[1]
        length[1] = pf
        pf = lengthNotSized[0]
        lengthNotSized[0] = lengthNotSized[2]
        lengthNotSized[2] = lengthNotSized[1]
        lengthNotSized[1] = pf
        pp = point[0].clone()
        point[0] = pointBC
        pointBC = pointAB
        pointAB = pp.clone()
        pf = angleCA
        angleCA = angleBC
        angleBC = angleAB
        angleAB = pf
        angle += angleCA + angleBC
        if (angle < 0) angle += 360f
        if (angle > 360) angle -= 360f
        pp = dimpoint.a.clone()
        dimpoint.b = dimpoint.c
        dimpoint.c = dimpoint.b
        dimpoint.b = pp.clone()
        temp = dim.vertical.a
        dim.vertical.a = dim.vertical.c
        dim.vertical.c = dim.vertical.b
        dim.vertical.b = temp
        temp = dimHorizontalA
        dimHorizontalA = dimHorizontalC
        dimHorizontalC = dimHorizontalB
        dimHorizontalB = temp
    }
}

fun Triangle.rotateLCRandGet(): Triangle {
    rotateLCR()
    return this
}

fun Triangle.rotateLCR(): PointXY {
    connectionLCR_--
    if (connectionLCR_ < 0) connectionLCR_ = 2
    cParam_.lcr = connectionLCR_
    setParentBCFromCLCR()
    return setBasePoint(cParam_)
}

// ---------- 判定系 ----------
fun Triangle.alreadyHaveChild(pbc: Int): Boolean {
    if (pbc < 1) return false
    return when (pbc) {
        1 -> nodeB != null
        2 -> nodeC != null
        else -> false
    }
}

fun Triangle.hasChildIn(cbc: Int): Boolean =
    (nodeB != null && cbc == 1) || (nodeC != null && cbc == 2)

fun Triangle.hasConstantParent(): Boolean = (mynumber - parentnumber) <= 1

fun Triangle.hasChild(): Boolean = nodeB != null || nodeC != null

fun Triangle.isValidLengthes(A: Float = lengthA_, B: Float = lengthB_, C: Float = lengthC_): Boolean =
    !(A + B <= C) && !(B + C <= A) && !(C + A <= B)

val Triangle.getIsFloating: Boolean
    get() = nodeA != null && connectionSide > 8

val Triangle.getIsColored: Boolean
    get() = if (nodeA == null) isColored else mycolor != nodeA!!.mycolor

fun Triangle.isCollide(p: PointXY): Boolean = p.isCollide(pointAB, pointBC, point[0])

val Triangle.isValid: Boolean
    get() = !(length[0] <= 0.0f || length[1] <= 0.0f || length[2] <= 0.0f) && isValidLengthes()

// Utility helpers ---------------------------------------------------- 