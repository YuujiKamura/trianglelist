package com.jpaver.trianglelist

import android.util.Log
import com.jpaver.trianglelist.util.Cloneable
import com.jpaver.trianglelist.util.InputParameter
import kotlin.math.roundToInt

// Utilities have been moved to Utils.kt

class Triangle : EditObject, Cloneable<Triangle> {

    companion object {
        private const val TAG = "Triangle"
    }

    var dim = Dims(this)

    override fun clone(): Triangle {
        return try {
            val b = Triangle()

            b.scaleFactor = scaleFactor
            b.pointNumber = pointNumber.clone()
            b.dimpoint = dimpoint.copy()//cloneArray(dimpoints) // 代入だと参照になるので要素ごとにクローン
            b.dimOnPath = dimOnPath.map { it.copy() }.toTypedArray()
            b.pathS = pathS.copy()
            b.dim = dim.clone()

            b.dimHorizontalA = dimHorizontalA
            b.dimHorizontalB = dimHorizontalB
            b.dimHorizontalC = dimHorizontalC

            b.length = length.copyOf(length.size)
            b.lengthNotSized = lengthNotSized.copyOf(lengthNotSized.size)
            b.angle = angle
            b.name = name
            b.mynumber = mynumber
            b.connectionSide = connectionSide
            b.parentnumber = parentnumber

            //b.point = point.clone()
            b.point[0] = point[0].clone()
            b.point[1] = point[1].clone()
            b.point[2] = point[2].clone()
            b.pointAB = pointAB.clone()
            b.pointBC = pointBC.clone()
            b.pointcenter = pointcenter.clone()
            b.pointnumber = pointnumber.clone()
            b.cParam_ = cParam_.clone()
            b.isFloating = isFloating
            b.isColored = isColored
            b.myBP_.left = myBP_.left
            b.myBP_.top = myBP_.top
            b.myBP_.right = myBP_.right
            b.myBP_.bottom = myBP_.bottom
            b.nodeA = nodeA
            b.nodeB = nodeB
            b.nodeC = nodeC
            b.childSide_ = childSide_
            b.mycolor = mycolor
            b.connectionLCR_ = connectionLCR_
            b.connectionType_ = connectionType_
            b.strLengthA = strLengthA
            b.strLengthB = strLengthB
            b.strLengthC = strLengthC

            b
        } catch (e: Exception) {
            Log.e(TAG, "clone() failed", e)
            Triangle()
        }
    }

    //region dimAlign
    // --- 以下のディメンション関連メソッドは TriangleDimExtensions.kt へ移動しました ---
    //endregion dimalign

    // region pointNumber
    // --- 以下の pointNumber 関連メソッドは TrianglePointNumberExtensions.kt へ移動しました ---
    //endregion pointNumber

    //region Parameters
    // ------- 変数・データ定義 -------
    val angleMmCA: Float
        get() = angle - angleCA
    val angleMpAB: Float
        get() = angle + angleAB

    // --- 基本的な保持値 ---
    var valid_ = false
    var length = FloatArray(3)
    var lengthNotSized = FloatArray(3)
    var angle = 180f
    var angleInLocal_ = 0f
    var dedcount = 0f
    var strLengthA = ""
    var strLengthB = ""
    var strLengthC = ""

