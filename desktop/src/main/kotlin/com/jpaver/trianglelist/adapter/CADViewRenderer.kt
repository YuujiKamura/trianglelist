package com.jpaver.trianglelist.adapter

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextMeasurer
import com.jpaver.trianglelist.dxf.DxfParseResult
import com.jpaver.trianglelist.util.CanvasUtil
import com.jpaver.trianglelist.cadview.ColorConverter

/**
 * CADView用の描画統合クラス
 * 線分、円、ポリライン、テキストの描画を統合管理
 */
class CADViewRenderer {
    private val textRenderer = TextRenderer()
    private val boundsCalculator = DrawingBoundsCalculator()
    
    /**
     * すべてのエンティティを描画する
     * @param drawScope 描画スコープ
     * @param parseResult DXF解析結果
     * @param scale 現在のスケール
     * @param textMeasurer テキスト測定器
     * @param debugMode デバッグモード（true時にテキストの描画起点・範囲をボックス表示）
     */
    fun drawAllEntities(
        drawScope: DrawScope,
        parseResult: DxfParseResult,
        scale: Float,
        textMeasurer: TextMeasurer,
        debugMode: Boolean = false
    ) {
        // 事前にY軸反転したデータを取得
        val data = CanvasUtil.flipYAxis(parseResult)
        // 線分を描画
        data.lines.forEach { line ->
            val color = ColorConverter.aciToColor(line.color)
            drawScope.drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(line.x1.toFloat(), line.y1.toFloat()),
                end = androidx.compose.ui.geometry.Offset(line.x2.toFloat(), line.y2.toFloat()),
                strokeWidth = 1f / scale
            )
        }
        
        // 円を描画
        data.circles.forEach { circle ->
            val color = ColorConverter.aciToColor(circle.color)
            drawScope.drawCircle(
                color = color,
                radius = circle.radius.toFloat(),
                center = androidx.compose.ui.geometry.Offset(circle.centerX.toFloat(), circle.centerY.toFloat()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f / scale)
            )
        }
        
        // ポリラインを描画
        data.lwPolylines.forEach { polyline ->
            if (polyline.vertices.size >= 2) {
                val color = ColorConverter.aciToColor(polyline.color)
                val path = Path()
                val firstVertex = polyline.vertices.first()
                path.moveTo(firstVertex.first.toFloat(), firstVertex.second.toFloat())
                
                polyline.vertices.drop(1).forEach { vertex ->
                    path.lineTo(vertex.first.toFloat(), vertex.second.toFloat())
                }
                
                if (polyline.isClosed && polyline.vertices.size > 2) {
                    path.close()
                }
                
                drawScope.drawPath(
                    path = path,
                    color = color,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f / scale)
                )
            }
        }
        
        // テキストを描画
        data.texts.forEach { text ->
            textRenderer.drawText(drawScope, text, scale, textMeasurer, debugMode)
        }
    }
    
    /**
     * 図面の境界を計算する
     * @param parseResult DXF解析結果
     * @param textMeasurer テキスト測定器
     * @param scale 現在のスケール
     * @return 境界座標 (minX, maxX, minY, maxY)
     */
    fun calculateDrawingBounds(
        parseResult: DxfParseResult,
        textMeasurer: TextMeasurer,
        scale: Float = 1.0f
    ): List<Float> {
        // 事前にY軸反転したデータを使用
        val flipped = CanvasUtil.flipYAxis(parseResult)
        // ヘッダー情報を優先
        val headerBounds = boundsCalculator.getBoundsFromHeader(flipped)
        if (headerBounds != null) {
            return headerBounds
        }
        
        // エンティティから計算
        return boundsCalculator.calculateBounds(flipped, textMeasurer, scale)
    }
}