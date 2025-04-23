package com.jpaver.trianglelist

class PointNumberManager( ): Cloneable {
    data class Flags( var isMovedByUser: Boolean = false, var isAutoAligned: Boolean = false )
    var flag = Flags()

    public override fun clone(): PointNumberManager {
        val b = PointNumberManager()

        b.flag = flag.copy()
        return b
    }

    // region pointNumber

    fun setPointByUser(to: com.example.trilib.PointXY, triangle: Triangle, is_user:Boolean = true ): com.example.trilib.PointXY {
        val BORDER = 20f * triangle.scaleFactror
        val length = to.lengthTo(triangle.pointcenter)
        if( length > BORDER ) return triangle.pointnumber// あまり遠い時はスルー
        flag.isMovedByUser = is_user
        flag.isAutoAligned = false
        return to
    }

    val WEIGHT = 35f

    fun resetAutoFlag(isAuto: Boolean = false){
        flag.isAutoAligned = isAuto
    }

    val BORDER_AREA = 3f
    val BORDER_LENGTH = 0.5f
    fun autoAlign( triangle: Triangle, outlineList: OutlineList? = null) : com.example.trilib.PointXY {
        if(flag.isMovedByUser || flag.isAutoAligned ) return triangle.pointnumber

        val length = arrayOf(triangle.lengthAforce, triangle.lengthBforce, triangle.lengthCforce)
        // lengthのどれかがBORDERよりも少ない場合にtrueを返す
        val isAnyLengthLessThanBorder = length.any { it < BORDER_LENGTH }

        if (triangle.getArea() <= BORDER_AREA && isAnyLengthLessThanBorder  ){
            flag.isAutoAligned = true

            return pointUnconnectedSide( triangle, outlineList )
        }

        return weightedMidpoint(triangle, WEIGHT)
    }

    //pointNumberだけ使う
    fun pointUnconnectedSide( triangle: Triangle, outlineList: OutlineList?=null ): com.example.trilib.PointXY {
        //外側に出すと実行時エラーになる
        val KEISUU = 0.7f
        val FLAG_LENGTH_B = triangle.lengthB*KEISUU
        val FLAG_LENGTH_C = triangle.lengthA*KEISUU
        val angle_ = arrayOf( triangle.angleCA, triangle.angleAB, triangle.angleBC )
        val point_ = arrayOf( triangle.pointCA, triangle.pointAB, triangle.pointBC )

        if (triangle.nodeB == null){
            val pointB = getPointByOuterAngle( triangle, angle_[1], angle_[2], point_[1], point_[2], outlineList )
            val resultB = triangle.pointcenter.offset( pointB, FLAG_LENGTH_C )
            return resultB
        }
        if (triangle.nodeC == null){
            val pointC = getPointByOuterAngle( triangle,  angle_[2], angle_[0], point_[2], point_[0], outlineList )
            val resultC = triangle.pointcenter.offset( pointC, FLAG_LENGTH_B )
            return resultC
        }

        return weightedMidpoint(triangle, WEIGHT)
    }

    fun getPointByOuterAngle(triangle: Triangle, angle1:Float, angle2:Float, point1: com.example.trilib.PointXY, point2: com.example.trilib.PointXY, outlineList: OutlineList? ): com.example.trilib.PointXY {
        //val number = triangle.mynumber

        println("getPointByOuterAngle triangle${triangle.mynumber} $angle1 $angle2 $point1 $point2")

        if(outlineList==null) return getPointByAngle(angle1,angle2,point1, point2)

        return outlineList.compare(point1,point2)
    }

    fun getPointByAngle(angle1:Float, angle2:Float, point1: com.example.trilib.PointXY, point2: com.example.trilib.PointXY): com.example.trilib.PointXY {
        if( angle1 > angle2 ) return point1
        return point2
    }

    fun weightedMidpoint(triangle: Triangle, bias: Float): com.example.trilib.PointXY {

        // 角度が大きいほど重みを大きくするための調整
        var weight1 = triangle.angleAB + bias // 角度が大きいほど重みが大きくなる
        var weight2 = triangle.angleBC + bias
        var weight3 = triangle.angleCA + bias

        // 重みの合計で正規化
        val totalWeight = weight1 + weight2 + weight3
        weight1 /= totalWeight
        weight2 /= totalWeight
        weight3 /= totalWeight
        val p1 = triangle.pointAB
        val p2 = triangle.pointBC
        val p3 = triangle.point[0]

        // 重み付き座標の計算
        val weightedX = p1.x * weight1 + p2.x * weight2 + p3.x * weight3
        val weightedY = p1.y * weight1 + p2.y * weight2 + p3.y * weight3
        return com.example.trilib.PointXY(weightedX, weightedY)
    }

    //endregion pointNumber

}