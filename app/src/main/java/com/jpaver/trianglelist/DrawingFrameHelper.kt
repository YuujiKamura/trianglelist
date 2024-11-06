class DrawingFrameHelper(
    private val printscale: Float,
    private val textscale: Float
) {
    fun calculateFrameSize(baseSize: Float): Float {
        return baseSize * printscale
    }

    fun calculateTextSize(baseTextSize: Float): Float {
        // テキストサイズの計算ロジック
        val calculatedSize = baseTextSize * textscale
        // 最小サイズの制限を設ける
        return maxOf(calculatedSize, MIN_TEXT_SIZE)
    }

    fun isTextFitInFrame(
        text: String,
        textSize: Float,
        frameWidth: Float,
        frameHeight: Float
    ): Boolean {
        // テキストが枠内に収まるかチェック
        val estimatedTextWidth = text.length * textSize
        val estimatedTextHeight = textSize
        return estimatedTextWidth <= frameWidth && estimatedTextHeight <= frameHeight
    }

    companion object {
        const val MIN_TEXT_SIZE = 0.1f
    }
} 