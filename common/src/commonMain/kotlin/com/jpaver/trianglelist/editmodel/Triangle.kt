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
     * A 辺は親と共有していない時 (nodeA == null && node.a == null) または再接続 (connectionSide > 2)
     * のとき出す。Triangle 子接続は Triangle 独自フィールド nodeA に親を保持し、Rectangle 親 trapTri は
     * EditObject 基底の node.a に親を保持する (継ぎ目が違う)。両方 null = 親共有なし → A 辺寸法 emit。
     */
    override fun emitDimensionSpecs(scale: Float): List<DimensionSpec> {
        val s = scaleFactor.toDouble()
        val dh = dimHeight.toDouble()
        val placeA = com.jpaver.trianglelist.label.DimensionLayout.layout(pointAB, point[0], dim.vertical.a, dim.horizontal.a, s, dh, 0.0)
        val placeB = com.jpaver.trianglelist.label.DimensionLayout.layout(pointBC, pointAB, dim.vertical.b, dim.horizontal.b, s, dh, 0.0)
        val placeC = com.jpaver.trianglelist.label.DimensionLayout.layout(point[0], pointBC, dim.vertical.c, dim.horizontal.c, s, dh, 0.0)
        this.setLengthStr()
        val specs = mutableListOf<DimensionSpec>()
        val emitA = (nodeA == null && node.a == null) || connectionSide > 2
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
        // 重なり防止
        if (apexTowardInterior(parent, base)) {
            initBasicArguments(a, C, B, base.right, baseAngle + 180f)
            calcPoints(base.right, baseAngle + 180f)
        }
    }

    /**
     * EditObject (三角形/台形) を親に、ConnParam に従って三角形を構築する (2026-06-16 混成対応)。
     */
    constructor(parent: EditObject, cParam: ConnParam, B: Float, C: Float) {
        val base = initByParent(parent, cParam.side)
        val baseAngle = base.getAngle().toFloat()

        // ConnParam 考慮の初期化 (Triangle(ptri, cp, ...) と同じロジックを EditObject へ汎用化)
        initBasicArguments(cParam.lenA, B, C, base.left, baseAngle)
        cParam_ = cParam.clone()
        connectionSide = cParam.side
        connectionType_ = cParam.type
        connectionLCR_ = cParam.lcr

        // 二重断面 / フロートのオフセット適用
        // setOn は引数に Triangle? を取る設計だが、EditObject 親の場合は独立したオフセット処理が必要。
        // 親種別に依存しないよう initByParent が返した base (Line) を基準に LCR / Type を解決する。
        val l = getBaseOffsetPoint(base, cParam.lenA, cParam.type, cParam.lcr)
        initBasicArguments(cParam.lenA, B, C, l.left, l.getAngle().toFloat())
        calcPoints(l.left, l.getAngle().toFloat())

        // 重なり防止
        if (apexTowardInterior(parent, base)) {
            // 反転時は重心ベースで逆側へ (A辺は不変、B↔C 入替)
            val revCp = cParam.clone()
            // LCR 反転: Left(0) -> Right(2), Right(2) -> Left(0)
            revCp.lcr = when(cParam.lcr) {
                0 -> 2
                2 -> 0
                else -> 1
            }
            initBasicArguments(cParam.lenA, C, B, base.right, baseAngle + 180f)
            cParam_ = revCp

            // base の向きを反転させた状態でのオフセットを再計算
            val revBase = Line(base.right, base.left)
            val lRev = getBaseOffsetPoint(revBase, revCp.lenA, revCp.type, revCp.lcr)
            initBasicArguments(revCp.lenA, C, B, lRev.left, lRev.getAngle().toFloat())
            calcPoints(lRev.left, lRev.getAngle().toFloat())
        }
    }

    /**
     * 親の種別に依存せず、親の接続辺 (base) と指定された LCR / Type に基づいて
     * 実際に接続する「新しい基線 (Line)」を返す。
     * type=1 (フロート), type=2 (二重断面), lcr=0(左), 1(中), 2(右)
     */
    private fun getBaseOffsetPoint(base: Line, lenA: Float, type: Int, lcr: Int): Line {
        // 通常接続 (type=0) なら親の辺をそのまま使う (スライドしない)
        if (type == 0) {
            return Line(base.left.clone(), base.right.clone())
        }
        
        // LCR に応じた基点 (left) のスライド
        val baseLen = base.left.lengthTo(base.right).toFloat()
        val pLeft = when (lcr) {
            0 -> base.left.offset(base.right, lenA.toDouble())
            1 -> base.left.offset(base.right, (baseLen * 0.5f + lenA * 0.5f).toDouble())
            2 -> base.right.clone()
            else -> base.left.clone()
        }
        
        // type=2 (二重断面) なら、左側に 1.0 はみ出す
        val finalPLeft = if (type == 2) {
            pLeft.crossOffset(base.right, -1.0)
        } else {
            pLeft
        }
        
        // 新しい基線 (角度は base と同じ)
        return Line(finalPLeft, finalPLeft.offset(lenA.toDouble(), base.getAngle()))
    }

    // 子三角形の頂点 (pointBC) が親図形の内部を向いているか。base 線に対し「頂点」と「親重心」が
    // 同じ側 (cross 積が同符号) なら内向き。calcPoints 実行後に呼ぶ (pointBC が確定している前提)。
    private fun apexTowardInterior(parent: EditObject, base: Line): Boolean {
        val cp = parent.centroid()
        val dx = base.right.x - base.left.x
        val dy = base.right.y - base.left.y
        val apexSide = dx * (pointBC.y - base.left.y) - dy * (pointBC.x - base.left.x)
        val centSide = dx * (cp.y - base.left.y) - dy * (cp.x - base.left.x)
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
