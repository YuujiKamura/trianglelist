enum class DimAlign {
    INSIDE,      // 内側に寸法値を配置
    OUTSIDE,     // 外側に寸法値を配置
    AUTOMATIC;   // 自動（デフォルトでは接続時はINSIDE、非接続時はOUTSIDE）

    companion object {
        fun fromLegacy(value: Int): DimAlign {
            return when (value) {
                3 -> INSIDE
                1 -> OUTSIDE
                else -> AUTOMATIC
            }
        }
    }
} 