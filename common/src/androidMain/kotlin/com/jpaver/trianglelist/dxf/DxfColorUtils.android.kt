package com.jpaver.trianglelist.dxf

import androidx.compose.ui.graphics.Color

/**
 * Android用のACI色番号からCompose Colorへの変換
 */
actual fun aciToColor(aciIndex: Int): Any {
    return when (aciIndex) {
        0 -> Color.Black // ByBlock
        1 -> Color.Red
        2 -> Color.Yellow
        3 -> Color.Green
        4 -> Color.Cyan
        5 -> Color.Blue
        6 -> Color.Magenta
        7 -> Color.White
        8 -> Color.Gray
        9 -> Color.LightGray
        10 -> Color.Red
        11 -> Color(0xFFFF9999) // LightRed
        12 -> Color(0xFFFFFF99) // LightYellow
        13 -> Color(0xFF99FF99) // LightGreen
        14 -> Color(0xFF99FFFF) // LightCyan
        15 -> Color(0xFF9999FF) // LightBlue
        16 -> Color(0xFFFF99FF) // LightMagenta
        else -> Color.Black // Unknown
    }
} 