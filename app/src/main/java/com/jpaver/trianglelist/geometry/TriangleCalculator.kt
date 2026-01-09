// 必要なインポートを追加
import kotlin.math.* // 数学関数用
import com.jpaver.trianglelist.PointXY // PointXYクラス用（パスは実際の場所に合わせて調整してください）

/**
 * 三角形の幾何学的計算を行うユーティリティクラス
 * 
 * 注意: すべての計算はスクリーン座標系（Y軸が下向き）を前提とする
 */
object TriangleCalculator {
    /**
     * 三角形の頂点を計算
     */
    fun calculatePoint(
        basepoint: PointXY,
        pointAB: PointXY,
        lengths: FloatArray
    ): PointXY {
        val theta = atan2(
            (basepoint.y - pointAB.y).toDouble(),
            (basepoint.x - pointAB.x).toDouble()
        )
        val alpha = calculateAlpha(lengths)
        val angle = theta + alpha
        
        val offset_x = (lengths[1] * cos(angle)).toFloat()
        val offset_y = (lengths[1] * sin(angle)).toFloat()
        return pointAB.plus(offset_x, offset_y)
    }

    /**
     * 内角の計算
     */
    fun calculateInternalAngle(p1: PointXY, p2: PointXY, p3: PointXY): Double {
        val v1 = p1.subtract(p2)
        val v2 = p3.subtract(p2)
        val angleRadian = acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()))
        return angleRadian * 180 / Math.PI
    }

    /**
     * 重心の計算
     */
    fun calculateCenter(points: Array<PointXY>): PointXY {
        val averageX = points.map { it.x }.average().toFloat()
        val averageY = points.map { it.y }.average().toFloat()
        return PointXY(averageX, averageY)
    }

    private fun calculateAlpha(lengths: FloatArray): Double {
        val powA = lengths[0].pow(2.0f).toDouble()
        val powB = lengths[1].pow(2.0f).toDouble()
        val powC = lengths[2].pow(2.0f).toDouble()
        return acos((powA + powB - powC) / (2 * lengths[0] * lengths[1]))
    }

    fun calculateInternalAngles(p1: PointXY, p2: PointXY, p3: PointXY): Triple<Float, Float, Float> {
        return Triple(
            calculateInternalAngle(p1, p2, p3).toFloat(),
            calculateInternalAngle(p2, p3, p1).toFloat(),
            calculateInternalAngle(p3, p1, p2).toFloat()
        )
    }
} 