package com.jpaver.trianglelist.regression

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleCollisionDetector
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.calcPoints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 当たり判定（衝突検出）、基線一致判定、モデル層ガード機構の徹底検証テスト。
 */
class GeometryIntegrityTest {

    // ==========================================
    // 1. 当たり判定（幾何衝突検出）の単体テスト
    // ==========================================

    @Test
    fun `線分交差判定_正常に交差する2線分を検出できる`() {
        val p1 = PointXY(0.0, 0.0)
        val p2 = PointXY(10.0, 10.0)
        val p3 = PointXY(0.0, 10.0)
        val p4 = PointXY(10.0, 0.0)

        assertTrue(
            TriangleCollisionDetector.doSegmentsCross(p1, p2, p3, p4),
            "X字に交差する2線分は真に交差すると判定されなければならない"
        )
    }

    @Test
    fun `線分交差判定_端点を共有する線分は交差とみなさない`() {
        val p1 = PointXY(0.0, 0.0)
        val p2 = PointXY(10.0, 0.0)
        val p3 = PointXY(10.0, 0.0) // p2 と同一点
        val p4 = PointXY(10.0, 10.0)

        assertFalse(
            TriangleCollisionDetector.doSegmentsCross(p1, p2, p3, p4),
            "頂点を共有して折れ曲がる線分同士は内部交差ではない"
        )
    }

    @Test
    fun `線分交差判定_平行で離れた線分は交差しない`() {
        val p1 = PointXY(0.0, 0.0)
        val p2 = PointXY(10.0, 0.0)
        val p3 = PointXY(0.0, 5.0)
        val p4 = PointXY(10.0, 5.0)

        assertFalse(TriangleCollisionDetector.doSegmentsCross(p1, p2, p3, p4))
    }

    @Test
    fun `点の内包判定_三角形内部の点を正確に判定できる`() {
        val a = PointXY(0.0, 0.0)
        val b = PointXY(10.0, 0.0)
        val c = PointXY(5.0, 8.0)

        val inside = PointXY(5.0, 3.0)
        val outside = PointXY(5.0, 12.0)
        val onBoundary = PointXY(5.0, 0.0)

        assertTrue(TriangleCollisionDetector.isPointStrictlyInside(inside, a, b, c))
        assertFalse(TriangleCollisionDetector.isPointStrictlyInside(outside, a, b, c))
        assertFalse(TriangleCollisionDetector.isPointStrictlyInside(onBoundary, a, b, c))
    }

    @Test
    fun `三角形衝突_完全に離れた2つの三角形は重複しない`() {
        val t1 = Triangle(5f, 4f, 3f)
        val t2 = Triangle(5f, 4f, 3f)
        t2.calcPoints(PointXY(50.0, 50.0), 0f)

        assertFalse(TriangleCollisionDetector.doTrianglesOverlap(t1, t2))
    }

    @Test
    fun `三角形衝突_辺がクロスして重なる2つの三角形の衝突を検出できる`() {
        val t1 = Triangle(10f, 10f, 10f) // 頂点: (0,0), (-10,0), (-5, 8.66)
        // t2 を t1 と交差する位置へ配置
        val t2 = Triangle(10f, 10f, 10f, PointXY(-5.0, 2.0), 0f)

        assertTrue(
            TriangleCollisionDetector.doTrianglesOverlap(t1, t2),
            "内部を貫通する三角形同士の衝突を検出できること"
        )
    }

    @Test
    fun `三角形衝突_内向きに折り返した親子三角形の衝突を検出できる`() {
        val parent = Triangle(6f, 5f, 4f)

        // 親のB辺に接続するが、わざと親の重心（内部）に倒れた頂点を持つ子供を作成
        val child = Triangle(parent, 1, 4f, 4f)
        child.pointBC = parent.centroid()

        assertTrue(
            TriangleCollisionDetector.isChildFoldingInward(child, parent),
            "親の内部へ向いた折り返し三角形を検出できること"
        )
        assertTrue(
            TriangleCollisionDetector.doTrianglesOverlap(parent, child),
            "内向き折り返しは重複として検出されること"
        )
    }

    // ==========================================
    // 2. 基線一致判定 (Baseline Integrity) のテスト
    // ==========================================

    @Test
    fun `基線一致判定_正常接続された親子三角形は合格する`() {
        val parent = Triangle(6f, 5f, 4f)
        parent.mynumber = 1
        val list = TriangleList(parent)

        // B辺長は 5.0f
        val child = Triangle(parent, 1, 4f, 3f)
        child.mynumber = 2
        list.add(child)

        val issues = GeometryIntegrityValidator.validateBaselineConnection(child, parent)
        assertTrue(issues.isEmpty(), "正常な基線接続でエラーが出てはならない: $issues")
    }

