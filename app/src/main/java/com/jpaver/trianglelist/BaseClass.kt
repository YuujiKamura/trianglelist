package com.jpaver.trianglelist

open class BaseClass {
    var mydata_ = 0

    open fun DoSomething(): Int{
        mydata_ += 1
        return mydata_
    }

    fun DoAnything(): Int{
        return DoSomething()
    }
}

class ExtendedClass : BaseClass(){

    override fun DoSomething(): Int{
        mydata_ += 2
        return mydata_
    }
}