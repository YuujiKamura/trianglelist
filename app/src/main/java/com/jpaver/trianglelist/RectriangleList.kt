package com.jpaver.trianglelist

class RectriangleList: TriangleList() {
    val list = ArrayList<EditObject>()

    fun add(rectangle: EditObject){
        list.add(rectangle)
    }
}
