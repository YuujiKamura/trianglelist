package com.jpaver.trianglelist

class TextScaleCalculator {
    companion object {
        const val S_500 = 50f
        const val S_400 = 40f
        const val S_300 = 30f
        const val S_250 = 25f
        const val S_200 = 20f
        const val S_150 = 15f
        const val S_100 = 10f
        const val S_50 = 5f
    }

    fun getTextScale(scale: Float, fileType: String): Float {
        return when (fileType.lowercase()) {
            "dxf", "sfc" -> getDxfTextScale(scale)
            "pdf" -> getPdfTextScale(scale)
            else -> getPdfTextScale(scale)  // 未知のファイルタイプの場合はPDFのマッピングを使用
        }
    }

    private fun getDxfTextScale(scale: Float): Float {
        return when (scale) {
            S_500 -> 0.45f
            S_400 -> 0.40f
            S_300 -> 0.35f
            S_250 -> 0.35f
            S_200 -> 0.30f
            S_100, S_50 -> 0.25f
            else -> 0.25f  // デフォルト値
        }
    }

    private fun getPdfTextScale(scale: Float): Float {
        return when (scale) {
            S_500 -> 3f
            S_400 -> 5f 
            S_300 -> 5f
            S_250 -> 6f
            S_200, S_150, S_100, S_50 -> 8f
            else -> 8f  // デフォルト値
        }
    }
} 