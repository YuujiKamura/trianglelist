package com.jpaver.trianglelist.editmodel

import com.jpaver.trianglelist.viewmodel.InputParameter
import com.jpaver.trianglelist.viewmodel.Cloneable

// 編集図形リストの型純粋な基底。
// 三角形 (Triangle) も台形 (Rectangle) も EditObject 派生。
// 派生クラス (TriangleList / DeductionList) は T を具象型に縛って Triangle/Deduction 専用に、
// EditList<EditObject> として直接使えば混在も受容できる (RectangleTest 参照)。
// 混在の SoT は CsvDoc.figureRows、本クラスは編集ループ向けの list 抽象。
open class EditList<T : EditObject>: Cloneable<EditList<T>> {

    private var list :MutableList<T?> = ArrayList()

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
    open fun get(num: Int) : EditObject {
        //if( list_.size > number ) return null
        return list[num - 1]!!
    }
    open fun getArea(): Float { return 0f }
    override fun clone(): EditList<T> {
        return this
    }
}
