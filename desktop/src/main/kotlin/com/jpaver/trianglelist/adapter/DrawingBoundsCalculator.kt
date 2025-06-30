package com.jpaver.trianglelist.adapter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextMeasurer
import com.jpaver.trianglelist.dxf.DxfParseResult
import com.jpaver.trianglelist.dxf.DxfText

/**
 * 図面の境界を計算するクラス
 * 線分、円、ポリライン、テキストを含むすべてのエンティティから境界を計算
 */
class DrawingBoundsCalculator {
    
    /**
     * 図面の境界を計算する
     * @param parseResult DXF解析結果
     * @param textMeasurer テキスト測定器（テキスト境界計算用）
     * @param scale 現在のスケール
     * @return 境界座標 (minX, maxX, minY, maxY)
     */
    fun calculateBounds(
        parseResult: DxfParseResult,
        textMeasurer: TextMeasurer,
        scale: Float = 1.0f
    ): List<Float> {
        val allPoints = mutableListOf<Offset>()
        
        // 線分の点を追加
        parseResult.lines.forEach { line ->
            allPoints.add(Offset(line.x1.toFloat(), line.y1.toFloat()))
            allPoints.add(Offset(line.x2.toFloat(), line.y2.toFloat()))
        }
        
        // 円の境界を追加
        parseResult.circles.forEach { circle ->
            allPoints.add(Offset((circle.centerX - circle.radius).toFloat(), (circle.centerY - circle.radius).toFloat()))
            allPoints.add(Offset((circle.centerX + circle.radius).toFloat(), (circle.centerY + circle.radius).toFloat()))
        }
        
        // ポリラインの頂点を追加
        parseResult.lwPolylines.forEach { polyline ->
            polyline.vertices.forEach { (x, y) ->
                allPoints.add(Offset(x.toFloat(), y.toFloat()))
            }
        }
        
        // テキストの境界を追加
        val textRenderer = TextRenderer()
        parseResult.texts.forEach { text ->
            val textBounds = textRenderer.calculateTextBounds(text, scale, textMeasurer)
            allPoints.add(Offset(textBounds[0], textBounds[2])) // minX, minY
            allPoints.add(Offset(textBounds[1], textBounds[3])) // maxX, maxY
        }
        
        return if (allPoints.isEmpty()) {
            listOf(0f, 100f, 0f, 100f) // デフォルト境界
        } else {
            listOf(
                allPoints.minOf { it.x },
                allPoints.maxOf { it.x },
                allPoints.minOf { it.y },
                allPoints.maxOf { it.y }
            )
        }
    }
    
    /**
     * ヘッダー情報から境界を取得する
     * @param parseResult DXF解析結果
     * @return 境界座標 (minX, maxX, minY, maxY) または null
     */
    fun getBoundsFromHeader(parseResult: DxfParseResult): List<Float>? {
        val header = parseResult.header ?: return null
        
        return if (header.extMax.first > header.extMin.first && 
                   header.extMax.second > header.extMin.second) {
            listOf(
                header.extMin.first.toFloat(),
                header.extMax.first.toFloat(), 
                header.extMin.second.toFloat(),
                header.extMax.second.toFloat()
            )
        } else {
            null
        }
    }
} 