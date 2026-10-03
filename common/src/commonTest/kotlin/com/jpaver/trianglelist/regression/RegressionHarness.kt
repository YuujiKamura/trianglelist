package com.jpaver.trianglelist.regression

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 不具合再現パスをテストケースとして蓄積・実行するための回帰テストハーネス。
 *
 * 【目的】
 * 「このCSVを開いて、この順で操作したら巻き戻った / 座標が狂った / 落ちた」
 * という不具合の操作パスをコード化し、リグレッションを永久に防止する。
 */
class RegressionHarness(
    var triangleList: TriangleList = TriangleList(Triangle(6f, 5f, 4f))
) {
    /** 実行された操作ログ（Breadcrumbs） */
    private val breadcrumbs = mutableListOf<String>()

    init {
        logAction("init: initial triangle(6,5,4)")
        verifyInvariants("init")
    }

    private fun logAction(action: String) {
        breadcrumbs.add("[step=${breadcrumbs.size + 1}] $action")
    }

    fun getBreadcrumbs(): List<String> = breadcrumbs.toList()

    /** 初期 CSV からセッションを開始 */
    fun loadCsv(csvText: String): RegressionHarness {
        logAction("loadCsv (${csvText.lines().size} lines)")
        val doc = CsvCodec.parse(csvText)
        val loaded = CsvCodec.build(doc, applyRecoverState = false)
        this.triangleList = loaded
        verifyInvariants("loadCsv")
        return this
    }

    /** 三角形を直前の三角形のB辺にチェーン接続して追加 */
    fun addTriangle(a: Float, b: Float, c: Float): RegressionHarness {
        val last = triangleList.getBy(triangleList.size())
        return addConnectedTriangle(a, b, c, parentNumber = last.mynumber, connectionSide = 1)
    }

    /** 親と接続辺を指定して三角形を追加 */
    fun addConnectedTriangle(
        a: Float,
        b: Float,
        c: Float,
        parentNumber: Int,
        connectionSide: Int
    ): RegressionHarness {
        logAction("addConnectedTriangle(a=$a, b=$b, c=$c, parent=$parentNumber, side=$connectionSide)")
        val parent = triangleList.getBy(parentNumber)
        val child = Triangle(parent, connectionSide, a, b, c)
        triangleList.add(child, true)
        verifyInvariants("addConnectedTriangle")
        return this
    }

    /** 末尾の三角形を削除 */
    fun removeLast(): RegressionHarness {
        val sizeBefore = triangleList.size()
        logAction("removeLast (current size=$sizeBefore)")
        if (sizeBefore > 1) {
            triangleList.remove(sizeBefore)
        }
        verifyInvariants("removeLast")
        return this
    }

    /** 全体回転 */
    fun rotate(angle: Float, basePoint: PointXY = PointXY(0.0, 0.0)): RegressionHarness {
        logAction("rotate(angle=$angle)")
        triangleList.rotate(basePoint, angle, 0, false)
        verifyInvariants("rotate")
        return this
    }

    /**
     * 保存 ＆ 再読み込みのシミュレーション（セーブ/ロード、画面復元 onResume 相当）。
     * サイレントな巻き戻りやデータ欠落がここで検出される。
     */
    fun serializeAndReload(): RegressionHarness {
        logAction("serializeAndReload")
        val sizeBefore = triangleList.size()
        val areaBefore = calculateTotalArea()

        // 1. Bake (Model -> CsvDoc) & Serialize (CsvDoc -> text)
        val emptyDoc = CsvCodec.CsvDoc(emptyList(), null, emptyList())
        val doc = CsvCodec.bake(triangleList, emptyDoc)
        val csvText = CsvCodec.serialize(doc)

        // 2. Parse & Build (Text -> CsvDoc -> Model)
        val reloadedDoc = CsvCodec.parse(csvText)
        val reloadedModel = CsvCodec.build(reloadedDoc, applyRecoverState = false)

        this.triangleList = reloadedModel

        // 3. 不変条件の検証
        assertEquals(sizeBefore, triangleList.size(), "Reloaded size must match before reload. Log: $breadcrumbs")
        val areaAfter = calculateTotalArea()
        assertTrue(
            abs(areaBefore - areaAfter) < 0.05f,
            "Total area must be preserved across reload. Before=$areaBefore, After=$areaAfter. Log: $breadcrumbs"
        )

        verifyInvariants("serializeAndReload")
        return this
    }

    /** 合計面積の計算 */
    fun calculateTotalArea(): Float {
        var total = 0f
        for (i in 1..triangleList.size()) {
            val t = triangleList.getBy(i)
            total += t.getArea()
        }
        return total
    }

    /**
     * 【不変条件（Invariants）の厳密検証】
     * どの操作ステップの直後でも絶対に破綻してはならない法則。
     */
    fun verifyInvariants(stepContext: String) {
        val size = triangleList.size()
        assertTrue(size >= 1, "[$stepContext] TriangleList must never be empty. Breadcrumbs: $breadcrumbs")

        for (i in 1..size) {
            val t = triangleList.getBy(i)
            assertNotNull(t, "[$stepContext] Triangle at 1-based index $i must exist. Breadcrumbs: $breadcrumbs")

            // 1. 番号の連続性
            assertEquals(i, t.mynumber, "[$stepContext] Triangle mynumber must equal index $i. Breadcrumbs: $breadcrumbs")

            // 2. 親番号の妥当性 (1番は-1、2番以降は自分より若い番号)
            if (i == 1) {
                assertEquals(-1, t.parentnumber, "[$stepContext] Root triangle must have parent -1")
            } else {
                assertTrue(
                    t.parentnumber in 1 until i,
                    "[$stepContext] Triangle $i has invalid parentnumber ${t.parentnumber}. Must be in 1..${i - 1}. Breadcrumbs: $breadcrumbs"
                )
            }

            // 3. 辺長の正値性
            val a = t.lengthA_
            val b = t.lengthB_
            val c = t.lengthC_
            assertTrue(a > 0f && b > 0f && c > 0f, "[$stepContext] Sides must be positive: a=$a, b=$b, c=$c. Breadcrumbs: $breadcrumbs")

            // 4. 三角形不等式 (縮退していないこと)
            assertTrue(
                a + b > c && b + c > a && c + a > b,
                "[$stepContext] Triangle inequality broken: a=$a, b=$b, c=$c. Breadcrumbs: $breadcrumbs"
            )

            // 5. 面積の正常性 (NaN / 0 / Infinity でないこと)
            val area = t.getArea()
            assertFalse(area.isNaN(), "[$stepContext] Triangle $i area is NaN. Breadcrumbs: $breadcrumbs")
            assertFalse(area.isInfinite(), "[$stepContext] Triangle $i area is Infinite. Breadcrumbs: $breadcrumbs")
            assertTrue(area > 0.0f, "[$stepContext] Triangle $i area must be > 0, was $area. Breadcrumbs: $breadcrumbs")

            // 6. 頂点座標の正常性 (NaN / Infinity でないこと)
            val pt = t.pointnumber
            assertFalse(pt.x.isNaN() || pt.y.isNaN(), "[$stepContext] Triangle $i pointnumber is NaN: (${pt.x}, ${pt.y})")
            assertFalse(pt.x.isInfinite() || pt.y.isInfinite(), "[$stepContext] Triangle $i pointnumber is Infinite")
        }

        // 7. 基線接続の整合性検証 (基線一致判定)
        verifyBaselineIntegrity(stepContext)
    }

    /** 基線長・接続座標の幾何的一致を検証 */
    fun verifyBaselineIntegrity(stepContext: String) {
        val issues = com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator.validateList(triangleList)
        val errors = issues.filter {
            it.severity == com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator.Severity.ERROR &&
            (it.type == com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator.IssueType.BASELINE_LENGTH_MISMATCH ||
             it.type == com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator.IssueType.BASELINE_GAP ||
             it.type == com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator.IssueType.INVALID_PARENT ||
             it.type == com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator.IssueType.TOPOLOGY_CYCLE)
        }
        assertTrue(errors.isEmpty(), "[$stepContext] Baseline integrity failed: ${errors.joinToString("; ")}. Breadcrumbs: $breadcrumbs")
    }

    /** 衝突・重複が存在しないことを検証 */
    fun verifyNoOverlaps(stepContext: String) {
        val issues = com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator.validateList(triangleList)
        val errors = issues.filter {
            it.severity == com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator.Severity.ERROR &&
            (it.type == com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator.IssueType.OVERLAP ||
             it.type == com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator.IssueType.INWARD_FOLD)
        }
        assertTrue(errors.isEmpty(), "[$stepContext] Overlap detected: ${errors.joinToString("; ")}. Breadcrumbs: $breadcrumbs")
    }
}
