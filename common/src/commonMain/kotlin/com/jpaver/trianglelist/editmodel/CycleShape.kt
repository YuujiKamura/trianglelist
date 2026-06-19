package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import com.jpaver.trianglelist.viewmodel.InputParameter

// 接続スロット。a=A辺(親と共有), b=B辺, c=C辺, d=D辺(台形=Rectangle の右脚 side=3 用)。
// d は既定 null で追加 — 三角形(3辺)は b/c までしか使わず、台形(4辺)のみ d を使う (trap-design.md 段4)。
data class Node(var a: CycleShape?=null, var b: CycleShape?=null, var c: CycleShape?=null, var d: CycleShape?=null )

/**
 * 全図形の基底 (user 方針 2026-06-14「あらゆる限定操作を基底クラスに寄せろ」「台形だから・三角形
 * だからっていう場合分けを吸収しろ」)。形状ごとの差は sideCount / vertices / getLine の多態だけに
 * 集約し、上位 (混在リストの bbox 計算、辺タップ動線、寸法レイアウト等) は kind 分岐なしで動く。
 */
open class CycleShape() {
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
     * 図形のラインの集合 (環閉合 = 一周巡回順)。 default は side 0..sideCount-1 順 (= Triangle は
     * これで連続巡回成立)、 巡回順と side index が一致しない図形 (Rectangle: side 1=B 左脚 と side 3=
     * D 右脚 で巡回順が飛ぶ) は override して巡回順に並べる。 環閉合の検証 / signedArea / 外向き
     * 算出はすべてここを起点にする。
     */
    open fun edges(): List<Line> = (0 until sideCount).map { getLine(it) }

    /**
     * 環閉合検証: 隣接ラインの end == 次ラインの start が全側面で成立するか。 不成立なら
     * 周回向きも内側外側も定義不能 ── user 指針 (2026-06-18):「環閉合したデータ群を揃えて初めて
     * 周回向きと内側外側が決まる」、「成立しない時は早期 return、 単に拒絶すれば済む」。
     */
    fun isClosed(): Boolean {
        val es = edges()
        if (es.size < 3) return false
        val EPS = 1e-3f
        for (i in es.indices) {
            val curr = es[i]
            val next = es[(i + 1) % es.size]
            val dx = curr.right.x - next.left.x
            val dy = curr.right.y - next.left.y
            if (dx > EPS || dx < -EPS || dy > EPS || dy < -EPS) return false
        }
        return true
    }

    /**
     * ラインの集合 (= edges) のシューレース signed area。 環閉合済前提、 違反時は IllegalStateException。
     *   > 0: CCW、 < 0: CW。 全 CycleShape が CW 規約遵守 (signedArea < 0) であるべき。
     */
    fun signedArea(): Double {
        check(isClosed()) { "signedArea requires closed-loop edges (環閉合違反)" }
        val es = edges()
        var s = 0.0
        for (e in es) {
            s += e.left.x.toDouble() * e.right.y.toDouble() - e.right.x.toDouble() * e.left.y.toDouble()
        }
        return s * 0.5
    }

    /**
     * 親辺 side の外向き単位 perpendicular ベクトル。 環閉合済前提、 違反時は IllegalStateException。
     * 環閉合した edges から signedArea で巡回方向 (CW/CCW) を動的算出、 親辺 forward の外側 perp を
     * 返す ── user 指針 (2026-06-18):「ラインの集合から外向きを教えてくれる関数を書くことは可能」。
     */
    fun outwardPerpUnit(side: Int): PointXY {
        check(isClosed()) { "outwardPerpUnit requires closed-loop edges (環閉合違反)" }
        val edge = getLine(side)
        val dx = (edge.right.x - edge.left.x).toDouble()
        val dy = (edge.right.y - edge.left.y).toDouble()
        val len = edge.left.lengthTo(edge.right)
        if (len <= 0.0) return PointXY(0f, 0f)
        val ccw = signedArea() > 0.0
        // CCW (signedArea > 0): forward の **左** が内側、 外側は perpCW = (dy, -dx) / len
        // CW (signedArea < 0): forward の **右** が内側、 外側は perpCCW = (-dy, dx) / len
        return if (ccw) PointXY((dy / len).toFloat(), (-dx / len).toFloat())
        else            PointXY((-dy / len).toFloat(), (dx / len).toFloat())
    }

