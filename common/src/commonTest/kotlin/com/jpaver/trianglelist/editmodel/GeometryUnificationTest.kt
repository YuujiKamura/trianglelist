package com.jpaver.trianglelist.editmodel

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * GEOMETRY_UNIFICATION_BRIEF.md Task C: Triangle(parent: Triangle, ...) と
 * Triangle(parent: EditObject, ...) が同一座標を返すことを pin するテスト。
 *
 * legacy path: Triangle(parent: Triangle, pbc, B, C) → setOn(parent, pbc, B, C)
 * new path:    Triangle(parent: EditObject, side, B, C) → initByParent + REVERSE
 *
 * 両者の point[0] / pointAB / pointBC が一致すれば、混在リストの接続が
 * legacy setOn と同じ幾何結果を保証できる。
 */
class GeometryUnificationTest {

    private fun near(expected: Double, actual: Double, tol: Double = 0.001, msg: String = "") {
        assertTrue(abs(expected - actual) < tol, "$msg: expected=$expected actual=$actual")
    }

    private fun assertSameCoords(legacy: Triangle, newPath: Triangle, label: String) {
        near(legacy.point[0].x.toDouble(), newPath.point[0].x.toDouble(), msg = "$label point[0].x")
        near(legacy.point[0].y.toDouble(), newPath.point[0].y.toDouble(), msg = "$label point[0].y")
        near(legacy.pointAB.x.toDouble(),  newPath.pointAB.x.toDouble(),  msg = "$label pointAB.x")
        near(legacy.pointAB.y.toDouble(),  newPath.pointAB.y.toDouble(),  msg = "$label pointAB.y")
        near(legacy.pointBC.x.toDouble(),  newPath.pointBC.x.toDouble(),  msg = "$label pointBC.x")
        near(legacy.pointBC.y.toDouble(),  newPath.pointBC.y.toDouble(),  msg = "$label pointBC.y")
    }

    // ---- 基本形: 独立三角形を親にして pbc=1 / pbc=2 で比較 ----

    @Test
    fun legacy_and_editobject_path_are_identical_pbc1() {
        val parent = Triangle(6f, 5f, 4f)
        val legacy = Triangle(parent, 1, 3f, 3f)
        val newPath = Triangle(parent as EditObject, 1, 3f, 3f)
        assertSameCoords(legacy, newPath, "pbc=1 (B辺接続)")
    }

    @Test
    fun legacy_and_editobject_path_are_identical_pbc2() {
        val parent = Triangle(6f, 5f, 4f)
        val legacy = Triangle(parent, 2, 3f, 3f)
        val newPath = Triangle(parent as EditObject, 2, 3f, 3f)
        assertSameCoords(legacy, newPath, "pbc=2 (C辺接続)")
    }

    // ---- 様々な親三角形サイズで cartesian (pbc × 3 形状 = 6 ケース) ----

    @Test
    fun unification_cartesian_parent_shapes_x_pbc() {
        data class Shape(val a: Float, val b: Float, val c: Float, val label: String)
        val parents = listOf(
            Shape(6f, 5f, 4f, "6-5-4"),
            Shape(5f, 5f, 5f, "equilateral"),
            Shape(3f, 4f, 5f, "right-triangle"),
        )
        val pbcs = listOf(1, 2)

        var count = 0
        for (s in parents) {
            val parent = Triangle(s.a, s.b, s.c)
            for (pbc in pbcs) {
                count++
                val legacy  = Triangle(parent, pbc, 3f, 3f)
                val newPath = Triangle(parent as EditObject, pbc, 3f, 3f)
                assertSameCoords(legacy, newPath, "shape=${s.label} pbc=$pbc")
            }
        }
        assertTrue(count == 6, "expected 6 cases, got $count")
    }

    // ---- 子の辺長が親辺長に等しいことを確認 (A辺 = 親の接続辺長) ----

    @Test
    fun child_side_a_matches_parent_edge_length_pbc1() {
        val parent = Triangle(6f, 5f, 4f)
        val child = Triangle(parent as EditObject, 1, 3f, 3f)
        near(parent.lengthB.toDouble(), child.length[0].toDouble(), msg = "子 A辺 = 親 B辺長")
    }

    @Test
    fun child_side_a_matches_parent_edge_length_pbc2() {
        val parent = Triangle(6f, 5f, 4f)
        val child = Triangle(parent as EditObject, 2, 3f, 3f)
        near(parent.lengthC.toDouble(), child.length[0].toDouble(), msg = "子 A辺 = 親 C辺長")
    }

    // ---- 頂点 (pointBC) が親図形の外側に出る (重複しない) ----

    @Test
    fun apex_is_outside_parent_for_pbc1() {
        val parent = Triangle(6f, 5f, 4f)
        val child = Triangle(parent as EditObject, 1, 4f, 4f)
        val edge = parent.getLine(1)
        val cp = parent.centroid()
        val dx = edge.right.x - edge.left.x
        val dy = edge.right.y - edge.left.y
        val apexSide = dx * (child.pointBC.y - edge.left.y) - dy * (child.pointBC.x - edge.left.x)
        val centSide = dx * (cp.y - edge.left.y) - dy * (cp.x - edge.left.x)
        assertTrue(apexSide * centSide < 0f,
            "頂点が親の内側を向いている (重なりが発生): apexSide=$apexSide centSide=$centSide")
    }

    @Test
    fun apex_is_outside_parent_for_pbc2() {
        val parent = Triangle(6f, 5f, 4f)
        val child = Triangle(parent as EditObject, 2, 4f, 4f)
        val edge = parent.getLine(2)
        val cp = parent.centroid()
        val dx = edge.right.x - edge.left.x
        val dy = edge.right.y - edge.left.y
        val apexSide = dx * (child.pointBC.y - edge.left.y) - dy * (child.pointBC.x - edge.left.x)
        val centSide = dx * (cp.y - edge.left.y) - dy * (cp.x - edge.left.x)
        assertTrue(apexSide * centSide < 0f,
            "頂点が親の内側を向いている (重なりが発生): apexSide=$apexSide centSide=$centSide")
    }

    // ---- Rectangle を親にした Triangle の底辺が指定辺に乗る (apex 外側は保証しない) ----
    // 注: 自動反転 (apexTowardInterior) 廃止後、Rectangle の斜辺 (D辺) では
    //     REVERSE 方向が内側を向くケースがある。これは仕様 (WebTrapezoidTest の
    //     triangle_can_attach_to_rectangle_edge コメント参照)。
    //     ここでは "辺に乗る" のみを pin する。

    @Test
    fun triangle_child_of_rectangle_base_lies_on_edge_for_all_sides() {
        val rect = Rectangle(5.0, 10.0, 7.0)
        for (side in 1..3) {
            val edge = rect.getLine(side)
            val child = Triangle(rect as EditObject, side, 5f, 5f)
            val onEdge = (child.point[0].nearBy(edge.left, 0.001) && child.pointAB.nearBy(edge.right, 0.001)) ||
                (child.point[0].nearBy(edge.right, 0.001) && child.pointAB.nearBy(edge.left, 0.001))
            assertTrue(onEdge,
                "side=$side: 三角形A辺が Rectangle 辺に乗らない p0=${child.point[0]} pAB=${child.pointAB} edge=$edge")
        }
    }
}