    // --- 座標関連 ---
    var point = arrayOf(
        com.example.trilib.PointXY(0f, 0f),
        com.example.trilib.PointXY(0f, 0f),
        com.example.trilib.PointXY(0f, 0f)
    )
        internal set
    var pointAB: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f)
        internal set
    var pointBC: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f)
        internal set
    var pointcenter = com.example.trilib.PointXY(0f, 0f)
        internal set

    var dimOnPath: Array<DimOnPath> = Array(3) { DimOnPath() }

    data class Dimpoint(
        var a: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f),
        var b: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f),
        var c: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f),
        var s: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f),
        var name: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f)
    )

    var dimpoint = Dimpoint()

    var nameAlign_ = 0
    var angleCA = 0f
    var angleAB = 0f
    var angleBC = 0f

    // --- 接続関連 ---
    var parentnumber = -1 // 0:root
    var connectionSide = -1 // 0:not use, 1:B, 2:C...
    var connectionType_ = 0
    var connectionLCR_ = 2
    var cParam_ = ConnParam(0, 0, 2, 0f)

    // --- 識別・表示 ---
    var mynumber = 1
    var dimHorizontalA = 0
    var dimHorizontalB = 0
    var dimHorizontalC = 0
    var lastTapSide_ = -1
    var mycolor = 4
    var childSide_ = 0
    var name = ""

    // --- バウンディングボックス・寸法 ---
    var myBP_ = Bounds(0f, 0f, 0f, 0f)
    var pathS = DimOnPath()
    var dimHeight = 0f

    // --- ノード ---
    var nodeA: Triangle? = null
    var nodeB: Triangle? = null
    var nodeC: Triangle? = null

    // --- フラグ ---
    var isFloating = false
    var isColored = false

    // --- 派生値 ---
    val lengthA_: Float
        get() = length[0]
    val lengthB_: Float
        get() = length[1]
    val lengthC_: Float
        get() = length[2]
    val lengthAforce: Float
        get() = lengthNotSized[0]
    val lengthBforce: Float
        get() = lengthNotSized[1]
    val lengthCforce: Float
        get() = lengthNotSized[2]
    val pointCA: com.example.trilib.PointXY
        get() = point[0].clone()

    fun pointAB_(): com.example.trilib.PointXY = com.example.trilib.PointXY(pointAB)
    fun pointBC_(): com.example.trilib.PointXY = com.example.trilib.PointXY(pointBC)

    // --- pointNumber 保持のみ ---
    var pointnumber = com.example.trilib.PointXY(0f, 0f)
    init { pointnumber = com.example.trilib.PointXY(0f, 0f) }
    var pointNumber = PointNumberManager()

    // --- スケール係数（スペル修正: scaleFactor） ---
    var scaleFactor = 1f

    //region constructor
    // 基本引数設定共通処理
    internal fun initBasicArguments(
        A: Float,
        B: Float,
        C: Float,
        pCA: com.example.trilib.PointXY?,
        baseAngle: Float
    ) {
        length[0] = A
        length[1] = B
        length[2] = C
        lengthNotSized[0] = A
        lengthNotSized[1] = B
        lengthNotSized[2] = C
        valid_ = isValidLengthes()
        point[0] = com.example.trilib.PointXY(pCA!!.x, pCA.y)
        pointAB = com.example.trilib.PointXY(0f, 0f)
        pointBC = com.example.trilib.PointXY(0f, 0f)
        pointcenter = com.example.trilib.PointXY(0f, 0f)
        this.angle = baseAngle
        angleCA = 0f
        angleAB = 0f
        angleBC = 0f
    }

    internal constructor()

    internal constructor(A: Float, B: Float, C: Float) {
        setNumber(1)
        point[0] = com.example.trilib.PointXY(0f, 0f)
        angle = 180f
        initBasicArguments(A, B, C, point[0], angle)
        if (A <= 0f) return
        calcPoints(point[0], angle)
    }

    internal constructor(A: Float, B: Float, C: Float, pCA: com.example.trilib.PointXY, baseAngle: Float) {
        setNumber(1)
        initBasicArguments(A, B, C, pCA, baseAngle)
        calcPoints(pCA, baseAngle)
    }

    internal constructor(myParent: Triangle?, pbc: Int, A: Float, B: Float, C: Float) {
        setOn(myParent, pbc, A, B, C)
    }

    internal constructor(myParent: Triangle?, cParam: ConnParam, B: Float, C: Float) {
        setOn(myParent, cParam, B, C)
    }

    internal constructor(parent: Triangle, pbc: Int, B: Float, C: Float) {
        initBasicArguments(
            parent.getLengthByIndex(pbc),
            B,
            C,
            parent.getPointBySide(pbc),
            parent.getAngleBySide(pbc)
        )
        setOn(parent, pbc, B, C)
    }

    internal constructor(myParent: Triangle?, dP: InputParameter) {
        setOn(myParent, dP.pl, dP.a, dP.b, dP.c)
        name = dP.name
    }

    internal constructor(dP: InputParameter, baseAngle: Float) {
        mynumber = dP.number
        name = dP.name
        initBasicArguments(dP.a, dP.b, dP.c, dP.point, baseAngle)
        calcPoints(dP.point, baseAngle)
    }
    //endregion constructor

    //region setter (moved to TriangleSetters.kt)
    /*
    // Moved to TriangleSetters.kt
    // ... existing code ...
    //endregion
    */

    //region node and boundaries
    // --- 以下の node / rotation / boolean 実装は TriangleExtensions.kt へ移動しました ---

    //endregion　isIt

    //region EditObject implementation
    override fun getParams(): InputParameter = InputParameter(
        name,
        "",
        mynumber,
        length[0],
        length[1],
        length[2],
        parentnumber,
        connectionSide,
        point[0],
        pointcenter
    )
    //endregion

    // 従来プロパティ (他ファイル互換用)
    val lengthA: Float
        get() = length[0]
    val lengthB: Float
        get() = length[1]
    val lengthC: Float
        get() = length[2]

    //region compatibility wrappers
    // （旧メソッド呼び出しは同名の拡張関数で代替可能）
    //endregion

    //--- area (needed for unit tests)
    override fun getArea(): Float {
        val (a, b, c) = lengthNotSized
        val s = (a + b + c) / 2f            // 半周長
        val heron = s * (s - a) * (s - b) * (s - c)
        if (heron <= 0f) return 0f          // 不正値を防止

        val area = kotlin.math.sqrt(heron.toDouble()).toFloat()
        // 小数第 2 位で四捨五入
        return (area * 100f).roundToInt() / 100f
    }

}
