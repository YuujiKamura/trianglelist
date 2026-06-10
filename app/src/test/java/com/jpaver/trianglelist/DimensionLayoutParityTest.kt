package com.jpaver.trianglelist

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.DimOnPath
import com.jpaver.trianglelist.label.DimensionLayout
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ADR 0003 Phase 1 の同値性証明 (golden parity)。
 * 既存 DimOnPath (app/editmodel) と新 DimensionLayout (common/label) を同じ入力で呼び、
 * gapPaperMm=0 のとき全出力フィールドが数値同値であることを全分岐で照合する。
 */
class DimensionLayoutParityTest {

    private val eps = 1e-4f

    // 反転 {pointA.x < pointB.x, >=} の両側 + 垂直辺 (x 同値で >= が成立) + 斜め + 小数を覆う
    private val edges = listOf(
        PointXY(0f, 0f) to PointXY(10f, 0f),          // 水平、A.x < B.x
        PointXY(10f, 5f) to PointXY(0f, 3f),          // A.x > B.x (反転)
        PointXY(3f, 0f) to PointXY(3f, 8f),           // 垂直辺、A.x == B.x (>= 成立)
        PointXY(3f, 8f) to PointXY(3f, 0f),           // 垂直辺の逆向き
        PointXY(1f, 2f) to PointXY(7f, 9f),           // 斜め右上
        PointXY(8f, 1f) to PointXY(2f, 6f),           // 斜め反転
        PointXY(0.5f, 1.25f) to PointXY(4.75f, 0.3f)  // 小数
    )
    private val scales = listOf(1f, 0.5f, 2.5f)
    private val dimheights = listOf(0.05f, 0.3f)

    private fun assertParity(
        leftP: PointXY,
        rightP: PointXY,
        vertical: Int,
        horizontal: Int,
        scale: Float,
        dimheight: Float
    ) {
        val old = DimOnPath(scale, PointXY(leftP), PointXY(rightP), vertical, horizontal, dimheight)
        val new = DimensionLayout.layout(PointXY(leftP), PointXY(rightP), vertical, horizontal, scale, dimheight)
        val label = "[L=$leftP R=$rightP v=$vertical h=$horizontal s=$scale dh=$dimheight]"

        assertEquals("dimpoint.x $label", old.dimpoint.x, new.dimpoint.x, eps)
        assertEquals("dimpoint.y $label", old.dimpoint.y, new.dimpoint.y, eps)
        assertEquals("offsetH $label", old.offsetH, new.offsetH, eps)
        assertEquals("offsetV $label", old.offsetV, new.offsetV, eps)
        assertEquals("verticalDxf $label", old.verticalDxf(), new.verticalDxf)
        assertEquals("pointA.x $label", old.pointA.x, new.pointA.x, eps)
        assertEquals("pointA.y $label", old.pointA.y, new.pointA.y, eps)
        assertEquals("pointB.x $label", old.pointB.x, new.pointB.x, eps)
        assertEquals("pointB.y $label", old.pointB.y, new.pointB.y, eps)
        assertEquals("clockwise $label", old.clockwise, new.clockwise)
    }

    private fun assertParityAllEdges(vertical: Int, horizontal: Int) {
        for ((leftP, rightP) in edges) {
            for (scale in scales) {
                for (dimheight in dimheights) {
                    assertParity(leftP, rightP, vertical, horizontal, scale, dimheight)
                }
            }
        }
    }

    @Test
    fun `horizontalCENTERは全座標と全verticalで同値`() {
        assertParityAllEdges(vertical = 1, horizontal = 0)
        assertParityAllEdges(vertical = 3, horizontal = 0)
    }

    @Test
    fun `horizontalINRIGHTは全座標と全verticalで同値`() {
        assertParityAllEdges(vertical = 1, horizontal = 1)
        assertParityAllEdges(vertical = 3, horizontal = 1)
    }

    @Test
    fun `horizontalINLEFTは全座標と全verticalで同値`() {
        assertParityAllEdges(vertical = 1, horizontal = 2)
        assertParityAllEdges(vertical = 3, horizontal = 2)
    }

    @Test
    fun `horizontalOUTERRIGHTの旗揚げは全座標と全verticalで同値`() {
        assertParityAllEdges(vertical = 1, horizontal = 3)
        assertParityAllEdges(vertical = 3, horizontal = 3)
    }

    @Test
    fun `horizontalOUTERLEFTの旗揚げは全座標と全verticalで同値`() {
        assertParityAllEdges(vertical = 1, horizontal = 4)
        assertParityAllEdges(vertical = 3, horizontal = 4)
    }

    @Test
    fun `測点名SIDE_SOKUTENは全座標と両SIDEで同値`() {
        assertParityAllEdges(vertical = 4, horizontal = 0)
        assertParityAllEdges(vertical = 4, horizontal = 1)
    }

    @Test
    fun `三角形の実生成と同じ点順序でも同値`() {
        // TriangleDimExtensions.setDimPath と同じ呼び方: A=(pointAB,point[0]), B=(pointBC,pointAB), C=(point[0],pointBC)
        val tri = com.jpaver.trianglelist.editmodel.Triangle(5f, 4f, 3f, PointXY(0f, 0f), 180f)
        val sides = listOf(
            tri.pointAB to tri.point[0],
            tri.pointBC to tri.pointAB,
            tri.point[0] to tri.pointBC
        )
        val verticals = listOf(tri.dim.vertical.a, tri.dim.vertical.b, tri.dim.vertical.c)
        val horizontals = listOf(tri.dim.horizontal.a, tri.dim.horizontal.b, tri.dim.horizontal.c)
        for (i in 0..2) {
            assertParity(sides[i].first, sides[i].second, verticals[i], horizontals[i], tri.scaleFactor, tri.dimHeight)
        }
    }
}
