package com.jpaver.trianglelist.editmodel

import com.jpaver.trianglelist.getLine
import com.jpaver.trianglelist.viewmodel.InputParameter

// 接続スロット。a=A辺(親と共有), b=B辺, c=C辺, d=D辺(台形=Rectangle の右脚 side=3 用)。
// d は既定 null で追加 — 三角形(3辺)は b/c までしか使わず、台形(4辺)のみ d を使う (trap-design.md 段4)。
data class Node(var a: EditObject?=null, var b: EditObject?=null, var c: EditObject?=null, var d: EditObject?=null )

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
            3 -> {
                // D辺 (台形の右脚)。子の A辺(底辺)を親と共有し、親の d スロットに子を挿す。
                target.node.a = this
                node.d = target
            }
        }
    }

    fun initByParent(parent: EditObject, side: Int): Line {
        var baseline = Line()
        when{
            ( parent is Rectangle) -> {
                // side で B(1)/C(2)/D(3) を出し分ける (旧: 上辺C 固定で side 無視)。
                // getLine の side→辺契約は Triangle.getLine と同じ (trap-design.md 段4 / 確定 B/C/D マッピング)。
                baseline = parent.getLine(side)
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