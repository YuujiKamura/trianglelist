package com.jpaver.trianglelist.editmodel

import com.jpaver.trianglelist.viewmodel.InputParameter
import com.jpaver.trianglelist.viewmodel.Cloneable

// 編集図形の混在リスト (混在の土台)。
// 三角形 (Triangle) も台形 (Rectangle) も EditObject なので、同じ一本の list に
// メンバーとして並べて持つ。図形種別ごとにリスト型を継承で増やさない (コンポジション)。
// TriangleList / DeductionList は自前の専用リストを持ち size()/get() を override するため、
// 基底を実体化してもそれらの挙動は変わらない (golden 不変)。
open class EditList: Cloneable<EditList> {

    private var list :MutableList<EditObject?> = ArrayList()

    open var basepoint = com.example.trilib.PointXY(0f, 0f)

    /** 図形を末尾に追加。種別を問わず同じ一本のリストに入る。 */
    open fun add(obj: EditObject?) { list.add(obj) }

    open fun retrieveCurrent() :Int { return 0 }
    //open fun setCurrent(i: Int) {}
    open fun addCurrent(num: Int) :Int { return 0 }
    open fun getParams(num: Int) : InputParameter { return InputParameter( )
    }
    open fun remove(num: Int) { if (num in 1..list.size) list.removeAt(num - 1) }

    open fun reverse(): EditList { return this }
    open fun size() :Int { return list.size }
    open fun get(num: Int) : EditObject {
        //if( list_.size > number ) return null
        return list[num - 1]!!
    }
    open fun getArea(): Float { return 0f }
    override fun clone(): EditList {
        return this
    }
}
