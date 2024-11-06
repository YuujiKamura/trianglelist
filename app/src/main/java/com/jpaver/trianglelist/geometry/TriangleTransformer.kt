/**
 * 三角形の変換操作（回転、移動、スケール）を行うユーティリティクラス
 */
import com.jpaver.trianglelist.PointXY

object TriangleTransformer {
    fun rotate(point: PointXY, center: PointXY, angle: Float): PointXY {
        // 回転の実装
        return PointXY(0f, 0f) // 仮の戻り値。実際の回転計算を実装してください
    }

    fun scale(point: PointXY, center: PointXY, scale: Float): PointXY {
        // スケールの実装
        return PointXY(
            center.x + (point.x - center.x) * scale,
            center.y + (point.y - center.y) * scale
        )
    }

    fun transformTriangle(p1: PointXY, p2: PointXY, p3: PointXY): Array<PointXY> {
        // ... 既存のコード ...
        return arrayOf(p1, p2, p3) // 戻り値を追加
    }

    fun transformTriangleReverse(p1: PointXY, p2: PointXY, p3: PointXY): Array<PointXY> {
        // ... 既存のコード ...
        return arrayOf(p1, p2, p3) // 戻り値を追加
    }
} 