    @Test
    fun `基線一致判定_辺長不一致を即座に検出してエラーとする`() {
        val parent = Triangle(6f, 5f, 4f)
        parent.mynumber = 1

        // 親のB辺長は 5.0f なのに、A辺長 9.9f で接続を詐称
        val child = Triangle(9.9f, 4f, 3f)
        child.mynumber = 2
        child.parentnumber = 1
        child.connectionSide = 1
        child.connectionType_ = 0

        val issues = GeometryIntegrityValidator.validateBaselineConnection(child, parent)
        assertTrue(
            issues.any { it.type == GeometryIntegrityValidator.IssueType.BASELINE_LENGTH_MISMATCH },
            "基線長の不一致が検出されること: $issues"
        )
    }

    @Test
    fun `基線一致判定_親から離脱した座標ギャップを検出できる`() {
        val parent = Triangle(6f, 5f, 4f)
        parent.mynumber = 1

        // A辺長は 5.0f で合わせるが、座標が遠くに離れている
        val child = Triangle(5f, 4f, 3f)
        child.mynumber = 2
        child.parentnumber = 1
        child.connectionSide = 1
        child.connectionType_ = 0
        child.calcPoints(PointXY(100.0, 100.0), 0f)

        val issues = GeometryIntegrityValidator.validateBaselineConnection(child, parent)
        assertTrue(
            issues.any { it.type == GeometryIntegrityValidator.IssueType.BASELINE_GAP },
            "親の辺から離れた座標ギャップが検出されること: $issues"
        )
    }

    // ==========================================
    // 3. モデル層ガード機構 (Model Guard) のテスト
    // ==========================================

    @Test
    fun `モデルガード_重複する三角形の追加をaddGuardedが拒絶する`() {
        val list = TriangleList(Triangle(10f, 10f, 10f))

        // 重複・衝突する位置の三角形
        val collidingTri = Triangle(10f, 10f, 10f, PointXY(-5.0, 2.0), 0f)

        // canAdd でエラーが返る
        val issues = list.canAdd(collidingTri, checkOverlap = true)
        assertTrue(
            issues.any { it.type == GeometryIntegrityValidator.IssueType.OVERLAP },
            "canAdd で衝突が報告されること: $issues"
        )

        // addGuarded は false を返し、リストのサイズは増えない
        val added = list.addGuarded(collidingTri)
        assertFalse(added, "衝突する三角形の追加は拒絶されなければならない")
        assertEquals(1, list.size(), "拒絶されたためリストサイズは1のままであること")
    }

    @Test
    fun `モデルガード_基線不一致の三角形の追加をaddGuardedが拒絶する`() {
        val list = TriangleList(Triangle(6f, 5f, 4f))

        // 親のB辺は 5.0f だが、A辺 8.0f を指定
        val brokenTri = Triangle(8f, 5f, 5f)
        brokenTri.parentnumber = 1
        brokenTri.connectionSide = 1
        brokenTri.connectionType_ = 0

        val added = list.addGuarded(brokenTri)
        assertFalse(added, "基線不一致の追加は拒絶されなければならない")
        assertEquals(1, list.size())
    }

    @Test
    fun `モデルガード_正常な三角形はaddGuardedで安全に追加できる`() {
        val list = TriangleList(Triangle(6f, 5f, 4f))
        val child = Triangle(list.getBy(1), 1, 4f, 3f)

        val added = list.addGuarded(child)
        assertTrue(added, "正常な三角形は追加に成功すること")
        assertEquals(2, list.size())
        assertFalse(list.hasOverlaps(), "正常追加後は重複が存在しないこと")
    }

    @Test
    fun `図形破壊検出_リスト全体走査で衝突と基線不整合を一網打尽に検出できる`() {
        val list = TriangleList(Triangle(10f, 10f, 10f))

        // ガードなしで強引に重なる三角形を追加
        val collidingTri = Triangle(10f, 10f, 10f, PointXY(-5.0, 2.0), 0f)
        list.add(collidingTri, numbering = true, guard = false)

        val allIssues = list.findIntegrityIssues()
        assertTrue(allIssues.isNotEmpty(), "リストの全体走査で異常が検出されること")
        assertTrue(list.hasOverlaps(), "hasOverlaps が true を返すこと")
    }
}
