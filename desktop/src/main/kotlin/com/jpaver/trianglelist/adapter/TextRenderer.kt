package com.jpaver.trianglelist.adapter

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.jpaver.trianglelist.dxf.DxfText
import com.jpaver.trianglelist.cadview.ColorConverter
import com.jpaver.trianglelist.dxf.calculateAlignedTopLeft

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
     * @param debugMode デバッグモード（true時に描画起点・テキスト範囲のボックスを表示）
     */
    fun drawText(
        drawScope: DrawScope, 
        text: DxfText, 
        scale: Float,
        textMeasurer: androidx.compose.ui.text.TextMeasurer,
        debugMode: Boolean = false
    ) {
        val color = ColorConverter.aciToColor(text.color)
        val textStyle = TextStyle(
            color = color,
            // scaleによるサイズ変更を取り除き、DXFサイズをそのまま使用
            fontSize = text.height.sp
        )
        
        // テキストのサイズを測定
        val textLayoutResult = textMeasurer.measure(text.text, textStyle)
        val textWidth = textLayoutResult.size.width.toFloat()
        val textHeight = textLayoutResult.size.height.toFloat() // 実際の描画高さを使用
        
        // 頂点データは既にY反転済みなので、そのまま使用
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
            // データYは既に反転済みなのでそのまま回転ピボットに使用
            drawScope.drawContext.transform.rotate(
                degrees = -text.rotation.toFloat(),
                pivot = androidx.compose.ui.geometry.Offset(text.x.toFloat(), text.y.toFloat())
            )
        }
        
        // テキストを描画
        drawScope.drawText(
            textLayoutResult = textLayoutResult,
            topLeft = adjustedPosition
        )
        
        // デバッグモード時に描画起点とテキスト範囲をボックスで可視化
        if (debugMode) {
            drawDebugBoxes(drawScope, text, adjustedPosition, textWidth, textHeight)
        }
        
        // 回転を復元
        if (text.rotation != 0.0) {
            drawScope.drawContext.canvas.restore()
        }
    }
    
    /**
     * テキストの境界ボックスを計算する
     * 実際の描画処理と全く同じロジックを使用して一貫性を保つ
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
        // drawTextメソッドと全く同じロジックを使用
        val textStyle = TextStyle(
            fontSize = text.height.sp // scaleは適用しない（drawTextと同じ）
        )
        
        val textLayoutResult = textMeasurer.measure(text.text, textStyle)
        val textWidth = textLayoutResult.size.width.toFloat()
        val textHeight = textLayoutResult.size.height.toFloat() // 実際の描画高さを使用
        
        // アライメントに基づいた実際の描画位置を計算（drawTextと同じロジック）
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
        // DXFのテキスト位置をComposeの描画位置（左上）に変換する
        // DXFでは(baseX, baseY)がテキストの基準点、Composeでは左上が基準
        
        // 水平アライメント
        val x = when (alignH) {
            0 -> baseX // 左揃え：そのまま
            1 -> baseX - textWidth / 2 // 中央揃え：半分左に
            2 -> baseX - textWidth // 右揃え：全幅左に
            else -> baseX
        }
        
        // 垂直アライメント（Composeのテキスト描画の特性に合わせて調整）
        val y = when (alignV) {
            0 -> baseY - textHeight // ベースライン：テキスト高さ分上に（Composeは左上基準のため）
            1 -> baseY - textHeight // 下揃え：テキスト高さ分上に
            2 -> baseY - textHeight / 2 // 中央揃え：半分上に
            3 -> baseY // 上揃え：そのまま
            else -> baseY - textHeight // デフォルトはベースライン
        }
        
        return Offset(x, y)
    }

    /**
     * デバッグ用のボックスを描画する
     * 1. 描画起点（DXFの基準点）- 小さい赤いボックス
     * 2. テキスト範囲（実際のテキスト領域）- 青い枠線
     * 3. DXF高さボックス（参考用）- 緑の点線
     * @param drawScope 描画スコープ
     * @param text DXFテキストデータ
     * @param adjustedPosition 調整済み描画位置（左上）
     * @param textWidth テキスト幅
     * @param textHeight テキスト高さ
     */
    private fun drawDebugBoxes(
        drawScope: DrawScope,
        text: DxfText,
        adjustedPosition: Offset,
        textWidth: Float,
        textHeight: Float
    ) {
        // 1. 描画起点（DXFの基準点）を小さい赤いボックスで表示
        val originSize = 10f
        drawScope.drawRect(
            color = Color.Red,
            topLeft = Offset(
                text.x.toFloat() - originSize / 2,
                text.y.toFloat() - originSize / 2
            ),
            size = Size(originSize, originSize),
            style = Stroke(width = 3f)
        )
        
        // 2. テキスト範囲を青い枠線で表示（実際の描画と同じサイズ）
        drawScope.drawRect(
            color = Color.Blue,
            topLeft = adjustedPosition,
            size = Size(textWidth, textHeight),
            style = Stroke(width = 2f)
        )
        
        // 3. DXF高さボックス（参考用）を緑の点線で表示
        val dxfHeight = text.height.toFloat()
        if (dxfHeight != textHeight) {
            val dxfY = when (text.alignV) {
                0 -> text.y.toFloat() - dxfHeight // ベースライン
                1 -> text.y.toFloat() - dxfHeight // 下揃え
                2 -> text.y.toFloat() - dxfHeight / 2 // 中央揃え
                3 -> text.y.toFloat() // 上揃え
                else -> text.y.toFloat() - dxfHeight
            }
            
            val dxfX = when (text.alignH) {
                0 -> text.x.toFloat() // 左揃え
                1 -> text.x.toFloat() - textWidth / 2 // 中央揃え
                2 -> text.x.toFloat() - textWidth // 右揃え
                else -> text.x.toFloat()
            }
            
            drawScope.drawRect(
                color = Color.Green,
                topLeft = Offset(dxfX, dxfY),
                size = Size(textWidth, dxfHeight),
                style = Stroke(width = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
            )
        }
        
        // 4. デバッグ情報をコンソールに出力
        println("=== Text Debug Info ===")
        println("Text: '${text.text}'")
        println("DXF Position: (${text.x}, ${text.y})")
        println("DXF Height: ${text.height}")
        println("Measured Height: $textHeight")
        println("DXF Align: H=${text.alignH}, V=${text.alignV}")
        println("Measured Width: $textWidth")
        println("Adjusted Position: $adjustedPosition")
        println("========================")
    }
}