package com.jpaver.trianglelist.cadview

import androidx.compose.ui.graphics.Color
import com.jpaver.trianglelist.dxf.DxfColor
import com.jpaver.trianglelist.dxf.DxfConstants

/**
 * DxfColorをCompose.Colorに変換するデスクトップ固有の実装
 */
object ColorConverter {
    
    fun toComposeColor(dxfColor: DxfColor): Color {
        return when (dxfColor.aciIndex) {
            DxfConstants.Colors.BY_BLOCK -> Color.Black // ByBlock - 通常は親エンティティの色、ここでは黒
            DxfConstants.Colors.RED -> Color.Red
            DxfConstants.Colors.YELLOW -> Color.Yellow
            DxfConstants.Colors.GREEN -> Color.Green
            DxfConstants.Colors.CYAN -> Color.Cyan
            DxfConstants.Colors.BLUE -> Color.Blue
            DxfConstants.Colors.MAGENTA -> Color.Magenta
            DxfConstants.Colors.WHITE_BLACK -> if (dxfColor.isBackgroundInverted) Color.Black else Color.White // 背景色の反転
            DxfConstants.Colors.GRAY -> Color.Gray
            DxfConstants.Colors.LIGHT_GRAY -> Color.LightGray
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