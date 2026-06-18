package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * R3 (段4-1): 台形を「親」にする幾何基盤の単体テスト。
 * side 番号 = 物理意味 (0=A 底辺, 1=B 左脚, 2=C 上辺, 3=D 右脚) で固定。 環閉合 (= 一周巡回) が
 * 要るのは signedArea / outwardPerpUnit のみで edges() override が巡回順を別軸で保持する、
 * side 番号と巡回順は別軸 (2026-06-18 確定)。
 *
 * 基準形: 独立 Rectangle(height=5, widthA=10, widthB=4, align=0)。 水平底辺で
 *   bl=(0,0) br=(10,0) tl=(0,5) tr=(4,5)
 */
class RectangleParentTest {

    private fun base() = Rectangle(5.0, 10.0, 4.0, alignment = 0)

    private fun assertPoint(exp: PointXY, act: PointXY, msg: String) {
        assertEquals(exp.x, act.x, 0.001, "$msg x")
        assertEquals(exp.y, act.y, 0.001, "$msg y")
    }

    /** getLine(0..3) は物理側面 (0=A, 1=B, 2=C, 3=D) を返す。 */
    @Test
    fun getLine_returns_correct_endpoints_for_each_side() {
        val r = base()
        val bl = PointXY(0f, 0f)
        val br = PointXY(10f, 0f)
        val tl = PointXY(0f, 5f)
        val tr = PointXY(4f, 5f)

        // side=0 = A 底辺 (br→bl、 CW 巡回起点で forward 逆向き、 CW 規約統一 2026-06-18)
        val a = r.getLine(0)
        assertPoint(br, a.left, "A.left(br)")
        assertPoint(bl, a.right, "A.right(bl)")

        // side=1 = B 左脚 (bl→tl)
        val b = r.getLine(1)
        assertPoint(bl, b.left, "B.left(bl)")
        assertPoint(tl, b.right, "B.right(tl)")

        // side=2 = C 上辺 (tl→tr)
        val c = r.getLine(2)
        assertPoint(tl, c.left, "C.left(tl)")
        assertPoint(tr, c.right, "C.right(tr)")

        // side=3 = D 右脚 (tr→br)
        val d = r.getLine(3)
        assertPoint(tr, d.left, "D.left(tr)")
        assertPoint(br, d.right, "D.right(br)")
    }

    /** getLine(2) は legacy calcPoint().b (= 上辺、 tl→tr) と同方向。 */
    @Test
    fun getLine_2_equals_legacy_top_edge() {
        val r = base()
        val top = r.calcPoint().b  // 旧 calcPoint().b は tl→tr
        val c = r.getLine(2)        // 新 getLine(2) も tl→tr
        assertPoint(top.left, c.left, "上辺C left = 旧 left")
        assertPoint(top.right, c.right, "上辺C right = 旧 right")
    }

    private fun assertEdgeSameEndpoints(childBase: Line, parentEdge: Line, tag: String) {
        val forward = childBase.left.nearBy(parentEdge.left, 0.001) && childBase.right.nearBy(parentEdge.right, 0.001)
        val reversed = childBase.left.nearBy(parentEdge.right, 0.001) && childBase.right.nearBy(parentEdge.left, 0.001)
        assertTrue(forward || reversed,
            "$tag 底辺 [${childBase.left}..${childBase.right}] が親辺 [${parentEdge.left}..${parentEdge.right}] に乗らない")
    }

    /** 子 Rectangle(nodeA=親, side=1) の底辺が親の B 辺 (左脚) にぴったり乗る (1=B 左脚)。 */
    @Test
    fun child_base_lies_on_parent_B_edge_side1() {
        val parent = base()
        val child = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 1)
        assertEdgeSameEndpoints(child.calcPoint().a, parent.getLine(1), "B辺")
    }

    /** 子 Rectangle(nodeA=親, side=2) の底辺が親の C 辺 (上辺) にぴったり乗る。 */
    @Test
    fun child_base_lies_on_parent_C_edge() {
        val parent = base()
        val child = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 2)
        assertEdgeSameEndpoints(child.calcPoint().a, parent.getLine(2), "C辺")
    }

    /** 子 Rectangle(nodeA=親, side=3) の底辺が親の D 辺 (右脚) にぴったり乗る (3=D 右脚)。 */
    @Test
    fun child_base_lies_on_parent_D_edge_side3() {
        val parent = base()
        val child = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 3)
        assertEdgeSameEndpoints(child.calcPoint().a, parent.getLine(3), "D辺")
    }

    /** setNode2(child, 1) で親の node.b が子を指す (1=B 左脚=node.b)。 */
    @Test
    fun setNode2_side1_links_child_into_node_b() {
        val parent = base()
        val child = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 1)
        child.calcPoint()
        assertSame(child, parent.node.b, "親 node.b が子を指していない")
        assertSame(parent, child.node.a, "子 node.a が親を指していない (A辺共有)")
        assertTrue(parent.node.c == null && parent.node.d == null, "side=1 が c/d を汚した")
    }

    /** side=2 (C 上辺) と side=3 (D 右脚) の結線も物理意味で揃う。 */
    @Test
    fun setNode2_existing_sides_unchanged() {
        val parent = base()
        val childC = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 2).also { it.calcPoint() }
        val childD = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 3).also { it.calcPoint() }
        assertSame(childC, parent.node.c, "side=2 が node.c を結線していない")
        assertSame(childD, parent.node.d, "side=3 が node.d を結線していない (3=D 右脚)")
        assertTrue(parent.node.b == null, "side=2/3 で node.b が汚れた")
    }
}
