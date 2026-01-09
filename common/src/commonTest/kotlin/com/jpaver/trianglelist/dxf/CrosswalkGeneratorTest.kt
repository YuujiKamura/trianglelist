package com.jpaver.trianglelist.dxf

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CrosswalkGeneratorのテスト
 *
 * 横断歩道:
 * - ストライプは道路を横断する方向に延びる
 * - ストライプはセンターラインを軸に左右対称配置
 * - 7本の場合: 左3本 + 中央1本 + 右3本
 */
class CrosswalkGeneratorTest {

    private val generator = CrosswalkGenerator()
    private val epsilon = 0.001

    @Test
    fun generateCrosswalk_singleStripe_generatesRectangle() {
        val centerlines = listOf(
            DxfLine(0.0, 0.0, 10000.0, 0.0, 7, "中心線")
        )

        val result = generator.generateCrosswalk(
            centerlineLines = centerlines,
            startOffset = 1000.0,
            stripeLength = 4000.0,
            stripeWidth = 450.0,
            stripeCount = 1,
            stripeSpacing = 0.0,
            layer = "横断歩道"
        )

        assertEquals(4, result.size, "1本のストライプは4本の線で構成される")
        assertTrue(result.all { it.layer == "横断歩道" })
    }

    @Test
    fun generateCrosswalk_sevenStripes_generates28Lines() {
        val centerlines = listOf(
            DxfLine(0.0, 0.0, 20000.0, 0.0, 7, "中心線")
        )

        val result = generator.generateCrosswalk(
            centerlineLines = centerlines,
            startOffset = 11000.0,
            stripeLength = 4000.0,
            stripeWidth = 450.0,
            stripeCount = 7,
            stripeSpacing = 450.0,
            layer = "横断歩道"
        )

        assertEquals(28, result.size, "7本のストライプは28本の線で構成される")
    }

    @Test
    fun generateCrosswalk_emptyCenterlines_returnsEmpty() {
        val result = generator.generateCrosswalk(
            centerlineLines = emptyList(),
            startOffset = 1000.0,
            stripeLength = 4000.0,
            stripeWidth = 450.0,
            stripeCount = 7,
            stripeSpacing = 450.0
        )

        assertTrue(result.isEmpty(), "中心線が空の場合は空リストを返す")
    }

    @Test
    fun filterCenterlinesByLayer_matchesPattern() {
        val lines = listOf(
            DxfLine(0.0, 0.0, 100.0, 0.0, 7, "中心線"),
            DxfLine(0.0, 0.0, 100.0, 0.0, 7, "中心-道路"),
            DxfLine(0.0, 0.0, 100.0, 0.0, 7, "ガードレール"),
            DxfLine(0.0, 0.0, 100.0, 0.0, 7, "CENTER_LINE")
        )

        val filtered = generator.filterCenterlinesByLayer(lines, "中心")
        assertEquals(2, filtered.size)
    }

    @Test
    fun polylineToLines_openPolyline() {
        val polyline = DxfLwPolyline(
            vertices = listOf(
                Pair(0.0, 0.0),
                Pair(100.0, 0.0),
                Pair(200.0, 50.0)
            ),
            isClosed = false,
            color = 3,
            layer = "道路中心"
        )

        val lines = generator.polylineToLines(polyline)
        assertEquals(2, lines.size)
        assertTrue(lines.all { it.layer == "道路中心" })
    }

    @Test
    fun polylineToLines_closedPolyline() {
        val polyline = DxfLwPolyline(
            vertices = listOf(
                Pair(0.0, 0.0),
                Pair(100.0, 0.0),
                Pair(50.0, 100.0)
            ),
            isClosed = true,
            color = 5,
            layer = "境界"
        )

        val lines = generator.polylineToLines(polyline)
        assertEquals(3, lines.size)
    }

    @Test
    fun generateCrosswalk_stripesCenteredOnCenterline() {
        // 水平中心線
        val centerlines = listOf(
            DxfLine(0.0, 0.0, 20000.0, 0.0, 7, "中心線")
        )

        val result = generator.generateCrosswalk(
            centerlineLines = centerlines,
            startOffset = 10000.0,
            stripeLength = 4000.0,
            stripeWidth = 450.0,
            stripeCount = 7,
            stripeSpacing = 450.0,
            layer = "横断歩道"
        )

        // Y座標の範囲を確認（センターラインY=0を軸に対称）
        val allY = result.flatMap { listOf(it.y1, it.y2) }
        val minY = allY.minOrNull() ?: 0.0
        val maxY = allY.maxOrNull() ?: 0.0

        // ストライプはY方向に4000mm延びる（センターから±2000mm）
        assertTrue(minY < 0 && maxY > 0, "ストライプはセンターラインを軸に左右に配置: minY=$minY, maxY=$maxY")
        assertTrue(abs(minY + maxY) < 1.0, "ストライプはセンターライン軸に対称: minY=$minY, maxY=$maxY")
    }

    @Test
    fun generateCrosswalk_stripesAlongRoadDirection() {
        // 水平中心線
        val centerlines = listOf(
            DxfLine(0.0, 0.0, 20000.0, 0.0, 7, "中心線")
        )

        val result = generator.generateCrosswalk(
            centerlineLines = centerlines,
            startOffset = 10000.0,
            stripeLength = 4000.0,
            stripeWidth = 450.0,
            stripeCount = 7,
            stripeSpacing = 450.0,
            layer = "横断歩道"
        )

        // X座標の範囲（7本のストライプが道路方向に並ぶ）
        val allX = result.flatMap { listOf(it.x1, it.x2) }
        val minX = allX.minOrNull() ?: 0.0
        val maxX = allX.maxOrNull() ?: 0.0

        // 総幅 = 7*450 + 6*450 = 5850mm、中心は10000
        // X範囲: 10000 - 2925 - 225 = 6850 ~ 10000 + 2925 + 225 = 13150
        val expectedCenter = 10000.0
        val expectedHalfWidth = (7 * 450.0 + 6 * 450.0) / 2
        assertTrue(minX >= expectedCenter - expectedHalfWidth - 250, "minX=$minX")
        assertTrue(maxX <= expectedCenter + expectedHalfWidth + 250, "maxX=$maxX")
    }
}
