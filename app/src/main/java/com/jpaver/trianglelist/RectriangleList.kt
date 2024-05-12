package com.jpaver.trianglelist

class RectriangleList: TriangleList() {
    val list = ArrayList<EditObject>()
    //val trialist = ArrayList<Triangle>()
    fun add(rectangle: EditObject){
        list.add(rectangle)
    }

    fun add(triangle: Triangle){
        list.add(triangle)
        //super.add(triangle, true)
    }
}