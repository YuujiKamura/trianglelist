package com.jpaver.trianglelist.editmodel

import com.jpaver.trianglelist.Bounds
import com.jpaver.trianglelist.getAngleBySide
import com.jpaver.trianglelist.getLengthByIndex
import com.jpaver.trianglelist.getPointBySide
import com.jpaver.trianglelist.setLengthStr
import com.jpaver.trianglelist.viewmodel.Cloneable
import com.jpaver.trianglelist.viewmodel.InputParameter
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
            TriLog.e(TAG, "clone() failed", e)
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
    var myBP_ = Bounds(0.0, 0.0, 0.0, 0.0)
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

    // EditObject の多態 (user 指針 2026-06-14「あらゆる限定操作を基底クラスに寄せろ」)。
    // 上位の混在リストは sideCount / vertices / getLine の共通契約だけで動き、kind 分岐を消す。
    override val sideCount: Int = 3
    override fun vertices(): List<com.example.trilib.PointXY> = listOf(point[0], pointAB, pointBC)
    override fun getLine(side: Int): Line = when (side) {
        0 -> Line(point[0], pointAB)
        1 -> Line(pointAB, pointBC)
        2 -> Line(pointBC, point[0])
        else -> Line()
    }

    /**
     * SoT 一本化 段3 寸法多態 (2026-06-15): 既存 WebPrimitiveRenderer.render(trilist) の三角形
     * 寸法ループを移植したもの。図形種別に依らない単一 emit ループを上位 (renderer) に許す。
     * A 辺は親と共有していない時 (node.a == null) または再接続 (connectionSide > 2) のとき出す
     * — 一般化により trapTri (node.a = Rectangle) も「親と共有 → A 辺寸法を抑制」が自然に成立。
     */
    override fun emitDimensionSpecs(scale: Float): List<DimensionSpec> {
        val s = scaleFactor.toDouble()
        val dh = dimHeight.toDouble()
        val placeA = com.jpaver.trianglelist.label.DimensionLayout.layout(pointAB, point[0], dim.vertical.a, dim.horizontal.a, s, dh, 0.0)
        val placeB = com.jpaver.trianglelist.label.DimensionLayout.layout(pointBC, pointAB, dim.vertical.b, dim.horizontal.b, s, dh, 0.0)
        val placeC = com.jpaver.trianglelist.label.DimensionLayout.layout(point[0], pointBC, dim.vertical.c, dim.horizontal.c, s, dh, 0.0)
        this.setLengthStr()
        val specs = mutableListOf<DimensionSpec>()
        val emitA = node.a == null || connectionSide > 2
        if (emitA) {
            specs.add(DimensionSpec(0, strLengthA, placeA, pointAB.calcDimAngle(pointCA), dim.horizontal.a, dim.vertical.a, dim.horizontal.a > 2))
        }
        specs.add(DimensionSpec(1, strLengthB, placeB, pointBC.calcDimAngle(pointAB), dim.horizontal.b, dim.vertical.b, dim.horizontal.b > 2))
        specs.add(DimensionSpec(2, strLengthC, placeC, pointCA.calcDimAngle(pointBC), dim.horizontal.c, dim.vertical.c, dim.horizontal.c > 2))
        return specs
    }

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

    constructor()

    constructor(A: Float, B: Float, C: Float) {
        setNumber(1)
        point[0] = com.example.trilib.PointXY(0f, 0f)
        angle = 180f
        initBasicArguments(A, B, C, point[0], angle)
        if (A <= 0f) return
        calcPoints(point[0], angle)
    }

    constructor(A: Float, B: Float, C: Float, pCA: com.example.trilib.PointXY, baseAngle: Float) {
        setNumber(1)
        initBasicArguments(A, B, C, pCA, baseAngle)
        calcPoints(pCA, baseAngle)
    }

    constructor(myParent: Triangle?, pbc: Int, A: Float, B: Float, C: Float) {
        setOn(myParent, pbc, A, B, C)
    }

    constructor(myParent: Triangle?, cParam: ConnParam, B: Float, C: Float) {
        setOn(myParent, cParam, B, C)
    }

    constructor(parent: Triangle, pbc: Int, B: Float, C: Float) {
        initBasicArguments(
            parent.getLengthByIndex(pbc),
            B,
            C,
            parent.getPointBySide(pbc),
            parent.getAngleBySide(pbc)
        )
        setOn(parent, pbc, B, C)
    }

    // 親 (三角形でも台形でも) の共有辺に底辺(A)を乗せて構築する。混在リストの接続土台:
    // initByParent (EditObject 共通の継ぎ目) が親種別を問わず getLine(side) で辺を返し node も繋ぐ。
    // これで「台形に三角形を接続」が三角形側でも成立する (Rectangle は既に同じ継ぎ目を使う)。
    // side は親の辺番号 (台形 1=B/2=C/3=D、三角形 1=B/2=C)。A長は共有辺の実長になる。
    constructor(parent: EditObject, side: Int, B: Float, C: Float) {
        val base = initByParent(parent, side)
        val a = base.left.lengthTo(base.right).toFloat()
        val baseAngle = base.getAngle().toFloat()
        initBasicArguments(a, B, C, base.left, baseAngle)
        calcPoints(base.left, baseAngle)
        // 台形(Rectangle)を親にすると、getLine の辺向き(winding)が辺ごとに揃っていない (B/C/D マッピングは
        // 寸法ラベル基準で決めたため、calcPoints が頂点を固定側 (+CCW) に置く規約と噛み合わない辺がある)。
        // そのままだと子三角形の頂点が親台形の内部に潜って図形が重なる。頂点が親重心と base 線の同じ側に
        // 来る (=内向き) なら base を反転 (始点↔終点・B↔C 入替) して外側へ出す。三角形親は従来どおり (getLine が外向き)。
        if (parent is Rectangle && apexTowardRectInterior(parent, base)) {
            initBasicArguments(a, C, B, base.right, baseAngle + 180f)
            calcPoints(base.right, baseAngle + 180f)
        }
    }

    // 子三角形の頂点 (pointBC) が親台形の内部を向いているか。base 線に対し「頂点」と「台形重心」が
    // 同じ側 (cross 積が同符号) なら内向き。calcPoints 実行後に呼ぶ (pointBC が確定している前提)。
    private fun apexTowardRectInterior(rect: Rectangle, base: Line): Boolean {
        val lp = rect.calcPoint()
        val cx = (lp.a.left.x + lp.a.right.x + lp.b.left.x + lp.b.right.x) / 4f
        val cy = (lp.a.left.y + lp.a.right.y + lp.b.left.y + lp.b.right.y) / 4f
        val dx = base.right.x - base.left.x
        val dy = base.right.y - base.left.y
        val apexSide = dx * (pointBC.y - base.left.y) - dy * (pointBC.x - base.left.x)
        val centSide = dx * (cy - base.left.y) - dy * (cx - base.left.x)
        return apexSide * centSide > 0f
    }

    constructor(myParent: Triangle?, dP: InputParameter) {
        setOn(myParent, dP.pl, dP.a, dP.b, dP.c)
        name = dP.name
    }

    constructor(dP: InputParameter, baseAngle: Float) {
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
