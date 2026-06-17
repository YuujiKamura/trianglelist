package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import com.jpaver.trianglelist.viewmodel.InputParameter

// 接続スロット。a=A辺(親と共有), b=B辺, c=C辺, d=D辺(台形=Rectangle の右脚 side=3 用)。
// d は既定 null で追加 — 三角形(3辺)は b/c までしか使わず、台形(4辺)のみ d を使う (trap-design.md 段4)。
data class Node(var a: EditObject?=null, var b: EditObject?=null, var c: EditObject?=null, var d: EditObject?=null )

/**
 * 全図形の基底 (user 方針 2026-06-14「あらゆる限定操作を基底クラスに寄せろ」「台形だから・三角形
 * だからっていう場合分けを吸収しろ」)。形状ごとの差は sideCount / vertices / getLine の多態だけに
 * 集約し、上位 (混在リストの bbox 計算、辺タップ動線、寸法レイアウト等) は kind 分岐なしで動く。
 */
open class EditObject() {
    var node = Node()
    
    // 共通の階層・表示メタデータ (Triangle / Rectangle 共通)
    open var mynumber = 1
    open var parentnumber = -1 // 0:root, -1:independent
    open var connectionSide = -1 // 0:not use, 1:B, 2:C, 3:D...
    open var name = ""
    open var mycolor = 4
    open var isColored = false
    open var isFloating = false

    /** 辺数 (三角形=3, 台形=4)。上位コードはこの値で配列 stride を決め、kind 分岐を消す。 */
    open val sideCount: Int = 0

    /** 図形の頂点列 (側面の順、bbox / fit / 重心算出の共通入口)。形状依存の場合分けは下位で完結。 */
    open fun vertices(): List<PointXY> = emptyList()

    /** side で辺を 1 本取り出す共通契約 (三角形=0:A,1:B,2:C / 台形=0:A,1:B,2:C,3:D)。 */
    open fun getLine(side: Int): Line = Line()

    /**
     * 寸法 spec を多態的に返す (SoT 一本化 段3、2026-06-15)。
     * 図形種別に依らず描画側が単一ループで寸法を emit できるよう、各図形が自分用の
     * DimensionSpec リストを返す責務を持つ。空 (= 寸法概念がない図形) は default の emptyList。
     * scale は CSV のビュー scale (renderCsv の effScale と同値)、辺長 / scale で実寸を出す系で使う。
     */
    open fun emitDimensionSpecs(scale: Float): List<DimensionSpec> = emptyList()

    open fun setNode2(target: EditObject, side:Int=0, side2:Int=1 ){
        when(side){
            0 -> {
                target.setNode2(this, side2 )
                node.a = target
            }
            1 -> {
                target.node.a = this
                node.b = target
            }
            2 -> {
                target.node.a = this
                node.c = target
            }
            3 -> {
                // D辺 (台形の右脚)。子の A辺(底辺)を親と共有し、親の d スロットに子を挿す。
                target.node.a = this
                node.d = target
            }
        }
    }

    /**
     * [時計回り展開の原則 / Clockwise Expansion Convention]
     *
     * 子図形は親辺を時計回りに継続して外側に展開する:
     *   - 子の A辺起点 = 親辺の終端 (right)
     *   - 子の A辺終点 = 親辺の始端 (left)
     *   - 子の頂点は +90° (右回り) で外側へ伸びる (Rectangle.crossClockwise=90.0)
     *
     * 返値の left が接続開始点、getAngle() がそのまま子の進行方向になる。
     * 実装上は「親辺の forward と逆走」に見えるが、それは結果であって原則ではない。
     * 原則は「時計回りで外に展開する」こと。
     */
    fun initByParent(parent: EditObject, side: Int): Line {
        val forward = parent.getLine(side)
        parent.setNode2(this, side)
        return Line(forward.right, forward.left)
    }

    // ---------- 階層走査ユーティリティ ----------
    fun alreadyHaveChild(side: Int): Boolean {
        if (side < 1) return false
        return when (side) {
            1 -> node.b != null
            2 -> node.c != null
            3 -> node.d != null
            else -> false
        }
    }

    fun hasChild(): Boolean = node.b != null || node.c != null || node.d != null

    fun hasConstantParent(): Boolean = (mynumber - parentnumber) <= 1

    /**
     * 図形の重心 (幾何中心) を vertices() から算出。
     * 子図形を接続する際、 頂点が図形の内側に向いているか (=重なるか) の判定に使う。
     */
    fun centroid(): PointXY {
        val vs = vertices()
        if (vs.isEmpty()) return PointXY(0f, 0f)
        var sx = 0.0; var sy = 0.0
        for (v in vs) {
            sx += v.x; sy += v.y
        }
        return PointXY((sx / vs.size).toFloat(), (sy / vs.size).toFloat())
    }

    open fun getParams() : InputParameter { return InputParameter() }
    open fun getArea(): Float { return 0f }

    var sameDedcount: Int = 0
}