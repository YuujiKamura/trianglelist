package com.jpaver.trianglelist.dxf

/**
 * DXF固有のACI色番号システムの定義
 * プラットフォーム固有の色型に依存しない
 */
data class DxfColor(
    val aciIndex: Int,
    val name: String,
    val isBackgroundInverted: Boolean = false
) {
    companion object {
        /**
         * DXF ACI色番号からDxfColorオブジェクトを生成
         * @param aciIndex DXF ACI色番号 (0-255)
         * @return DxfColor DXF色オブジェクト
         */
        fun fromAci(aciIndex: Int): DxfColor = when (aciIndex) {
            0 -> DxfColor(0, "ByBlock", false)
            1 -> DxfColor(1, "Red", false)
            2 -> DxfColor(2, "Yellow", false)
            3 -> DxfColor(3, "Green", false)
            4 -> DxfColor(4, "Cyan", false)
            5 -> DxfColor(5, "Blue", false)
            6 -> DxfColor(6, "Magenta", false)
            7 -> DxfColor(7, "White", true) // 背景色の反転
            8 -> DxfColor(8, "Gray", false)
            9 -> DxfColor(9, "LightGray", false)
            10 -> DxfColor(10, "Red", false)
            11 -> DxfColor(11, "LightRed", false)
            12 -> DxfColor(12, "LightYellow", false)
            13 -> DxfColor(13, "LightGreen", false)
            14 -> DxfColor(14, "LightCyan", false)
            15 -> DxfColor(15, "LightBlue", false)
            16 -> DxfColor(16, "LightMagenta", false)
            else -> DxfColor(aciIndex, "Unknown", false) // 未定義色
        }
    }
}

/**
 * ACI色番号をプラットフォーム固有の色型に変換
 * expect/actualパターンで各プラットフォームで実装
 */
expect fun aciToColor(aciIndex: Int): Any