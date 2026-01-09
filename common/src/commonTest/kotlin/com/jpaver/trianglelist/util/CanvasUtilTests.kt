package com.jpaver.trianglelist.util

import com.jpaver.trianglelist.dxf.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CanvasUtilTests {
    private val delta = 0.0001

    @Test
    fun flipYAxis_line() {
        val line = DxfLine(1.0, 2.0, 3.0, -4.0)
        val result = CanvasUtil.flipYAxis(DxfParseResult(lines = listOf(line)))
        val flipped = result.lines.first()
        assertEquals(1.0, flipped.x1)
        assertEquals(-2.0, flipped.y1)
        assertEquals(3.0, flipped.x2)
        assertEquals(4.0, flipped.y2)
    }

    @Test
    fun flipYAxis_circle() {
        val circle = DxfCircle(5.0, -6.0, 7.0)
        val result = CanvasUtil.flipYAxis(DxfParseResult(circles = listOf(circle)))
        val flipped = result.circles.first()
        assertEquals(5.0, flipped.centerX)
        assertEquals(6.0, flipped.centerY)
        assertEquals(7.0, flipped.radius)
    }

    @Test
    fun flipYAxis_polyline() {
        val poly = DxfLwPolyline(vertices = listOf(1.0 to 2.0, 3.0 to -4.0))
        val result = CanvasUtil.flipYAxis(DxfParseResult(lwPolylines = listOf(poly)))
        val flippedVerts = result.lwPolylines.first().vertices
        assertEquals(1.0, flippedVerts[0].first)
        assertEquals(-2.0, flippedVerts[0].second)
        assertEquals(3.0, flippedVerts[1].first)
        assertEquals(4.0, flippedVerts[1].second)
    }

    @Test
    fun flipYAxis_text() {
        val text = DxfText(8.0, -9.0, "t")
        val result = CanvasUtil.flipYAxis(DxfParseResult(texts = listOf(text)))
        val flipped = result.texts.first()
        assertEquals(8.0, flipped.x)
        assertEquals(9.0, flipped.y)
        assertEquals("t", flipped.text)
    }

    @Test
    fun flipYAxis_header() {
        val header = DxfHeader(
            acadVer = "X",
            insUnits = 1,
            extMin = Triple(0.0, 10.0, 0.0),
            extMax = Triple(5.0, -15.0, 0.0),
            limMin = Pair(2.0, 3.0),
            limMax = Pair(-4.0, -6.0),
            insBase = Triple(7.0, 8.0, 0.0),
            dimScale = 1.0
        )
        val result = CanvasUtil.flipYAxis(DxfParseResult(header = header))
        val flipped = result.header
        assertNotNull(flipped)
        assertEquals(0.0, flipped!!.extMin.first)
        assertEquals(-10.0, flipped.extMin.second)
        assertEquals(5.0, flipped.extMax.first)
        assertEquals(15.0, flipped.extMax.second)
        assertEquals(2.0, flipped.limMin.first)
        assertEquals(-3.0, flipped.limMin.second)
        assertEquals(-4.0, flipped.limMax.first)
        assertEquals(6.0, flipped.limMax.second)
        assertEquals(7.0, flipped.insBase.first)
        assertEquals(-8.0, flipped.insBase.second)
    }

    @Test
    fun flipYAxis_noHeader() {
        val result = CanvasUtil.flipYAxis(DxfParseResult())
        assertNull(result.header)
    }
}
