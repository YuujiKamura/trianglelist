package com.jpaver.trianglelist

class Collision(
    var point: PointXY = PointXY(
        0f,
        0f
    ),
    var num: Int = 0,
    var side: Int = 0,
    var isHit: Boolean = false,
    var wx: Float = 0f,
    var wy: Float = 0f
){

    fun detect(list: ArrayList<Collision>) : Collision {
        if(list.size == 0) return Collision()
        for( i in 0 until list.size){
            if(true == list.get(i).inside(point)) return list.get(i)
        }

        return Collision()
    }

    fun inside(p: PointXY) :Boolean{
        if(point.x - wx < p.x && p.x < point.x + wx && point.y - wy < p.y && p.y < point.y + wy)
            return true
        return false
    }

}