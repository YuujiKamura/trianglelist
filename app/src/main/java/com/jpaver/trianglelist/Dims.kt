package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.Cloneable

data class Flags(var isMovedByUser: Boolean = false, var isAutoAligned: Boolean = false)
data class DimAligns(var a: Int = 1, var b: Int = 1, var c: Int = 1, var s: Int = 0)

class Dims(val triangle: Triangle) : Cloneable<Dims> {
    // ──────────────────────────────
    // 一時的に自動水平配置を無効化するフラグ（必要なら使えます）
    var enableAutoHorizontal = false
    // ──────────────────────────────

    // region properties and clone
    var vertical = DimAligns(1, 1, 1, 1)
    var horizontal = DimAligns(0, 0, 0, 0)
    var flag = arrayOf(Flags(), Flags(), Flags())
    var flagS = Flags()
    var height = 0f
    var scale = 1f

    override fun clone(): Dims {
        val b = Dims(triangle)
        b.vertical = vertical.copy()
        b.horizontal = horizontal.copy()
        b.height = height
        // Flags を要素ごとにコピー
        b.flag = Array(flag.size) { idx -> flag[idx].copy() }
        b.flagS = flagS.copy()
        b.scale = scale
        return b
    }
    // endregion properties

    // region constants
    val SIDE_SOKUTEN = 4
    val HORIZONTAL_OPTIONMAX = 4

    // 内外判定
    val OUTER = 1
    val INNER = 3

    val SIDEA = 0
    val SIDEB = 1
    val SIDEC = 2
    // endregion constants

    /**
     * 寸法配置を一気に実行
     */
    fun arrangeDims(isVertical: Boolean = false, isHorizontal: Boolean = true) {
        if (isHorizontal && enableAutoHorizontal ) autoDimHorizontalByAngle()
        if (isVertical) {
            vertical.a = autoDimVertical(0)
            vertical.b = autoDimVertical(1)
            vertical.c = autoDimVertical(2)
        }
    }

    // region horizontal

    val SHARP_THRESHOLD = 20f
    // どっちをみて右？三角形の内側？
    val OUTERRIGHT = 3
    val OUTERLEFT  = 4

    /**
     * 接続の手動操作が入っていたらスキップ、
     */
    private fun autoDimHorizontalByAngle() {
        // ユーザーが両端とも動かしていたら何もしない
        if (triangle.getArea()>5f) return
        if (flag[SIDEB].isMovedByUser && flag[SIDEC].isMovedByUser) return

        // 各頂点の内角を取得
        val (angleCA, angleAB, angleBC) = triangle.getVertexAngles()

        // 鋭角はどれだ
        val targetSide = when {
            angleBC <= SHARP_THRESHOLD -> SIDEB
            angleCA <= SHARP_THRESHOLD -> SIDEC
            else                       -> SIDEA
        }

        // ユーザー操作済みならそこにも何もしない
        if (flag[targetSide].isMovedByUser) return

        // 実際の配置
        when (targetSide) {
            SIDEB -> {
                horizontal.b = getNotSharpenSide( angleAB, angleBC )
                flag[1].isAutoAligned = true
            }
            SIDEC -> {
                horizontal.c = getNotSharpenSide( angleBC, angleCA )
                flag[2].isAutoAligned = true
            }
        }
    }

    /**
     * 鋭角頂点を挟む辺のうち、より広い角度（鈍角側）方向を返す
     * 三角形の向き（頂点の順序）で右・左は変わる
     */
    private fun getNotSharpenSide(leftAngle:Float, rightAngle:Float): Int {
        return if ( leftAngle <= rightAngle ) OUTERLEFT else OUTERRIGHT
    }

    /**
     * 手動操作でサイクル
     */
    fun controlHorizontal(side: Int) {
        when (side) {
            SIDEA -> horizontal.a = cycleIncrement(horizontal.a)
            SIDEB -> {
                horizontal.b = cycleIncrement(horizontal.b)
                flag[1].isMovedByUser = true
            }
            SIDEC -> {
                horizontal.c = cycleIncrement(horizontal.c)
                flag[2].isMovedByUser = true
            }
            SIDE_SOKUTEN -> {
                horizontal.s = cycleIncrement(horizontal.s, 1)
                flagS.isMovedByUser = true
            }
        }
    }

    private fun cycleIncrement(num: Int, max: Int = HORIZONTAL_OPTIONMAX): Int =
        (num + 1) % (max + 1)

    // endregion horizontal

    // region vertical

    fun autoDimVertical(side: Int): Int {
        return when (side) {
            SIDEA -> if (!flag[0].isMovedByUser && triangle.connectionSide < 3) OUTER else INNER
            SIDEB -> if (!flag[1].isMovedByUser) autoDimVerticalByAreaCompare(triangle.nodeB) else vertical.b
            SIDEC -> if (!flag[2].isMovedByUser) autoDimVerticalByAreaCompare(triangle.nodeC) else vertical.c
            else  -> OUTER
        }
    }

    /**
     * 接続ノードがない→外側。
     * ある場合、ノード面積 > 自分の面積 & 特殊接続でなければ外側。
     */
    private fun autoDimVerticalByAreaCompare(node: Triangle?): Int {
        if (node == null) return OUTER
        return if (node.getArea() > triangle.getArea() && node.connectionSide < 3) OUTER else INNER
    }

    fun flipVertical(vside: Int): Int = if (vside == OUTER) INNER else OUTER

    fun controlVertical(side: Int) {
        when (side) {
            SIDEA -> {
                vertical.a = flipVertical(vertical.a)
                flag[0].isMovedByUser = true
            }
            SIDEB -> {
                vertical.b = flipVertical(vertical.b)
                flag[1].isMovedByUser = true
            }
            SIDEC -> {
                vertical.c = flipVertical(vertical.c)
                flag[2].isMovedByUser = true
            }
            SIDE_SOKUTEN -> {
                triangle.nameAlign_ = flipVertical(triangle.nameAlign_)
            }
        }
    }

    fun setAlignByChild() {
        if (!flag[1].isMovedByUser) {
            vertical.b = if (triangle.nodeB == null) OUTER else INNER
        }
        if (!flag[2].isMovedByUser) {
            vertical.c = if (triangle.nodeC == null) OUTER else INNER
        }
    }

    fun setAligns(sa: Int, sb: Int, sc: Int, ha: Int, hb: Int, hc: Int) {
        horizontal.a = sa
        horizontal.b = sb
        horizontal.c = sc
        vertical.a = ha
        vertical.b = hb
        vertical.c = hc
    }
    // endregion vertical
}