    /**
     * 寸法 spec を多態的に返す (SoT 一本化 段3、2026-06-15)。
     * 図形種別に依らず描画側が単一ループで寸法を emit できるよう、各図形が自分用の
     * DimensionSpec リストを返す責務を持つ。空 (= 寸法概念がない図形) は default の emptyList。
     * scale は CSV のビュー scale (renderCsv の effScale と同値)、辺長 / scale で実寸を出す系で使う。
     */
    open fun emitDimensionSpecs(scale: Float): List<DimensionSpec> = emptyList()

    open fun setNode2(target: CycleShape, side:Int=0, side2:Int=1 ){
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
    fun initByParent(parent: CycleShape, side: Int): Line {
        parent.setNode2(this, side)

        val refLine = parent.getLine(side)
        return Line(refLine.right, refLine.left)
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
    open fun centroid(): PointXY {
        val vs = vertices()
        if (vs.isEmpty()) return PointXY(0f, 0f)
        var sx = 0.0; var sy = 0.0
        for (v in vs) {
            sx += v.x; sy += v.y
        }
        return PointXY((sx / vs.size).toFloat(), (sy / vs.size).toFloat())
    }

    /**
     * 番号 (mynumber) を画面に表示する基準点。
     * default は centroid (= 幾何中心)。Triangle のように user が手動で動かした位置を
     * 保持する場合は override してその位置を返す。上位 (WebPrimitiveRenderer 等) はこの
     * メソッドを呼ぶだけで kind 分岐なしに番号位置を取れる。
     */
    open fun pointNumberAnchor(): PointXY = centroid()

    /**
     * 描画前の段階0: 寸法の text size を図形に配布する。
     * default は no-op (寸法を持たない図形)。Triangle / Rectangle 等の寸法を持つ図形は
     * override して内部の dim キャッシュや height 値を更新する。上位は kind 分岐なしに
     * 全要素に一括配布できる。
     */
    open fun applyDimTextSize(size: Float) {}

    /**
     * 測点 (Station Flag) 描画用のパラメータ 3 種。
     * default は「測点固有の値を持たない図形」向けに、上位から渡された fallback を返す。
     * Triangle のみ override して dim.horizontal.s / scaleFactor / dimHeight を返す。
     */
    open fun sokutenHorizontal(): Int = 0
    open fun sokutenScale(fallback: Double): Double = fallback
    open fun sokutenHeight(fallback: Double): Double = fallback

    /**
     * 図形がこの点を内側に含むか (タップ判定の共通契約)。
     * default は凸多角形 (環閉合済 edges) の edge-sign 一致判定 ── 全 edge について
     * (b-a) × (p-a) の符号が一致 (= 全て同じ side) ⇔ 内側。 sign==0 は edge / 頂点上を
     * 内側扱い (boundary inclusive)。 fan 分解の対角線上で false になる数値事故を避ける
     * (centroid hit が ぎりぎり対角線に乗ると fan の境界で漏れる、 2026-06-18 WebHitTest
     * 'tri-tri-rect' Rectangle 子 centroid (0.298, -2.216) で発生)。
     * 別実装を持つ図形 (例: Triangle の符号判定) は override で差し替えてよい。
     */
    open fun containsPoint(p: PointXY): Boolean {
        val v = vertices()
        val n = v.size
        if (n < 3) return false
        var hasPos = false
        var hasNeg = false
        for (i in 0 until n) {
            val a = v[i]; val b = v[(i + 1) % n]
            val cross = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
            if (cross > 0f) hasPos = true
            if (cross < 0f) hasNeg = true
            if (hasPos && hasNeg) return false
        }
        return true
    }

    open fun getParams() : InputParameter { return InputParameter() }
    open fun getArea(): Float { return 0f }

    var sameDedcount: Int = 0
}