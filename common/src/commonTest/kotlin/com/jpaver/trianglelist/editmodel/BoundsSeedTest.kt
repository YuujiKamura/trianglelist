package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import com.jpaver.trianglelist.setBoundaryBox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * bbox の計算が原点 (0,0) を巻き込まないことを pin する。
 *
 * 2026-08-25 発見: TriangleList.calcBounds() が `Bounds(0.0, 0.0, 0.0, 0.0)` を種にして
 * expandBoundaries を回していたため、**bbox が常に原点を含んで**しまっていた。図形が
 * 原点から離れた位置にあるほど中心がずれる。
 *
 * 実害 (この 1 バグが 3 つの failing test の共通原因だった):
 *  - TriangleRotationTest.testCenteringLogic … center 分だけ move しても中心が 0 にならない
 *    (三角形を (100,100) に置くと bbox が 0..105 になり中心 52.5、移動後も 23.75 残る)
 *  - WebFrameTest.frame_outer_rect_centers … 図面枠の中心が図形中心と 3.0 ずれる
 *  - web-js fix-verification.test.ts … 回転時に図形中心が画面上で動く
 *
 * 種を原点にすると「原点も図形の一部」と宣言したのと同じで、図形が原点から遠いほど
 * 中心が原点側へ引っ張られる。正しくは最初の頂点を種にする (= 図形の実 bbox)。
 */
class BoundsSeedTest {

    private val tol = 1e-3

    @Test
    fun `原点から離れた図形の中心が図形自身の中心になる`() {
        val trilist = TriangleList()
        trilist.add(Triangle(10f, 10f, 10f, PointXY(100f, 100f), 180f), true)

        val c = trilist.center
        // 一辺 10 の正三角形なので、中心は基準点から高々 10 程度しか離れない。
        // 原点を巻き込んでいると 52.5 付近になる。
        assertTrue(
            kotlin.math.abs(c.x - 100f) < 10f && kotlin.math.abs(c.y - 100f) < 10f,
            "図形中心が図形から離れすぎている (${c.x}, ${c.y}) ── bbox が原点を巻き込んでいる"
        )
    }

    @Test
    fun `中心ぶん移動すると中心が原点に来る`() {
        val trilist = TriangleList()
        trilist.add(Triangle(10f, 10f, 10f, PointXY(100f, 100f), 180f), true)

        val gc = trilist.center
        trilist.move(PointXY(-gc.x, -gc.y))

        val newC = trilist.center
        assertEquals(0.0, newC.x.toDouble(), tol, "移動後の中心 X が 0 でない: ${newC.x}")
        assertEquals(0.0, newC.y.toDouble(), tol, "移動後の中心 Y が 0 でない: ${newC.y}")
    }

    @Test
    fun `centering は冪等 (二度やっても動かない)`() {
        // 種が原点だと 1 回目で中心が半分に縮むだけで、繰り返すたびに 0 へ漸近する
        // (= 冪等でない)。正しく実 bbox を取れていれば 2 回目は何も動かない。
        val trilist = TriangleList()
        trilist.add(Triangle(10f, 10f, 10f, PointXY(100f, 100f), 180f), true)

        val gc = trilist.center
        trilist.move(PointXY(-gc.x, -gc.y))
        val after1 = trilist.center
        val gc2 = trilist.center
        trilist.move(PointXY(-gc2.x, -gc2.y))
        val after2 = trilist.center

        assertEquals(after1.x.toDouble(), after2.x.toDouble(), tol, "2 回目の centering で動いた")
        assertEquals(after1.y.toDouble(), after2.y.toDouble(), tol, "2 回目の centering で動いた")
    }

    @Test
    fun `三角形の bbox が 3 頂点すべてを含む`() {
        // 2026-08-25 発見: setBoundaryBox() が pointAB と pointBC の 2 頂点しか見ておらず、
        // 第 3 頂点 (point[0] = pointCA) が bbox から抜けていた。三角形は 3 頂点なので
        // 2 点の min/max では当然足りない ── 第 3 頂点がその箱の外にある形では必ずずれる。
        //
        // 影響: TriangleList.center (= calcBounds 経由) と CycleShape.vertices() ベースの
        // bbox (WebFrame.figuresBboxCenter) が食い違い、図面枠の中心が図形中心から 3.0
        // ずれていた (WebFrameTest.frame_outer_rect_centers)。
        val t = Triangle(6f, 5f, 4f, PointXY(0f, 0f), 180f)
        t.setBoundaryBox()
        val vs = t.vertices()
        val left = vs.minOf { it.x }.toDouble()
        val right = vs.maxOf { it.x }.toDouble()
        val bottom = vs.minOf { it.y }.toDouble()
        val top = vs.maxOf { it.y }.toDouble()
        assertEquals(left, t.myBP_.left, tol, "bbox left が頂点を含まない (第 3 頂点が抜けている)")
        assertEquals(right, t.myBP_.right, tol, "bbox right が頂点を含まない")
        assertEquals(bottom, t.myBP_.bottom, tol, "bbox bottom が頂点を含まない")
        assertEquals(top, t.myBP_.top, tol, "bbox top が頂点を含まない")
    }

    @Test
    fun `TriangleList の bbox が頂点ベースの bbox と一致する`() {
        // calcBounds (setBoundaryBox 経由) と vertices() ベースが同じ答えを出すこと。
        // 図面枠の中心決めは後者、図形の centering は前者を使うので、食い違うと枠がずれる。
        val trilist = TriangleList()
        trilist.add(Triangle(6f, 5f, 4f, PointXY(3f, 7f), 180f), true)
        val b = trilist.calcBounds()
        val vs = trilist.get(1).vertices()
        assertEquals(vs.minOf { it.x }.toDouble(), b.left, tol)
        assertEquals(vs.maxOf { it.x }.toDouble(), b.right, tol)
        assertEquals(vs.minOf { it.y }.toDouble(), b.bottom, tol)
        assertEquals(vs.maxOf { it.y }.toDouble(), b.top, tol)
    }

    @Test
    fun `空リストの bbox はゼロ`() {
        // 種を最初の頂点にする変更で、要素ゼロのときの挙動が変わらないことを押さえる
        val trilist = TriangleList()
        val b = trilist.calcBounds()
        assertEquals(0.0, b.left, tol)
        assertEquals(0.0, b.right, tol)
        assertEquals(0.0, b.top, tol)
        assertEquals(0.0, b.bottom, tol)
    }
}
