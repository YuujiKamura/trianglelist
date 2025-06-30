package com.jpaver.trianglelist.adapter

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.jpaver.trianglelist.dxf.DxfText
import com.jpaver.trianglelist.dxf.alignH
import com.jpaver.trianglelist.dxf.alignV
import com.jpaver.trianglelist.cadview.ColorConverter

/**
 * デスクトップ版テキスト描画クラス
 * DXFテキストエンティティをCompose Canvasに描画する機能を提供
 */
class TextRenderer {
    
    /**
     * DXFテキストを描画する
     * @param drawScope 描画スコープ
     * @param text DXFテキストデータ
     * @param scale 現在のスケール
     * @param textMeasurer テキスト測定器
     */
    fun drawText(
        drawScope: DrawScope, 
        text: DxfText, 
        scale: Float,
        textMeasurer: androidx.compose.ui.text.TextMeasurer
    ) {
        val color = ColorConverter.aciToColor(text.color)
        val textStyle = TextStyle(
            color = color,
            fontSize = (text.height * scale).sp
        )
        
        // テキストのサイズを測定
        val textLayoutResult = textMeasurer.measure(text.text, textStyle)
        val textWidth = textLayoutResult.size.width.toFloat()
        val textHeight = textLayoutResult.size.height.toFloat()
        
        // アライメントに基づいた位置を計算
        val adjustedPosition = calculateAlignedPosition(
            text.x.toFloat(),
            text.y.toFloat(),
            textWidth,
            textHeight,
            text.alignH,
            text.alignV
        )
        
        // 回転を適用
        if (text.rotation != 0.0) {
            drawScope.drawContext.canvas.save()
            drawScope.drawContext.transform.rotate(
                degrees = text.rotation.toFloat(),
                pivot = androidx.compose.ui.geometry.Offset(text.x.toFloat(), text.y.toFloat())
            )
        }
        
        // テキストを描画
        drawScope.drawText(
            textLayoutResult = textLayoutResult,
            topLeft = adjustedPosition
        )
        
        // 回転を復元
        if (text.rotation != 0.0) {
            drawScope.drawContext.canvas.restore()
        }
    }
    
    /**
     * テキストの境界ボックスを計算する
     * @param text DXFテキストデータ
     * @param scale 現在のスケール
     * @param textMeasurer テキスト測定器
     * @return 境界ボックスの座標 (minX, maxX, minY, maxY)
     */
    fun calculateTextBounds(
        text: DxfText, 
        scale: Float,
        textMeasurer: androidx.compose.ui.text.TextMeasurer
    ): List<Float> {
        val fontSize = (text.height * scale).sp
        val textStyle = TextStyle(fontSize = fontSize)
        val textLayoutResult = textMeasurer.measure(text.text, textStyle)
        
        val textWidth = textLayoutResult.size.width.toFloat()
        val textHeight = textLayoutResult.size.height.toFloat()
        
        // アライメントに基づいた実際の描画位置を計算
        val adjustedPosition = calculateAlignedPosition(
            text.x.toFloat(),
            text.y.toFloat(),
            textWidth,
            textHeight,
            text.alignH,
            text.alignV
        )
        
        val minX = adjustedPosition.x
        val maxX = adjustedPosition.x + textWidth
        val minY = adjustedPosition.y
        val maxY = adjustedPosition.y + textHeight
        
        return listOf(minX, maxX, minY, maxY)
    }
    
    /**
     * アライメントに基づいてテキストの描画位置を計算する
     * @param baseX 基準X座標
     * @param baseY 基準Y座標
     * @param textWidth テキスト幅
     * @param textHeight テキスト高さ
     * @param alignH 水平アライメント（0=左、1=中央、2=右）
     * @param alignV 垂直アライメント（0=ベースライン、1=下、2=中央、3=上）
     * @return 調整された描画位置
     */
    private fun calculateAlignedPosition(
        baseX: Float,
        baseY: Float,
        textWidth: Float,
        textHeight: Float,
        alignH: Int,
        alignV: Int
    ): androidx.compose.ui.geometry.Offset {
        // 水平アライメントによるX座標の調整
        val adjustedX = when (alignH) {
            0 -> baseX // 左揃え（デフォルト）
            1 -> baseX - textWidth / 2 // 中央揃え
            2 -> baseX - textWidth // 右揃え
            else -> baseX
        }
        
        // 垂直アライメントによるY座標の調整
        val adjustedY = when (alignV) {
            0 -> baseY // ベースライン（デフォルト）
            1 -> baseY - textHeight // 下揃え
            2 -> baseY - textHeight / 2 // 中央揃え
            3 -> baseY // 上揃え
            else -> baseY
        }
        
        return androidx.compose.ui.geometry.Offset(adjustedX, adjustedY)
    }
} 