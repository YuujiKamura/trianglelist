package com.jpaver.trianglelist.cadview

import androidx.compose.ui.graphics.Color
import com.jpaver.trianglelist.common.DxfColor

/**
 * DxfColorをCompose.Colorに変換するデスクトップ固有の実装
 */
object ColorConverter {
    
    fun toComposeColor(dxfColor: DxfColor): Color {
        return when (dxfColor.aciIndex) {
            0 -> Color.Black // ByBlock - 通常は親エンティティの色、ここでは黒
            1 -> Color.Red
            2 -> Color.Yellow
            3 -> Color.Green
            4 -> Color.Cyan
            5 -> Color.Blue
            6 -> Color.Magenta
            7 -> if (dxfColor.isBackgroundInverted) Color.Black else Color.White // 背景色の反転
            8 -> Color.Gray
            9 -> Color.LightGray
            10 -> Color.Red
            11 -> Color(0xFFFF7F7F) // Light Red
            12 -> Color(0xFFFFFF7F) // Light Yellow
            13 -> Color(0xFF7FFF7F) // Light Green
            14 -> Color(0xFF7FFFFF) // Light Cyan
            15 -> Color(0xFF7F7FFF) // Light Blue
            16 -> Color(0xFFFF7FFF) // Light Magenta
            else -> Color.Black // 未定義色は黒
        }
    }
    
    fun aciToColor(aciIndex: Int): Color {
        return toComposeColor(DxfColor.fromAci(aciIndex))
    }
} 