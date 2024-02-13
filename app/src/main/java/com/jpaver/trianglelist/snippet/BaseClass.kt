package com.jpaver.trianglelist

open class BaseClass {
    var mydata = 0

    open fun doSomething(): Int{
        mydata += 1
        return mydata
    }

    fun doAnything(): Int{
        return doSomething()
    }
}

class ExtendedClass : BaseClass(){

    override fun doSomething(): Int{
        mydata += 2
        return mydata
    }
}