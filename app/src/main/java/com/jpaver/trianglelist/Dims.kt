package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.Cloneable

data class Flags( var isMovedByUser: Boolean = false, var isAutoAligned: Boolean = false )
data class DimAligns(var a:Int=1, var b:Int=1, var c:Int=1)

class Dims( val triangle: Triangle ) : Cloneable<Dims> {

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
        b.flag = flag.copyOf()
        b.scale = scale
        return b
    }
    //endregion propaties

    // region constant parameters
    val SIDE_SOKUTEN = 4
    val BORDERDISTANCE = 0.8f
    val SELFDISTANCE = 0f
    val HORIZONTAL_OPTIONMAX = 4

    //夾角の、1:外 　3:内
    val OUTER = 1
    val INNER = 3

    val SIDEA = 0
    val SIDEB = 1
    val SIDEC = 2
    //endregion const

    fun arrangeDims(isVertical:Boolean=false, isHorizontal:Boolean=true ) {
        if(isHorizontal) autoDimHorizontal(0)
        if(isVertical){
            vertical.a = autoDimVertical(0)
            vertical.b = autoDimVertical(1)
            vertical.c = autoDimVertical(2)
        }
    }

    //region horizontal

    fun autoDimHorizontal(selfside_index: Int ) {
        makeflags_dim_distances(selfside_index).forEachIndexed { targetside_index, result ->
            if ( result == true ) outerDimHorizontal(targetside_index)
        }
    }

    fun makeflags_dim_distances(selfside: Int ): List<Boolean> {
        val dimpoint = triangle.dimpoint.toArray()
        return dimpoint[selfside].distancesTo(dimpoint).map { it < BORDERDISTANCE && it > SELFDISTANCE }
    }

    //つかうの難しい？
    fun outerDimHorizontal(targetindex: Int){
        val OUTERRIGHT = 3
        val OUTERLEFT = 4
        when (targetindex) {
            SIDEC -> {
                if(!flag[2].isMovedByUser )
                    horizontal.c = getunconnectedSide(OUTERRIGHT,OUTERLEFT)
                flag[2].isAutoAligned = true
                return
            }
            SIDEB -> {
                if(!flag[1].isMovedByUser )
                    horizontal.b = getunconnectedSide(OUTERRIGHT,OUTERLEFT)
                flag[1].isAutoAligned = true
                return
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
                return INNER
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
