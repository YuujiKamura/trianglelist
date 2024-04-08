import com.jpaver.trianglelist.PointXY
import com.jpaver.trianglelist.Triangle

class PointNumber( var triangle: Triangle ): Cloneable {
    data class Flags( var isMovedByUser: Boolean = false, var isAutoAligned: Boolean = false )
    var flag = Flags()
    var point = PointXY(0f,0f)

    public override fun clone(): PointNumber {
        val b = PointNumber(triangle)

        b.flag = flag.copy()
        b.point = point.clone()

        return b
    }

    // region pointNumber

    val WEIGHT = 35f

    fun arrangeNumber(isUse: Boolean = false ): PointXY {
        if(flag.isMovedByUser || flag.isAutoAligned ) return point
        if(!isUse) return weightedMidpoint(WEIGHT)
        return autoAlignPointNumber()
    }

    val BORDER_AREA = 4f
    val BORDER_LENGTH = 2.0f
    private fun autoAlignPointNumber() : PointXY {

        val length = arrayOf(triangle.lengthAforce_, triangle.lengthBforce_, triangle.lengthCforce_)
        // lengthのどれかがBORDERよりも少ない場合にtrueを返す
        val isAnyLengthLessThanBorder = length.any { it < BORDER_LENGTH }

        if (triangle.getArea() <= BORDER_AREA && isAnyLengthLessThanBorder  ){
            point = triangle.pointcenter
            flag.isAutoAligned = true
            return pointUnconnectedSide(triangle.pointcenter)
        }

        return weightedMidpoint(WEIGHT)
    }

    val FLAG_LENGTH_B = triangle.lengthB_*0.8f
    val FLAG_LENGTH_C = triangle.lengthA_*0.8f
    //pointNumberだけ使う
    fun pointUnconnectedSide(
        point: PointXY
    ): PointXY {
        val angle_ = arrayOf( triangle.angleCA, triangle.angleAB, triangle.angleBC )
        val point_ = arrayOf( triangle.point[0], triangle.pointAB, triangle.pointBC )

        if (triangle.nodeTriangleB == null)
            return point.offset( getPointByAngle( angle_[1], angle_[2], point_[1], point_[2] ), FLAG_LENGTH_C ) //.mirroredAndScaledPoint(triangle.pointAB, triangle.pointBC, clockwise)
        if (triangle.nodeTriangleC == null)
            return point.offset( getPointByAngle( angle_[2], angle_[0], point_[2], point_[0] ),  FLAG_LENGTH_B ) //mirroredAndScaledPoint(triangle.point[0], triangle.pointBC, clockwise)

        return weightedMidpoint(WEIGHT)
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