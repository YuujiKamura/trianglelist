package com.jpaver.trianglelist.dxf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DxfIndexTest {

    @Test
    fun testStationExtraction() {
        // テスト用のDxfParseResult
        val texts = listOf(
            DxfText(49721.0, -130267.0, "No.0", 1994.3, 0.0, 5, 1, 1, "測点"),
            DxfText(69721.0, -130267.0, "No.1", 1994.3, 0.0, 5, 1, 1, "測点"),
            DxfText(109721.0, -130267.0, "No.3", 1994.3, 0.0, 5, 1, 1, "測点"),
            DxfText(120721.0, -130267.0, "No.3+11", 1994.3, 0.0, 5, 1, 1, "測点"),
            DxfText(124721.0, -130267.0, "No.3+15", 1994.3, 0.0, 5, 1, 1, "測点")
        )
        val parseResult = DxfParseResult(
            lines = emptyList(),
            circles = emptyList(),
            lwPolylines = emptyList(),
            texts = texts
        )

        val index = DxfIndex(parseResult)

        // 測点座標の確認
        val no0 = index.getStationCoord("No.0")
        assertNotNull(no0)
        assertEquals(49721.0, no0.first, 1.0)

        val no3 = index.getStationCoord("No.3")
        assertNotNull(no3)
        assertEquals(109721.0, no3.first, 1.0)

        val no3_11 = index.getStationCoord("No.3+11")
        assertNotNull(no3_11)
        assertEquals(120721.0, no3_11.first, 1.0)

        // 2点間の中間点
        val midpoint = index.getMidpointBetweenStations("No.3+11", "No.3+15")
        assertNotNull(midpoint)
        assertEquals(122721.0, midpoint.first, 1.0)
    }

    @Test
    fun testCenterlineAlignment() {
        // センターラインと測点の関係を確認
        // 横断歩道はセンターライン上のNo.3+11～No.3+15の中間に配置されるべき

        val centerlines = listOf(
            DxfLine(49721.0, -115000.0, 150000.0, -115000.0, 7, "中心線")
        )
        val texts = listOf(
            DxfText(109721.0, -130267.0, "No.3", 1994.3, 0.0, 5, 1, 1, "測点"),
            DxfText(120721.0, -130267.0, "No.3+11", 1994.3, 0.0, 5, 1, 1, "測点"),
            DxfText(124721.0, -130267.0, "No.3+15", 1994.3, 0.0, 5, 1, 1, "測点")
        )
        val parseResult = DxfParseResult(
            lines = centerlines,
            circles = emptyList(),
            lwPolylines = emptyList(),
            texts = texts
        )

        val index = DxfIndex(parseResult)

        // No.3+11の座標
        val no3_11 = index.getStationCoord("No.3+11")
        assertNotNull(no3_11)

        // センターラインの始点からNo.3+11までの距離
        val centerlineStart = centerlines[0].x1
        val distanceFromStart = no3_11.first - centerlineStart

        println("センターライン始点: $centerlineStart")
        println("No.3+11 X座標: ${no3_11.first}")
        println("始点からの距離: $distanceFromStart")

        // この距離がstartOffsetとして使用されるべき
        assertTrue(distanceFromStart > 0)
    }
}
