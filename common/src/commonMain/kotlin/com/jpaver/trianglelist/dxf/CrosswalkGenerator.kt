package com.jpaver.trianglelist.dxf

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 横断歩道生成クラス
 * 中心線データから横断歩道のストライプを生成
 *
 * 横断歩道の構成:
 * - ストライプは道路を横断する方向に延びる（道路に垂直）
 * - ストライプはセンターラインを軸に左右対称に配置
 * - 例: 7本の場合、左3本 + 中央1本 + 右3本
 */
class CrosswalkGenerator {

    /**
     * 横断歩道のストライプを生成
     *
     * @param centerlineLines 中心線のLINEエンティティリスト
     * @param startOffset 開始位置（中心線の始点からのオフセット、mm）
     * @param stripeLength ストライプの長さ（道路を横断する方向、センターライン軸に左右に延びる、mm）
     * @param stripeWidth ストライプの幅（道路方向、mm）
     * @param stripeCount ストライプ本数
     * @param stripeSpacing ストライプ間隔（道路方向、mm）
     * @param layer 出力レイヤー名
     * @return 横断歩道を構成するLINEエンティティのリスト
     */
    fun generateCrosswalk(
        centerlineLines: List<DxfLine>,
        startOffset: Double,
        stripeLength: Double,
        stripeWidth: Double,
        stripeCount: Int,
        stripeSpacing: Double,
        layer: String = "横断歩道"
    ): List<DxfLine> {
        if (centerlineLines.isEmpty()) return emptyList()

        val path = buildCenterlinePath(centerlineLines)
        if (path.isEmpty()) return emptyList()

        val centerPoint = getPointAtDistance(path, startOffset) ?: return emptyList()
        val nextPoint = getPointAtDistance(path, startOffset + 100) ?: centerPoint

        val roadAngle = atan2(nextPoint.second - centerPoint.second, nextPoint.first - centerPoint.first)
        val stripeAngle = roadAngle + Math.PI / 2

        val totalRoadWidth = stripeCount * stripeWidth + (stripeCount - 1) * stripeSpacing
        val halfRoadWidth = totalRoadWidth / 2

        val result = mutableListOf<DxfLine>()

        for (i in 0 until stripeCount) {
            val roadOffset = -halfRoadWidth + stripeWidth / 2 + i * (stripeWidth + stripeSpacing)
            val stripeCenterX = centerPoint.first + roadOffset * cos(roadAngle)
            val stripeCenterY = centerPoint.second + roadOffset * sin(roadAngle)

            val stripeLines = generateStripeRectangle(
                stripeCenterX, stripeCenterY,
                roadAngle, stripeAngle,
                stripeLength, stripeWidth, layer
            )
            result.addAll(stripeLines)
        }

        return result
    }

    private fun buildCenterlinePath(lines: List<DxfLine>): List<Pair<Double, Double>> {
        if (lines.isEmpty()) return emptyList()
        val path = mutableListOf<Pair<Double, Double>>()
        path.add(Pair(lines[0].x1, lines[0].y1))
        for (line in lines) {
            path.add(Pair(line.x2, line.y2))
        }
        return path
    }

    private fun getPointAtDistance(path: List<Pair<Double, Double>>, distance: Double): Pair<Double, Double>? {
        if (path.size < 2) return null
        var accumulatedDistance = 0.0
        for (i in 0 until path.size - 1) {
            val p1 = path[i]
            val p2 = path[i + 1]
            val segmentLength = sqrt(
                (p2.first - p1.first) * (p2.first - p1.first) +
                (p2.second - p1.second) * (p2.second - p1.second)
            )
            if (accumulatedDistance + segmentLength >= distance) {
                val remainingDistance = distance - accumulatedDistance
                val ratio = if (segmentLength > 0) remainingDistance / segmentLength else 0.0
                return Pair(
                    p1.first + (p2.first - p1.first) * ratio,
                    p1.second + (p2.second - p1.second) * ratio
                )
            }
            accumulatedDistance += segmentLength
        }
        return path.lastOrNull()
    }

    private fun generateStripeRectangle(
        centerX: Double,
        centerY: Double,
        roadAngle: Double,
        stripeAngle: Double,
        stripeLength: Double,
        stripeWidth: Double,
        layer: String
    ): List<DxfLine> {
        val halfLength = stripeLength / 2
        val halfWidth = stripeWidth / 2
        val dx = cos(roadAngle)
        val dy = sin(roadAngle)
        val px = cos(stripeAngle)
        val py = sin(stripeAngle)

        val corner1 = Pair(centerX - halfLength * px - halfWidth * dx, centerY - halfLength * py - halfWidth * dy)
        val corner2 = Pair(centerX + halfLength * px - halfWidth * dx, centerY + halfLength * py - halfWidth * dy)
        val corner3 = Pair(centerX + halfLength * px + halfWidth * dx, centerY + halfLength * py + halfWidth * dy)
        val corner4 = Pair(centerX - halfLength * px + halfWidth * dx, centerY - halfLength * py + halfWidth * dy)

        return listOf(
            DxfLine(corner1.first, corner1.second, corner2.first, corner2.second, 7, layer),
            DxfLine(corner2.first, corner2.second, corner3.first, corner3.second, 7, layer),
            DxfLine(corner3.first, corner3.second, corner4.first, corner4.second, 7, layer),
            DxfLine(corner4.first, corner4.second, corner1.first, corner1.second, 7, layer)
        )
    }

    fun filterCenterlinesByLayer(lines: List<DxfLine>, layerPattern: String): List<DxfLine> {
        return lines.filter { it.layer.contains(layerPattern, ignoreCase = true) }
    }

    fun polylineToLines(polyline: DxfLwPolyline): List<DxfLine> {
        val result = mutableListOf<DxfLine>()
        val vertices = polyline.vertices
        for (i in 0 until vertices.size - 1) {
            result.add(DxfLine(
                vertices[i].first, vertices[i].second,
                vertices[i + 1].first, vertices[i + 1].second,
                polyline.color, polyline.layer
            ))
        }
        if (polyline.isClosed && vertices.size > 2) {
            result.add(DxfLine(
                vertices.last().first, vertices.last().second,
                vertices.first().first, vertices.first().second,
                polyline.color, polyline.layer
            ))
        }
        return result
    }
}
