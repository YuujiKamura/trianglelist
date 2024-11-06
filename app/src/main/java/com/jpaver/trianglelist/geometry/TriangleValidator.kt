/**
 * 三角形の妥当性検証を行うユーティリティクラス
 */
object TriangleValidator {
    fun isValid(lengths: FloatArray): Boolean {
        if (lengths.any { it <= 0.0f }) return false
        return isValidLengths(lengths[0], lengths[1], lengths[2])
    }

    fun isValidLengths(a: Float, b: Float, c: Float): Boolean {
        return !(a + b <= c) && !(b + c <= a) && !(c + a <= b)
    }
} 