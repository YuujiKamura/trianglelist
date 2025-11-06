package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import com.jpaver.trianglelist.getAngleBySide
import com.jpaver.trianglelist.getLengthByIndex
import com.jpaver.trianglelist.getNode
import com.jpaver.trianglelist.getParentPointByType
import com.jpaver.trianglelist.getPbc
import com.jpaver.trianglelist.getPointByCParam
import com.jpaver.trianglelist.parentSide
import com.jpaver.trianglelist.setDimAlignByChild
import com.jpaver.trianglelist.viewmodel.InputParameter

// Triangle の setter / mutator 系関数を拡張関数として分離
// 元の Triangle.kt からそのまま移植しているためロジックは従来と同一です。

fun Triangle.resetNode(prms: InputParameter, parent: Triangle?, doneObjectList: ArrayList<Triangle>) {
    nodeA?.removeNode(this)
    nodeA = parent
    nodeA?.setNode(this, prms.pl)
    reset(prms)
    doneObjectList.add(this)
}

fun Triangle.setScale(newScale: Float) {
    scaleFactor = newScale
    length[0] *= newScale
    length[1] *= newScale
    length[2] *= newScale
    calcPoints(point[0], angle)
}

fun Triangle.setReverseDefSide(pbc: Int, bToC: Boolean) {
    connectionSide = when {
        !bToC -> when (pbc) {
            3 -> 4; 4 -> 3; 5 -> 6; 6 -> 5; else -> pbc
        }
        else -> when (pbc) {
            3 -> 6; 4 -> 5; 5 -> 4; 6 -> 3; 9 -> 10; 10 -> 9; else -> -pbc + 3
        }
    }
}

fun Triangle.setParentBCFromCLCR() {
    if (cParam_.type == 2) return
    connectionSide = when (cParam_.lcr) {
        0 -> if (cParam_.side == 1) 4 else 6
        1 -> if (cParam_.side == 1) 7 else 8
        else -> if (cParam_.side == 1) 3 else 5
    }
}

// -------------------- setOn オーバーロード群 --------------------
fun Triangle.setOn(parent: Triangle?, pbc: Int, B: Float, C: Float): Triangle {
    connectionSide = pbc
    if (parent == null) {
        resetLength(pbc.toFloat(), B, C)
        return clone()
    }
    setNode(parent, 0)
    parent.setNode(this, parentSide)

    when (pbc) {
        1 -> {
            length[0] = parent.lengthNotSized[1]
            point[0] = parent.pointBC_()
            angle = parent.angleMpAB
        }
        2 -> {
            length[0] = parent.lengthNotSized[2]
            point[0] = parent.pointCA
            angle = parent.angleMmCA
        }
        else -> {
            length[0] = 0f
            lengthNotSized[0] = 0f
            point[0] = PointXY(0f, 0f)
            angle = 180f
        }
    }
    parentnumber = parent.mynumber
    initBasicArguments(length[0], B, C, point[0], angle)
    calcPoints(point[0], angle)
    return this
}

fun Triangle.setOn(parent: Triangle?, pbc: Int, A: Float, B: Float, C: Float): Triangle? {
    connectionSide = pbc
    if (parent == null) return resetLength(pbc.toFloat(), B, C)

    setNode(parent, 0)
    parent.setNode(this, parentSide)

    // A 辺の決定
    length[0] = if (A != parent.getLengthByIndex(pbc)) A else parent.getLengthByIndex(pbc)
    lengthNotSized[0] = length[0]

    setCParamFromParentBC(pbc)
    point[0] = getParentPointByType(cParam_)
    angle = parent.getAngleBySide(pbc)

    parentnumber = parent.mynumber
    initBasicArguments(length[0], B, C, point[0], angle)
    if (!isValidLengthes()) return null
    calcPoints(point[0], angle)
    return clone()
}

