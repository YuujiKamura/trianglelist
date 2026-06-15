package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import com.jpaver.trianglelist.Bounds
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B14-pre: Rectangle.expandBoundaries / rotateBy の cartesian pin テスト。
 *
 * Y 規約 (Triangle.expandBoundaries と同一):
 *   left=minX, right=maxX, top=maxY, bottom=minY
 *   union: left=min, top=max, right=max, bottom=min
 */
class RectangleGeometryTest {

    // ---- ヘルパ ----

    private fun near(expected: Double, actual: Double, tol: Double = 1e-4, msg: String = "") {
        assertTrue(abs(expected - actual) <= tol, "$msg: expected=$expected actual=$actual diff=${abs(expected-actual)}")
    }

    /** bounds が頂点 v を全て包含していることを assert */
    private fun assertContainsVertex(bounds: Bounds, v: PointXY, caseLabel: String) {
        val x = v.x.toDouble()
        val y = v.y.toDouble()
        assertTrue(x >= bounds.left - 1e-4,   "$caseLabel vertex.x=$x < bounds.left=${bounds.left}")
        assertTrue(x <= bounds.right + 1e-4,  "$caseLabel vertex.x=$x > bounds.right=${bounds.right}")
        assertTrue(y >= bounds.bottom - 1e-4, "$caseLabel vertex.y=$y < bounds.bottom=${bounds.bottom}")
        assertTrue(y <= bounds.top + 1e-4,    "$caseLabel vertex.y=$y > bounds.top=${bounds.top}")
    }

    /** bounds が listBound を union していること (listBound の全コーナーが返り値に包含) を assert */
    private fun assertContainsListBound(result: Bounds, listBound: Bounds, caseLabel: String) {
        assertTrue(result.left   <= listBound.left   + 1e-4, "$caseLabel result.left=${result.left} > listBound.left=${listBound.left}")
        assertTrue(result.right  >= listBound.right  - 1e-4, "$caseLabel result.right=${result.right} < listBound.right=${listBound.right}")
        assertTrue(result.top    >= listBound.top    - 1e-4, "$caseLabel result.top=${result.top} < listBound.top=${listBound.top}")
        assertTrue(result.bottom <= listBound.bottom + 1e-4, "$caseLabel result.bottom=${result.bottom} > listBound.bottom=${listBound.bottom}")
    }

    // ---- expandBoundaries テスト (216 ケース) ----

    /**
     * 接続状態 x angle x alignment x listBound x widthPair の cartesian product (3x4x3x2x3=216 ケース)。
     * 各ケースで:
     *  1. expandBoundaries 返り値が 4 頂点を全て包含する
     *  2. expandBoundaries 返り値が listBound も包含する (union の性質)
     */
    @Test
    fun expandBoundaries_contains_all_vertices_and_listBound() {
        // 接続状態: (nodeA, side) のペア。null=独立、非null=親接続あり
        val parentTriangle = Triangle(6f, 5f, 4f)  // 親として使う Triangle

        data class ConnectionCase(val label: String, val nodeA: EditObject?, val side: Int)
        val connections = listOf(
            ConnectionCase("独立", null, 1),
            ConnectionCase("親=Triangle side=1", parentTriangle, 1),
            ConnectionCase("親=Triangle side=2", parentTriangle, 2),
        )

        val angles = listOf(0.0, 45.0, 90.0, 180.0)
        val alignments = listOf(0, 1, 2)
        val listBounds = listOf(
            Bounds(0.0, 0.0, 0.0, 0.0),
            Bounds(-5.0, 10.0, 15.0, -3.0),
        )
        data class WidthPair(val widthA: Double, val widthB: Double, val label: String)
        val widthPairs = listOf(
            WidthPair(10.0, 4.0, "通常"),
            WidthPair(6.0, 6.0, "長方形"),
            WidthPair(4.0, 10.0, "逆台形"),
        )

        var count = 0
        for (conn in connections) {
            for (angle in angles) {
                for (alignment in alignments) {
                    for (lb in listBounds) {
                        for (wp in widthPairs) {
                            count++
                            val label = "case#$count conn=${conn.label} angle=$angle align=$alignment " +
                                "lb=(${lb.left},${lb.top},${lb.right},${lb.bottom}) widths=(${wp.widthA},${wp.widthB})"

                            val rect = Rectangle(
                                length = 5.0,
                                widthA = wp.widthA,
                                widthB = wp.widthB,
                                angle = angle,
                                basepoint = PointXY(0f, 0f),
                                nodeA = conn.nodeA,
                                side = conn.side,
                                alignment = alignment,
                            )

                            val result = rect.expandBoundaries(lb)

                            // 1. 4 頂点が全て result bounds に包含される
                            val verts = rect.vertices()
                            for (v in verts) {
                                assertContainsVertex(result, v, label)
                            }

                            // 2. listBound も result に包含される (union)
                            assertContainsListBound(result, lb, label)
                        }
                    }
                }
            }
        }

        // ケース数確認 (3 x 4 x 3 x 2 x 3 = 216)
        assertTrue(count == 216, "expected 216 cases, got $count")
    }

