package com.jpaver.trianglelist.editmodel

import com.jpaver.trianglelist.viewmodel.InputParameter
import com.jpaver.trianglelist.viewmodel.Cloneable

// 編集図形リストの基底 SoT。
// 三角形 (Triangle) も台形 (Rectangle) も CycleShape 派生。派生クラス (TriangleList / DeductionList) は
// 自前の独立フィールドを廃止し、基底 list を唯一の SoT として使う (2026-06-15 SoT 一本化)。
open class EditList<T : CycleShape>: Cloneable<EditList<T>> {

    protected val list: MutableList<T?> = ArrayList()

    open var basepoint = com.example.trilib.PointXY(0f, 0f)

    /** 図形を末尾に追加。T に縛られているので型安全。 */
    open fun add(obj: T?) { list.add(obj) }

    open fun retrieveCurrent() :Int { return 0 }
    //open fun setCurrent(i: Int) {}
    open fun addCurrent(num: Int) :Int { return 0 }
    open fun getParams(num: Int) : InputParameter { return InputParameter( )
    }
    open fun remove(num: Int) { if (num in 1..list.size) list.removeAt(num - 1) }

    open fun reverse(): EditList<T> { return this }
    open fun size() :Int { return list.size }
    open fun get(num: Int) : CycleShape {
        //if( list_.size > number ) return null
        return list[num - 1]!!
    }
    open fun getArea(): Float { return 0f }
    override fun clone(): EditList<T> {
        return this
    }

    // SoT 一本化 (2026-06-15): 派生クラスの自前リストを段階的に置き換えるため、
    // 必要な管理 API を基底に装備する。null 要素は飛ばす (派生側の挙動に合わせる)。

    /** 末尾追加で、追加後のインデックス (1始まり) を返す。Diff 管理向け。 */
    open fun appendAndIndex(obj: T?): Int { list.add(obj); return list.size }

    /** 任意位置 (1始まり) へ挿入。 */
    open fun insertAt(num: Int, obj: T?) {
        val i = (num - 1).coerceIn(0, list.size)
        list.add(i, obj)
    }

    /** 全消去。 */
    open fun clearAll() { list.clear() }

    open fun isEmpty(): Boolean = list.isEmpty()
    open fun isNotEmpty(): Boolean = list.isNotEmpty()

    /** 要素が存在する番号レンジ (1始まり、空なら IntRange.EMPTY)。 */
    open fun numberRange(): IntRange = if (list.isEmpty()) IntRange.EMPTY else 1..list.size

    /** 全要素を 1 → size の順に巡回 (null は飛ばす)。 */
    open fun forEachItem(action: (CycleShape) -> Unit) {
        for (e in list) if (e != null) action(e)
    }

    /** 番号 (1始まり) 付きで全要素を巡回 (null は飛ばす)。 */
    open fun forEachItemIndexed(action: (Int, CycleShape) -> Unit) {
        for ((i, e) in list.withIndex()) if (e != null) action(i + 1, e)
    }

    /** 指定 CycleShape の番号 (1始まり)、見つからなければ -1。 */
    open fun indexOfItem(obj: CycleShape?): Int {
        if (obj == null) return -1
        for ((i, e) in list.withIndex()) if (e === obj) return i + 1
        return -1
    }

    open fun firstItem(): CycleShape? = list.firstOrNull { it != null }
    open fun lastItem(): CycleShape? = list.lastOrNull { it != null }
}
