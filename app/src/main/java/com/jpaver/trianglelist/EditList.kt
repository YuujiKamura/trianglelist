package com.jpaver.trianglelist

open class EditList {

    open var lastTapNum_ = 0

    open val mode: Int = 0
    open var counter: Int = 0

    open fun getCurrent() :Int { return 0 }
    open fun setCurrent(i: Int) {}
    open fun addCurrent(i: Int) :Int { return 0 }
    open fun getParams(i: Int) :Params { return Params( )}
    open fun remove(i: Int) {}

    open fun size() :Int { return 0 }
    open fun get(number: Int) :EditObject { return EditObject() }
    open fun getArea(): Float { return 0f }
}

open class EditObject :Cloneable {
    open fun getParams() :Params { return Params() }
    open fun getArea(): Float { return 0f }

    var sameDedcount: Int = 0
}
