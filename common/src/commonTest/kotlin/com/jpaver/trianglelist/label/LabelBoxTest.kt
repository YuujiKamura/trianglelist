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

    private fun assertDepth(expected: Float, actual: Float?, message: String) {
        assertTrue(actual != null && abs(actual - expected) < delta, "$message: expected=$expected actual=$actual")
    }

    @Test
    fun `部分的に重なる OBB の深さは食い込み量になる`() {
        val a = LabelBox(PointXY(0f, 0f), widthMm = 4f, heightMm = 4f)
        val b = LabelBox(PointXY(3f, 0f), widthMm = 4f, heightMm = 4f)
        assertDepth(1f, a.penetrationDepth(b), "x 方向に 1 食い込むはず")
    }

    @Test
    fun `境界接触の OBB の深さは 0`() {
        val a = LabelBox(PointXY(0f, 0f), widthMm = 4f, heightMm = 4f)
        val b = LabelBox(PointXY(4f, 0f), widthMm = 4f, heightMm = 4f)
        assertDepth(0f, a.penetrationDepth(b), "x=2 で接触するだけのはず")
    }

    @Test
    fun `離れた OBB の深さは null`() {
        val a = LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f)
        val b = LabelBox(PointXY(5f, 5f), widthMm = 2f, heightMm = 2f)
        assertTrue(a.penetrationDepth(b) == null, "分離していれば null のはず")
    }

    @Test
    fun `内包された OBB の深さは抜け出すのに必要な移動量になる`() {
        val outer = LabelBox(PointXY(0f, 0f), widthMm = 10f, heightMm = 10f)
        val inner = LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f)
        // 重なり区間長 (2) ではなく分離に必要な移動量 (5+1=6) で測る
        assertDepth(6f, outer.penetrationDepth(inner), "中央の内包は半幅の和 6 のはず")
    }

    @Test
    fun `OBB の辺上を走る線分の深さは 0 (寄り添い)`() {
        // 寸法値が自分の辺に接して置かれる正常配置の幾何そのもの
        val box = LabelBox(PointXY(0f, 0f), widthMm = 4f, heightMm = 2f)
        assertDepth(0f, box.penetrationDepthSegment(PointXY(-3f, 1f), PointXY(3f, 1f)), "上辺 y=1 を走る線分は接触 0 のはず")
    }

    @Test
    fun `OBB を貫く線分の深さは押し出しに必要な移動量になる`() {
        val box = LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f)
        // 縦線 x=0 が中央を貫く → 左右どちらかへ 1 動かせば抜ける
        assertDepth(1f, box.penetrationDepthSegment(PointXY(0f, -3f), PointXY(0f, 3f)), "中央貫通は 1 のはず")
    }

    @Test
    fun `円が OBB に食い込む深さは半径とクランプ距離の差になる`() {
        val box = LabelBox(PointXY(0f, 0f), widthMm = 4f, heightMm = 4f)
        assertDepth(0.5f, box.penetrationDepthCircle(PointXY(3f, 0f), radiusMm = 1.5f), "右辺 x=2 まで距離 1、r=1.5 → 0.5")
        assertDepth(0f, box.penetrationDepthCircle(PointXY(3.5f, 0f), radiusMm = 1.5f), "距離 1.5 = r は接触 0")
        assertTrue(box.penetrationDepthCircle(PointXY(4f, 0f), radiusMm = 1.5f) == null, "距離 2 > r は null のはず")
    }

    @Test
    fun `回転した OBB でも円の深さを閉形式で返す`() {
        // 4x2 を 90 度回転 → 実質 2x4。真上 y=3 の円はローカルで右辺相当に距離 1
        val box = LabelBox(PointXY(0f, 0f), widthMm = 4f, heightMm = 2f, rotationDeg = 90f)
        assertDepth(0.5f, box.penetrationDepthCircle(PointXY(0f, 3f), radiusMm = 1.5f), "回転後の上端 y=2 まで距離 1、r=1.5 → 0.5")
    }
}
