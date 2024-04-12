package com.jpaver.trianglelist

class PointNumber( var triangle: Triangle ): Cloneable {
    data class Flags( var isMovedByUser: Boolean = false, var isAutoAligned: Boolean = false )
    var flag = Flags()
    var pointnumber = PointXY(0f,0f) //ここでtriangle.pointnumberを代入するとエラーになるの何でかな？
    var pointcenter = PointXY(0f,0f)

    public override fun clone(): PointNumber {
        val b = PointNumber(triangle)

        b.flag = flag.copy()
        b.pointnumber = pointnumber.clone()
        b.pointcenter = pointcenter.clone()

        return b
    }

    // region pointNumber

    fun isFlagOn():Boolean{
        return flag.isMovedByUser || flag.isAutoAligned
    }

    val WEIGHT = 35f

    fun resetAutoFlag(onoff:Boolean=false){
        flag.isAutoAligned = onoff
    }

    fun arrangeNumber(isUse: Boolean = false , outlineList: OutlineList? = null ): PointXY {
        if(flag.isMovedByUser || flag.isAutoAligned ) return pointnumber
        if(!isUse) return weightedMidpoint(WEIGHT)
        return autoAlignPointNumber(outlineList)
    }

    val BORDER_AREA = 4f
    val BORDER_LENGTH = 1.5f
    private fun autoAlignPointNumber(outlineList: OutlineList? = null) : PointXY {

        val length = arrayOf(triangle.lengthAforce_, triangle.lengthBforce_, triangle.lengthCforce_)
        // lengthのどれかがBORDERよりも少ない場合にtrueを返す
        val isAnyLengthLessThanBorder = length.any { it < BORDER_LENGTH }

        if (triangle.getArea() <= BORDER_AREA && isAnyLengthLessThanBorder  ){
            flag.isAutoAligned = true
            pointcenter = triangle.pointcenter
            pointnumber = pointUnconnectedSide( pointcenter, outlineList )
            return pointnumber
        }

        return weightedMidpoint(WEIGHT)
    }

    //pointNumberだけ使う
    fun pointUnconnectedSide( point: PointXY, outlineList: OutlineList?=null ): PointXY {
        //外側に出すと実行時エラーになる
        val KEISUU = 0.9f
        val FLAG_LENGTH_B = triangle.lengthB*KEISUU
        val FLAG_LENGTH_C = triangle.lengthA*KEISUU
        val angle_ = arrayOf( triangle.angleCA, triangle.angleAB, triangle.angleBC )
        val point_ = arrayOf( triangle.pointCA, triangle.pointAB, triangle.pointBC )

        if (triangle.nodeTriangleB == null)
            return point.offset( getPointByOuterAngle( angle_[1], angle_[2], point_[1], point_[2], outlineList ), FLAG_LENGTH_C )
        if (triangle.nodeTriangleC == null)
            return point.offset( getPointByOuterAngle( angle_[2], angle_[0], point_[2], point_[0], outlineList ), FLAG_LENGTH_B )

        return weightedMidpoint(WEIGHT)
    }

    fun getPointByOuterAngle( angle1:Float, angle2:Float, point1:PointXY, point2:PointXY, outlineList: OutlineList? ):PointXY{

        println("getPointByOuterAngle triangle${triangle.mynumber} $angle1 $angle2 $point1 $point2")

        if(outlineList==null) return getPointByAngle(angle1,angle2,point1, point2)

        return outlineList.compare(point1,point2)
    }

    fun getPointByAngle(angle1:Float, angle2:Float, point1:PointXY, point2:PointXY):PointXY{
        if( angle1 > angle2 ) return point1
        return point2
    }

    fun weightedMidpoint(bias: Float): PointXY {

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
        return PointXY(weightedX, weightedY)
    }

    //endregion pointNumber

}