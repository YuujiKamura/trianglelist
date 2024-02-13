package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.Params

open class EditList :Cloneable {

    private var list :MutableList<EditObject?> = ArrayList()

    open var counter: Int = 0
    open var basepoint = PointXY(0f, 0f)

    public override fun clone() :Any { return super.clone() }
    open fun retrieveCurrent() :Int { return 0 }
    //open fun setCurrent(i: Int) {}
    open fun addCurrent(num: Int) :Int { return 0 }
    open fun getParams(num: Int) : Params { return Params( )
    }
    open fun remove(num: Int) {}

    open fun reverse(): EditList{ return this }
    open fun size() :Int { return 0 }
    open fun get(num: Int) : EditObject {
        //if( list_.size > number ) return null
        return list[num - 1]!!
    }
    open fun getArea(): Float { return 0f }
}

open class EditObject :Cloneable {
    open fun getParams() : Params { return Params() }
    open fun getArea(): Float { return 0f }

    var sameDedcount: Int = 0
}
