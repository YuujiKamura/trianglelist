package com.jpaver.trianglelist.editmodel

class RectriangleList: TriangleList() {
    val list = ArrayList<EditObject>()

    fun add(rectangle: EditObject){
        list.add(rectangle)
    }
}
