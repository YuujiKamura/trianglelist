package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.Params

open class EditList :Cloneable {

    private var list :MutableList<EditObject?> = ArrayList()

    open var counter: Int = 0

    public override fun clone() :Any { return super.clone() }
    open fun getCurrent() :Int { return 0 }
    open fun setCurrent(i: Int) {}
    open fun addCurrent(i: Int) :Int { return 0 }
    open fun getParams(i: Int) : Params { return Params( )
    }
    open fun remove(i: Int) {}

    open fun size() :Int { return 0 }
    open fun get(number: Int) : EditObject {
        //if( list_.size > number ) return null
        return list[number - 1]!!
    }
    open fun getArea(): Float { return 0f }
}

open class EditObject :Cloneable {
    open fun getParams() : Params { return Params() }
    open fun getArea(): Float { return 0f }

    var sameDedcount: Int = 0
}
