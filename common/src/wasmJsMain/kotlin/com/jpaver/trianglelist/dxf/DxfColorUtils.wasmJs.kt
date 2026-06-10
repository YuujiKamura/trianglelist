package com.jpaver.trianglelist.dxf

/**
 * wasmJs 用の ACI 色番号変換。Web の自然な色型 = CSS カラー文字列を返す
 * (desktop/android actual の Compose Color と同じ色対応)
 */
actual fun aciToColor(aciIndex: Int): Any {
    return when (aciIndex) {
        0 -> "#000000" // ByBlock
        1 -> "#ff0000" // Red
        2 -> "#ffff00" // Yellow
        3 -> "#00ff00" // Green
        4 -> "#00ffff" // Cyan
        5 -> "#0000ff" // Blue
        6 -> "#ff00ff" // Magenta
        7 -> "#ffffff" // White
        8 -> "#888888" // Gray
        9 -> "#cccccc" // LightGray
        10 -> "#ff0000" // Red
        11 -> "#ff9999" // LightRed
        12 -> "#ffff99" // LightYellow
        13 -> "#99ff99" // LightGreen
        14 -> "#99ffff" // LightCyan
        15 -> "#9999ff" // LightBlue
        16 -> "#ff99ff" // LightMagenta
        else -> "#000000" // Unknown
    }
}
