package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.InputParameter
import com.jpaver.trianglelist.util.Cloneable

open class EditList: Cloneable<EditList> {

    private var list :MutableList<EditObject?> = ArrayList()

    open var basepoint = com.example.trilib.PointXY(0f, 0f)

    open fun retrieveCurrent() :Int { return 0 }
    //open fun setCurrent(i: Int) {}
    open fun addCurrent(num: Int) :Int { return 0 }
    open fun getParams(num: Int) : InputParameter { return InputParameter( )
    }
    open fun remove(num: Int) {}

    open fun reverse(): EditList{ return this }
    open fun size() :Int { return 0 }
    open fun get(num: Int) : EditObject {
        //if( list_.size > number ) return null
        return list[num - 1]!!
    }
    open fun getArea(): Float { return 0f }
    override fun clone(): EditList {
        return this
    }
}
