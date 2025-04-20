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
        b.flag = flag.copyOf()
        b.flagS = flagS.copy()
        b.scale = scale
        return b
    }
    // endregion properties

    // region constants
    val SIDE_SOKUTEN = 4
    val BORDERDISTANCE = 0.8f
    val SELFDISTANCE = 0f
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
        if (isHorizontal && enableAutoHorizontal ) autoDimHorizontal(0)
        if (isVertical) {
            vertical.a = autoDimVertical(0)
            vertical.b = autoDimVertical(1)
            vertical.c = autoDimVertical(2)
        }
    }

    // region horizontal

    companion object {
        private const val OBtuseThreshold = 90f
        private const val SHARP_THRESHOLD = 2f / 20f  // 0.1f
        private const val OUTERRIGHT = 3
        private const val OUTERLEFT  = 4
    }

    /**
     * 条件を満たさなければ即リターン。
     * 鋭角判定も早期リターンで書く。
     */
    fun autoDimHorizontal(selfSide: Int) {
        val (a, b, c) = triangle.lengthNotSized
        if (a == 0f) return   // 防御

        val ratioB = b / a
        val ratioC = c / a
        if (ratioB > SHARP_THRESHOLD && ratioC > SHARP_THRESHOLD) return

        autoDimHorizontalByAngle()
    }

    /**
     * 接続の手動操作が入っていたらスキップ、
     * 90°超判定 ・ 大きい方判定 も早期リターン中心で。
     */
    private fun autoDimHorizontalByAngle() {
        // ユーザーが両端とも動かしていたら何もしない
        if (flag[SIDEB].isMovedByUser && flag[SIDEC].isMovedByUser) return

        // 各頂点の内角を取得
        val (_, angleB, angleC) = triangle.getVertexAngles()

        // 鈍角（>=90）を優先、なければ大きいほう
        val targetSide = when {
            angleB >= OBtuseThreshold && angleB >= angleC -> SIDEB
            angleC >= OBtuseThreshold && angleC >  angleB -> SIDEC
            angleB >= angleC                             -> SIDEB
            else                                          -> SIDEC
        }

        // ユーザー操作済みならそこにも何もしない
        if (flag[targetSide].isMovedByUser) return

        // 実際の配置
        when (targetSide) {
            SIDEB -> {
                horizontal.b = getunconnectedSide(OUTERRIGHT, OUTERLEFT)
                flag[1].isAutoAligned = true
            }
            SIDEC -> {
                horizontal.c = getunconnectedSide(OUTERRIGHT, OUTERLEFT)
                flag[2].isAutoAligned = true
            }
        }
    }

    private fun getunconnectedSide(outerleft: Int, outerright: Int): Int {
        // nodeC がなければ右（外側）、あれば左
        return if (triangle.nodeC == null) outerright else outerleft
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
