package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.Cloneable

data class Flags( var isMovedByUser: Boolean = false, var isAutoAligned: Boolean = false )
data class DimAligns(var a:Int=1, var b:Int=1, var c:Int=1)

class Dims( val triangle: Triangle ) : Cloneable<Dims> {

    // ──────────────────────────────
    // 一時的に自動水平配置を無効化するフラグ
    var enableAutoHorizontal = false
    // ──────────────────────────────

    //region propaties and clone
    var vertical = DimAligns(1,1,1 )
    var horizontal = DimAligns(0,0,0 )
    var flag = arrayOf( Flags(), Flags(),Flags() )
    var height = 0f
    var scale = 1f

    override fun clone(): Dims {
        val b = Dims(triangle)
        b.vertical = vertical.copy()
        b.horizontal = horizontal.copy()
        b.height = height
        b.flag = Array(flag.size) { idx -> flag[idx].copy() } //b.flag = flag.copyOf()
        b.scale = scale
        return b
    }
    //endregion propaties

    // region constant parameters
    val SIDE_SOKUTEN = 4
    val HORIZONTAL_OPTIONMAX = 4

    //夾角の、1:外 　3:内
    val OUTER = 1
    val INNER = 3

    val SIDEA = 0
    val SIDEB = 1
    val SIDEC = 2
    //endregion const

    fun arrangeDims(isVertical:Boolean=false, isHorizontal:Boolean=true ) {
        if(isHorizontal) autoDimHorizontal()
        if(isVertical){
            vertical.a = autoDimVertical(0)
            vertical.b = autoDimVertical(1)
            vertical.c = autoDimVertical(2)
        }
    }

    //region horizontal
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
    fun autoDimHorizontal() {
        if (!enableAutoHorizontal) return   // ← 無効化されていたら何もしない

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

    fun getunconnectedSide(outerright: Int, outerleft: Int): Int{
        if (triangle.nodeTriangleC == null) return outerright
        return outerleft
    }

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
            SIDE_SOKUTEN -> triangle.nameHorizontal = cycleIncrement(triangle.nameHorizontal)
        }
    }

    fun cycleIncrement(num: Int, max: Int = HORIZONTAL_OPTIONMAX ): Int = (num + 1) % (max + 1)

    //endregion horizontal

    //region vertical
    fun autoDimVertical(side: Int): Int {
        when(side){
            SIDEA -> {
                if(!flag[0].isMovedByUser ) if(triangle.connectionType < 3) return OUTER
                return vertical.a
            }
            SIDEB -> {
                if(!flag[1].isMovedByUser ) return autoDimVerticalByAreaCompare(triangle.nodeTriangleB)
                return vertical.b
            }
            SIDEC -> {
                if(!flag[2].isMovedByUser ) return autoDimVerticalByAreaCompare(triangle.nodeTriangleC)
                return vertical.c
            }
        }

        return OUTER
    }

    // まず接続ノードがないか、なければ外、あったら内側、ただし、
    // 特殊接続でなければ面積の大きい側に寸法値を配置する
    fun autoDimVerticalByAreaCompare(node: Triangle?): Int{
        if(node==null) return OUTER

        if(node.getArea() > triangle.getArea() && node.connectionType < 3 )  return OUTER

        return INNER
    }

    fun flipVertical(vside: Int): Int {
        if (vside == OUTER) return INNER
        if (vside == INNER) return OUTER
        return vside
    }

    // 自動処理の中で呼ばない。
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
            else -> throw IllegalArgumentException("Unknown side: $side")
        }
    }

    fun setAlignByChild() {
        if (!flag[1].isMovedByUser) {
            vertical.b = if (triangle.nodeTriangleB == null) OUTER else INNER
        }
        if (!flag[2].isMovedByUser) {
            vertical.c = if (triangle.nodeTriangleC == null) OUTER else INNER
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

    //endregion vertical
}