fun Triangle.setOn(parent: Triangle?, cParam: ConnParam, B: Float, C: Float): Triangle? {
    if (parent == null) return resetLength(cParam.lenA, B, C)

    setNode(parent, 0)
    parent.setNode(this, cParam.side)
    angle = parent.getAngleBySide(cParam.side)
    setConnectionType(cParam)
    initBasicArguments(length[0], B, C, point[0], angle)
    if (!isValidLengthes()) return null
    calcPoints(point[0], angle)
    setDimAlignByChild()
    return clone()
}

// -------------------- set / reset utilities --------------------
fun Triangle.setNode(node: Triangle?, sideInput: Int) {
    if (node == null) return
    val side = if (sideInput > 2) parentSide else sideInput
    when (side) {
        0 -> nodeA = node.also { if (it === nodeB) nodeB = null; if (it === nodeC) nodeC = null }
        1 -> nodeB = node.also { if (it === nodeA) nodeA = null; if (it === nodeC) nodeC = null }
        2 -> nodeC = node.also { if (it === nodeA) nodeA = null; if (it === nodeB) nodeB = null }
    }
}

fun Triangle.setNumber(num: Int) { mynumber = num }
fun Triangle.setColor(num: Int) { mycolor = num; isColored = true }

fun Triangle.setChild(newChild: Triangle, cbc: Int) {
    childSide_ = cbc
    if (getPbc(cbc) == 1) nodeB = newChild else if (getPbc(cbc) == 2) nodeC = newChild
    setDimAlignByChild()
}

fun Triangle.setBasePoint(cParam: ConnParam): PointXY {
    point[0] = getParentPointByType(cParam.side, cParam.type, cParam.lcr)
    connectionType_ = cParam.type
    connectionLCR_ = cParam.lcr
    calcPoints(point[0], angle)
    return point[0]
}

fun Triangle.setBasePoint(pbc: Int, pct: Int, lcr: Int): PointXY =
    setBasePoint(ConnParam(pbc, pct, lcr, lengthA_))

fun Triangle.reset(prm: InputParameter) {
    length[0] = prm.a
    lengthNotSized[0] = prm.a
    setCParamFromParentBC(prm.pl)
    connectionSide = prm.pl
    parentnumber = prm.pn
    if (nodeA == null || parentnumber < 1) {
        resetLength(prm.a, prm.b, prm.c)
    } else {
        setOn(nodeA, cParam_, prm.b, prm.c)
    }
    name = prm.name
}

fun Triangle.reset(newTri: Triangle, cParam: ConnParam): Triangle {
    if (nodeA == null) resetLength(newTri.length[0], newTri.length[1], newTri.length[2])
    else setOn(nodeA, cParam, newTri.length[1], newTri.length[2])
    name = newTri.name
    return clone()
}

fun Triangle.resetLength(A: Float, B: Float, C: Float): Triangle {
    initBasicArguments(A, B, C, point[0], angle)
    calcPoints(point[0], angle)
    return clone()
}

fun Triangle.resetByParent(prnt: Triangle, cParam: ConnParam): Boolean {
    if (!isValidLengthes(prnt.getLengthByIndex(parentSide), length[1], length[2])) return false
    return setOn(prnt, cParam, length[1], length[2]) != null
}

fun Triangle.resetByParent(parent: Triangle?, pbc: Int): Boolean {
    if (parent == null) return false
    val parentLength = parent.getLengthByIndex(pbc)
    val triValid = if (pbc <= 2) {
        if (!isValidLengthes(parentLength, length[1], length[2])) return true
        setOn(parent, pbc, parentLength, length[1], length[2])
    } else setOn(parent, pbc, length[0], length[1], length[2])
    return triValid == null
}

fun Triangle.resetByChild(myChild: Triangle) {
    setDimAlignByChild()
    if (myChild.cParam_.type != 0) return
    val cbc = myChild.connectionSide
    if (cbc == 1 && !isValidLengthes(length[0], myChild.length[0], length[2])) return
    if (cbc == 2 && !isValidLengthes(length[0], length[1], myChild.length[0])) return
    childSide_ = myChild.connectionSide
    if (nodeA == null || parentnumber < 1) {
        if (cbc == 1) resetLength(length[0], myChild.length[0], length[2])
        if (cbc == 2) resetLength(length[0], length[1], myChild.length[0])
        return
    }
    if (cbc == 1) setOn(nodeA, connectionSide, length[0], myChild.length[0], length[2])
    if (cbc == 2) setOn(nodeA, connectionSide, length[0], length[1], myChild.length[0])
}

