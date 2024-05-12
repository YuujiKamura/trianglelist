package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.InputParameter
import com.jpaver.trianglelist.util.Cloneable

open class EditList: Cloneable<EditList> {

    private var list :MutableList<EditObject?> = ArrayList()

    open var basepoint = PointXY(0f, 0f)

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

data class Node(var a:EditObject?=null, var b:EditObject?=null, var c:EditObject?=null )

open class EditObject() {
    var node = Node()
    open fun setNode2( target:EditObject, side:Int=0, side2:Int=1 ){
        when(side){
            0 -> {
                target.setNode2(this, side2 )
                node.a = target
            }
            1 -> {
                target.node.a = this
                node.b = target
            }
            2 -> {
                target.node.a = this
                node.c = target
            }
        }
    }

    open fun getParams() : InputParameter { return InputParameter() }
    open fun getArea(): Float { return 0f }

    var sameDedcount: Int = 0
}
