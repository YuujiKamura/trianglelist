package com.jpaver.trianglelist.editmodel

import com.jpaver.trianglelist.getLine
import com.jpaver.trianglelist.viewmodel.InputParameter

data class Node(var a: EditObject?=null, var b: EditObject?=null, var c: EditObject?=null )

open class EditObject() {
    var node = Node()
    open fun setNode2(target: EditObject, side:Int=0, side2:Int=1 ){
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

    fun initByParent(parent: EditObject, side: Int): Line {
        var baseline = Line()
        when{
            ( parent is Rectangle) -> {
                baseline = parent.calcPoint().b
            }
            ( parent is Triangle) -> {
                baseline = parent.getLine(side)
            }
        }
        parent.setNode2(this,side)
        return baseline
    }

    open fun getParams() : InputParameter { return InputParameter() }
    open fun getArea(): Float { return 0f }

    var sameDedcount: Int = 0
}