    // ---- rotateBy テスト (60 ケース) ----

    /**
     * 接続状態 x center x degrees x 初期angle の cartesian product (2x3x5x2=60 ケース)。
     * 独立: basepoint が rotate(center, degrees) の期待値に一致、angle が degrees 加算される。
     * 親接続あり: no-op (basepoint/angle が変化しない)。
     */
    @Test
    fun rotateBy_cartesian() {
        val parentTriangle = Triangle(6f, 5f, 4f)

        data class ConnectionCase(val label: String, val nodeA: EditObject?, val side: Int)
        val connections = listOf(
            ConnectionCase("独立", null, 1),
            ConnectionCase("親接続あり", parentTriangle, 1),
        )

        val centers = listOf(
            PointXY(0f, 0f),
            PointXY(5f, 0f),
            PointXY(0f, 5f),
        )
        val degreesList = listOf(0f, 90f, -90f, 180f, 360f)
        val initialAngles = listOf(0.0, 45.0)

        var count = 0
        for (conn in connections) {
            for (center in centers) {
                for (degrees in degreesList) {
                    for (initAngle in initialAngles) {
                        count++
                        val label = "case#$count conn=${conn.label} center=(${center.x},${center.y}) " +
                            "degrees=$degrees initAngle=$initAngle"

                        val initBp = PointXY(2f, 3f)
                        val rect = Rectangle(
                            length = 5.0,
                            widthA = 8.0,
                            widthB = 4.0,
                            angle = initAngle,
                            basepoint = initBp,
                            nodeA = conn.nodeA,
                            side = conn.side,
                        )

                        val bpBefore = PointXY(rect.basepoint.x, rect.basepoint.y)
                        val angleBefore = rect.angle

                        rect.rotateBy(center, degrees)

                        if (conn.nodeA == null) {
                            // 独立: basepoint が回転した位置に一致
                            val expected = bpBefore.rotate(center, degrees.toDouble())
                            near(expected.x.toDouble(), rect.basepoint.x.toDouble(), 1e-4, "$label basepoint.x")
                            near(expected.y.toDouble(), rect.basepoint.y.toDouble(), 1e-4, "$label basepoint.y")
                            // angle に degrees が加算されている
                            near(angleBefore + degrees, rect.angle, 1e-4, "$label angle")
                        } else {
                            // 親接続あり: no-op
                            near(bpBefore.x.toDouble(), rect.basepoint.x.toDouble(), 1e-4, "$label noop basepoint.x")
                            near(bpBefore.y.toDouble(), rect.basepoint.y.toDouble(), 1e-4, "$label noop basepoint.y")
                            near(angleBefore, rect.angle, 1e-4, "$label noop angle")
                        }
                    }
                }
            }
        }

        // ケース数確認 (2 x 3 x 5 x 2 = 60)
        assertTrue(count == 60, "expected 60 cases, got $count")
    }

    // ---- 追加境界テスト ----

    /** 4 回 90 度回転で元に戻る (浮動小数誤差内) */
    @Test
    fun rotateBy_360_returns_to_original() {
        val center = PointXY(0f, 0f)
        val rect = Rectangle(
            length = 5.0,
            widthA = 10.0,
            widthB = 4.0,
            angle = 30.0,
            basepoint = PointXY(3f, 4f),
        )
        val origBpX = rect.basepoint.x.toDouble()
        val origBpY = rect.basepoint.y.toDouble()
        val origAngle = rect.angle

        repeat(4) { rect.rotateBy(center, 90f) }

        near(origBpX,  rect.basepoint.x.toDouble(), 1e-3, "4x90° basepoint.x")
        near(origBpY,  rect.basepoint.y.toDouble(), 1e-3, "4x90° basepoint.y")
        // angle は 360 加算されているので mod 360 で比較
        val angleDiff = (rect.angle - origAngle) % 360.0
        near(0.0, angleDiff, 1e-3, "4x90° angle mod 360")
    }

    /**
     * RectangleParentTest.base() と同じ Rectangle (length=5, widthA=10, widthB=4, align=0)
     * で expandBoundaries が正常動作する回帰テスト。
     * base() の頂点: bl=(0,0) br=(10,0) tl=(0,5) tr=(4,5)
     */
    @Test
    fun expandBoundaries_existing_RectangleParentTest_cases_green() {
        val r = Rectangle(5.0, 10.0, 4.0, alignment = 0)
        val lb = Bounds(0.0, 0.0, 0.0, 0.0)
        val result = r.expandBoundaries(lb)

        // 期待 bounds: left=0, right=10, top=5, bottom=0
        near(0.0,  result.left,   1e-4, "left")
        near(10.0, result.right,  1e-4, "right")
        near(5.0,  result.top,    1e-4, "top")
        near(0.0,  result.bottom, 1e-4, "bottom")

        // 4 頂点 (bl/br/tl/tr) が全て bounds に包含される
        val verts = r.vertices()
        for (v in verts) {
            assertContainsVertex(result, v, "RectangleParentTest base()")
        }
    }
}
