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
    fun `モデルガード_重複する三角形はWARNINGとして検出されるが測量操作を妨害しない`() {
        val list = TriangleList(Triangle(10f, 10f, 10f))

        // 重複・衝突する位置の三角形
        val collidingTri = Triangle(10f, 10f, 10f, PointXY(-5.0, 2.0), 0f)

        // canAdd で OVERLAP が WARNING として報告される
        val issues = list.canAdd(collidingTri, checkOverlap = true)
        val overlapIssue = issues.find { it.type == GeometryIntegrityValidator.IssueType.OVERLAP }
        assertTrue(overlapIssue != null, "canAdd で衝突が報告されること")
        assertEquals(GeometryIntegrityValidator.Severity.WARNING, overlapIssue.severity, "衝突は現場操作を阻害しないよう WARNING であること")

        // 重なりがあっても致命的破壊ではないため、addGuarded は追加をブロックしない（正常な測量操作を維持）
        val added = list.addGuarded(collidingTri)
        assertTrue(added, "土木測量の現場で重なる図形が入力不能にならないよう、WARNINGでは追加拒絶されないこと")
        assertEquals(2, list.size(), "三角形が追加されていること")
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

        // 重なる三角形を追加
        val collidingTri = Triangle(10f, 10f, 10f, PointXY(-5.0, 2.0), 0f)
        list.add(collidingTri, numbering = true, guard = false)

        val allIssues = list.findIntegrityIssues()
        assertTrue(allIssues.isNotEmpty(), "リストの全体走査で異常が検出されること")
        assertTrue(list.hasOverlaps(), "hasOverlaps が true を返すこと")
    }

    // ==========================================
    // 4. 二重断面・フロート接続・実測データ検証
    // ==========================================

    @Test
    fun `二重断面接続_BL接続コード4が不正な親と誤検知されない`() {
        val parent = Triangle(3.72f, 4.2f, 2.05f)
        parent.mynumber = 21

        val child = Triangle(6.3f, 3.4f, 5.57f)
        child.mynumber = 22
        child.parentnumber = 21
        child.connectionSide = 4 // BL (二重断面・左接続)
        child.connectionType_ = 2

        val issues = GeometryIntegrityValidator.validateBaselineConnection(child, parent)
        val errors = issues.filter { it.severity == GeometryIntegrityValidator.Severity.ERROR }
        assertTrue(errors.isEmpty(), "二重断面(BL)接続はERRORと判定されてはならない: $errors")
    }

    @Test
    fun `フロート接続_FB接続コード9が基線エラーと誤検知されない`() {
        val parent = Triangle(5.0f, 4.0f, 3.0f)
        parent.mynumber = 1

        val child = Triangle(5.0f, 4.0f, 3.0f)
        child.mynumber = 2
        child.parentnumber = 1
        child.connectionSide = 9 // FB (フロートB接続)
        child.connectionType_ = 1

        val issues = GeometryIntegrityValidator.validateBaselineConnection(child, parent)
        val errors = issues.filter { it.severity == GeometryIntegrityValidator.Severity.ERROR }
        assertTrue(errors.isEmpty(), "フロート(FB)接続はERRORと判定されてはならない: $errors")
    }

    @Test
    fun `実測データ検証_4_11実測データ30個で幾何破壊エラーが0件であること`() {
        val csv = """
            koujiname,
            rosenname, 新規路線
            gyousyaname,
            zumennum,
            1,5.45,7.0,4.08,-1,-1,,-0.903588,-1.7926679,true,4,0,0,0,3,3,1,0,0,0,false,false,-436.37372,0.0,0.0,-616.3735
            2,7.0,4.5,4.85,1,1,,-1.9767483,-3.84403,false,4,0,0,0,3,3,3,1,0,2,false,false,-400.79504,-4.015406,-0.7231248,-616.3735
            3,4.5,4.75,1.84,2,1,,-1.6381136,-6.0474815,false,4,0,0,0,3,1,3,1,0,2,true,true,-357.33325,-3.2111588,-5.505977,-616.3735
            4,4.75,6.35,2.95,3,1,,-1.4560454,-7.6337333,false,4,0,0,0,3,1,3,1,0,2,false,true,-334.593,-3.0066214,-7.3345747,-616.3735
            5,2.95,1.92,2.2,4,2,,-3.295402,-8.922676,false,4,0,0,0,3,1,1,2,0,2,true,true,-443.44916,-3.0066214,-7.3345747,-616.3735
            6,1.84,0.9,1.4,3,2,,-5.3497977,-8.692822,true,4,0,0,4,3,1,1,2,0,2,true,true,-443.61783,-3.2111588,-5.505977,-616.3735
            7,1.4,1.93,3.1,6,2,,-6.3100824,-9.292602,true,4,0,4,0,3,1,1,2,0,2,false,true,-471.93234,-3.2111588,-5.505977,-616.3735
            8,3.1,2.0,2.87,7,2,,-4.9179835,-6.2762737,false,4,0,2,0,3,1,1,2,0,2,true,true,-497.2684,-3.2111588,-5.505977,-616.3735
            9,4.85,6.28,4.0,2,2,No.4,-5.058988,-2.5467882,false,4,0,0,0,3,3,1,2,0,2,false,false,-440.45502,-4.015406,-0.7231248,-616.3735
            10,6.28,2.87,4.72,9,1,,-5.7430725,-4.213817,false,4,0,0,0,3,3,3,1,0,2,true,false,-400.89093,-7.95858,-1.3949691,-616.3735
            11,4.72,1.0,4.3,10,2,,-8.750397,-7.279096,true,4,0,0,0,3,1,1,2,0,2,false,true,-426.45425,-7.95858,-1.3949691,-616.3735
            12,4.3,16.35,16.29,11,2,,-12.797563,-4.557225,false,4,0,0,0,3,1,3,2,0,2,false,false,-438.01572,-7.95858,-1.3949691,-616.3735
            13,16.29,4.06,15.8,12,2,,-18.34789,-3.5586967,false,4,0,0,0,3,3,1,2,0,2,false,false,-521.23865,-7.95858,-1.3949691,-616.3735
            14,4.06,9.96,9.0,13,1,No.3,-26.593878,-4.138953,false,4,0,0,0,3,3,1,1,0,2,false,false,-445.33148,-23.713495,-2.5877433,-616.3735
            15,9.96,9.0,4.3,14,1,,-29.471252,-5.744524,false,4,0,0,0,3,1,3,1,0,2,false,false,-380.72754,-32.698364,-3.109207,-616.3735
            16,4.3,11.98,11.0,15,2,,-36.24145,-4.7171445,false,4,0,0,0,3,3,1,2,0,2,false,false,-445.364,-32.698364,-3.109207,-616.3735
            17,11.98,11.05,4.4,16,1,,-39.80306,-6.324594,false,4,0,0,0,3,1,3,1,0,2,false,false,-378.82037,-43.690304,-3.5303898,-616.3735
            18,4.4,12.74,12.0,17,2,No.2,-47.571564,-5.3589334,false,4,0,0,0,3,3,1,2,0,2,false,false,-446.06494,-43.690304,-3.5303898,-616.3735
            19,12.74,19.85,8.85,18,1,,-54.07408,-7.2189717,false,4,0,0,0,3,1,3,1,0,2,false,false,-375.6972,-55.653202,-4.4732037,-616.3735
            20,8.85,3.72,8.05,19,2,,-60.8289,-6.3849874,false,4,0,0,0,3,3,1,2,0,2,false,false,-508.55945,-55.653202,-4.4732037,-616.3735
            21,3.72,4.2,2.05,20,1,No.1,-64.17012,-6.7960777,false,4,0,1,2,3,3,1,1,0,2,false,false,-443.11047,-63.650112,-5.396361,-616.3735
            22,6.3,3.4,5.57,21,4,,-65.55537,-7.619203,false,4,0,0,0,3,1,3,1,1,0,false,false,-413.90613,-66.91525,-3.9987817,-616.3735
            23,5.57,6.63,4.0,22,2,,-68.1161,-6.098779,false,4,0,0,0,3,3,1,2,0,2,false,false,-446.46082,-66.91525,-3.9987817,-616.3735
            24,6.63,2.0,5.95,23,1,,-68.65076,-8.09438,false,4,0,0,0,3,1,3,1,0,2,false,false,-409.4557,-70.88118,-4.519998,-616.3735
            25,5.95,1.0,6.18,24,2,,-69.58458,-8.413789,false,4,0,0,4,3,1,3,2,0,2,false,false,-426.67828,-70.88118,-4.519998,-616.3735
            26,6.18,8.0,10.93,25,2,,-72.29875,-9.685342,false,4,0,0,0,3,1,3,2,0,2,false,false,-435.88354,-70.88118,-4.519998,-616.3735
            27,10.93,8.5,4.0,26,2,,-74.094955,-8.018917,false,4,0,0,0,3,3,1,2,0,2,false,false,-481.99396,-70.88118,-4.519998,-616.3735
            28,8.5,7.16,5.0,27,1,,-76.95477,-8.914108,false,4,0,0,0,3,3,1,1,0,2,false,false,-463.02057,-74.75713,-5.5083323,-616.3735
            29,7.16,4.68,8.9,28,1,,-78.96524,-12.411993,false,4,0,0,0,3,1,3,1,0,2,false,false,-427.05426,-79.46357,-7.196351,-616.3735
            30,8.9,7.14,5.4,29,2,,-81.48609,-11.062627,false,4,0,0,0,3,1,1,2,0,2,false,false,-458.63513,-79.46357,-7.196351,-616.3735
        """.trimIndent()

        val doc = com.jpaver.trianglelist.datamanager.CsvCodec.parse(csv)
        val list = com.jpaver.trianglelist.datamanager.CsvCodec.build(doc)
        assertEquals(30, list.size(), "実測CSVから30個の三角形が復元されること")

        val issues = GeometryIntegrityValidator.validateList(list)
        val errors = issues.filter { it.severity == GeometryIntegrityValidator.Severity.ERROR }
        assertTrue(errors.isEmpty(), "実測4.11データから幾何エラーが一切出ないこと（偽陽性ゼロ）: $errors")
    }
}
