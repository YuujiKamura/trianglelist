package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LabelBoxTest {
    private val delta = 0.001f

    private fun assertHasCorner(corners: List<PointXY>, x: Float, y: Float) {
        val found = corners.any { abs(it.x - x) < delta && abs(it.y - y) < delta }
        assertTrue(found, "期待した隅 ($x, $y) が corners に無い: $corners")
    }

    @Test
    fun `回転なしの OBB は中心まわりに4隅を返す`() {
        val box = LabelBox(PointXY(0f, 0f), widthMm = 4f, heightMm = 2f)
        val corners = box.corners()
        assertHasCorner(corners, -2f, -1f)
        assertHasCorner(corners, 2f, -1f)
        assertHasCorner(corners, 2f, 1f)
        assertHasCorner(corners, -2f, 1f)
    }

    @Test
    fun `90度回転で幅と高さが入れ替わった4隅を返す`() {
        val box = LabelBox(PointXY(0f, 0f), widthMm = 4f, heightMm = 2f, rotationDeg = 90f)
        val corners = box.corners()
        assertHasCorner(corners, 1f, -2f)
        assertHasCorner(corners, 1f, 2f)
        assertHasCorner(corners, -1f, 2f)
        assertHasCorner(corners, -1f, -2f)
    }

    @Test
    fun `回転なしで重なる OBB 同士は交差を検出する`() {
        val a = LabelBox(PointXY(0f, 0f), widthMm = 4f, heightMm = 4f)
        val b = LabelBox(PointXY(2f, 2f), widthMm = 4f, heightMm = 4f)
        assertTrue(a.intersects(b))
    }

    @Test
    fun `回転なしで離れた OBB 同士は交差しない`() {
        val a = LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f)
        val b = LabelBox(PointXY(5f, 5f), widthMm = 2f, heightMm = 2f)
        assertFalse(a.intersects(b))
    }

    @Test
    fun `小さい OBB を大きい OBB が完全に内包するとき交差を検出する`() {
        val outer = LabelBox(PointXY(0f, 0f), widthMm = 10f, heightMm = 10f)
        val inner = LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f)
        assertTrue(outer.intersects(inner))
    }

    @Test
    fun `45度回転した OBB が軸並行 OBB と角で重なるとき交差を検出する`() {
        val axisAligned = LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f)
        val rotated = LabelBox(PointXY(2f, 0f), widthMm = 2f, heightMm = 2f, rotationDeg = 45f)
        assertTrue(axisAligned.intersects(rotated))
    }

    @Test
    fun `45度回転した OBB が離れていれば交差しない`() {
        val axisAligned = LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f)
        val rotated = LabelBox(PointXY(5f, 0f), widthMm = 2f, heightMm = 2f, rotationDeg = 45f)
        assertFalse(axisAligned.intersects(rotated))
    }

    @Test
    fun `OBB を貫く線分は交差を検出する`() {
        val box = LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f)
        assertTrue(box.intersectsSegment(PointXY(-3f, 0f), PointXY(3f, 0f)))
    }

    @Test
    fun `OBB の外を通る線分は交差しない`() {
        val box = LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f)
        assertFalse(box.intersectsSegment(PointXY(-3f, 3f), PointXY(3f, 3f)))
    }

    @Test
    fun `線分の端点が OBB の辺に接触するのは交差として検出する`() {
        val box = LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f)
        // 端点 (1, 0) は右辺 x=1 上、線分はそこから外へ伸びる (かすめ)
        assertTrue(box.intersectsSegment(PointXY(1f, 0f), PointXY(3f, 0f)))
    }
}