fun Triangle.setConnectionType(cParam: ConnParam) {
    if (nodeA == null) {
        android.util.Log.e("TriangleSetters", "setConnectionType called with null nodeA")
        return
    }
    parentnumber = nodeA!!.mynumber
    connectionType_ = cParam.type
    connectionLCR_ = cParam.lcr
    cParam_ = cParam.clone()
    angle = nodeA!!.getAngleBySide(cParam.side)

    if (cParam.type == 0) {
        if (cParam.lenA != 0.0f) {
            length[0] = cParam.lenA
            lengthNotSized[0] = cParam.lenA
            point[0] = getParentPointByType(cParam.side, cParam.type, cParam.lcr)
        } else {
            length[0] = nodeA!!.getLengthByIndex(cParam.side)
            lengthNotSized[0] = nodeA!!.getLengthByIndex(cParam.side)
            point[0] = nodeA!!.getPointByCParam(cParam, nodeA)!!
        }
    } else {
        if (cParam.lenA != 0.0f) {
            length[0] = cParam.lenA
            lengthNotSized[0] = cParam.lenA
        }
        point[0] = getParentPointByType(cParam.side, cParam.type, cParam.lcr)
    }
}

fun Triangle.setCParamFromParentBC(pbc: Int) {
    var curLCR = cParam_.lcr
    if (cParam_.side == 0 && (pbc == 4 || pbc == 6)) curLCR = 0
    if (cParam_.side == 0 && (pbc == 7 || pbc == 8)) curLCR = 1
    cParam_ = when (pbc) {
        -1, 0 -> ConnParam(0, 0, 2, lengthNotSized[0])
        1 -> ConnParam(1, 0, 2, lengthNotSized[0])
        2 -> ConnParam(2, 0, 2, lengthNotSized[0])
        3 -> ConnParam(1, 1, 2, lengthNotSized[0])
        4 -> ConnParam(1, 1, 0, lengthNotSized[0])
        5 -> ConnParam(2, 1, 2, lengthNotSized[0])
        6 -> ConnParam(2, 1, 0, lengthNotSized[0])
        7 -> ConnParam(1, 1, 1, lengthNotSized[0])
        8 -> ConnParam(2, 1, 1, lengthNotSized[0])
        9 -> ConnParam(1, 2, curLCR, lengthNotSized[0])
        10 -> ConnParam(2, 2, curLCR, lengthNotSized[0])
        else -> cParam_
    }
}

fun Triangle.reset(newTri: Triangle) {
    val backup = cParam_.clone()
    if (nodeA == null || parentnumber < 1) {
        angle = newTri.angle
        angleInLocal_ = newTri.angleInLocal_
        resetLength(newTri.length[0], newTri.length[1], newTri.length[2])
    } else setOn(nodeA, newTri.connectionSide, newTri.length[0], newTri.length[1], newTri.length[2])
    cParam_ = backup.clone()
    name = newTri.name
}

fun Triangle.resetElegant(prm: InputParameter) {
    reset(prm)
    nodeA?.resetByNode(parentSide)
}

fun Triangle.resetByNode(pbc: Int) {
    val node = getNode(pbc)
    var lengthConnected = getLengthByIndex(pbc)
    if (node.connectionSide < 3) lengthConnected = node.length[0]
    when (pbc) {
        0 -> {}
        1 -> initBasicArguments(length[0], lengthConnected, length[2], node.pointBC, -node.angle)
        2 -> initBasicArguments(length[0], length[1], lengthConnected, node.point[0], node.angle + angleBC)
    }
    calcPoints(point[0], angle)
} 