package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * R3 (段4-1): 台形を「親」にする幾何基盤の単体テスト。
 * 環閉合順 (2026-06-18 規約統一) で side index は 0=A 底辺、 1=D 右脚、 2=C 上辺、 3=B 左脚。
 *
 * 基準形: 独立 Rectangle(length=5, widthA=10, widthB=4, align=0, angle=0)。水平底辺で
 *   bl=(0,0) br=(10,0) tl=(0,5) tr=(4,5)
 */
class RectangleParentTest {

    private fun base() = Rectangle(5.0, 10.0, 4.0, alignment = 0)

    private fun assertPoint(exp: PointXY, act: PointXY, msg: String) {
        assertEquals(exp.x, act.x, 0.001, "$msg x")
        assertEquals(exp.y, act.y, 0.001, "$msg y")
    }

    /** getLine(0..3) の端点が環閉合順 (bl→br→tr→tl→bl) で連続する */
    @Test
    fun getLine_returns_correct_endpoints_for_each_side() {
        val r = base()
        val bl = PointXY(0f, 0f)
        val br = PointXY(10f, 0f)
        val tl = PointXY(0f, 5f)
        val tr = PointXY(4f, 5f)

        // side=0 = A 底辺 (bl→br)
        val a = r.getLine(0)
        assertPoint(bl, a.left, "A.left(bl)")
        assertPoint(br, a.right, "A.right(br)")

        // side=1 = D 右脚 (br→tr) ── 環閉合順 2 番目
        val d = r.getLine(1)
        assertPoint(br, d.left, "D.left(br)")
        assertPoint(tr, d.right, "D.right(tr)")

        // side=2 = C 上辺 (tr→tl) ── 環閉合順 3 番目
        val c = r.getLine(2)
        assertPoint(tr, c.left, "C.left(tr)")
        assertPoint(tl, c.right, "C.right(tl)")

        // side=3 = B 左脚 (tl→bl) ── 環閉合順 4 番目 (= 一周閉じる)
        val b = r.getLine(3)
        assertPoint(tl, b.left, "B.left(tl)")
        assertPoint(bl, b.right, "B.right(bl)")
    }

    /** getLine(2) は旧 initByParent が固定で返していた上辺C と頂点位置で一致 (forward 反転は新規約) */
    @Test
    fun getLine_2_equals_legacy_top_edge() {
        val r = base()
        val top = r.calcPoint().b  // 旧 calcPoint().b は tl→tr
        val c = r.getLine(2)        // 新 getLine(2) は tr→tl (forward 反転)
        assertPoint(top.right, c.left, "上辺C 反転 left = 旧 right")
        assertPoint(top.left, c.right, "上辺C 反転 right = 旧 left")
    }

    private fun assertEdgeSameEndpoints(childBase: Line, parentEdge: Line, tag: String) {
        val forward = childBase.left.nearBy(parentEdge.left, 0.001) && childBase.right.nearBy(parentEdge.right, 0.001)
        val reversed = childBase.left.nearBy(parentEdge.right, 0.001) && childBase.right.nearBy(parentEdge.left, 0.001)
        assertTrue(forward || reversed,
            "$tag 底辺 [${childBase.left}..${childBase.right}] が親辺 [${parentEdge.left}..${parentEdge.right}] に乗らない")
    }

    /** 子 Rectangle(nodeA=親, side=1) の底辺が親の D辺 (右脚) にぴったり乗る (新規約 1=D 右脚) */
    @Test
    fun child_base_lies_on_parent_D_edge_side1() {
        val parent = base()
        val child = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 1)
        assertEdgeSameEndpoints(child.calcPoint().a, parent.getLine(1), "D辺")
    }

    /** 子 Rectangle(nodeA=親, side=2) の底辺が親の C辺 (上辺) にぴったり乗る */
    @Test
    fun child_base_lies_on_parent_C_edge() {
        val parent = base()
        val child = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 2)
        assertEdgeSameEndpoints(child.calcPoint().a, parent.getLine(2), "C辺")
    }

    /** 子 Rectangle(nodeA=親, side=3) の底辺が親の B辺 (左脚) にぴったり乗る (新規約 3=B 左脚) */
    @Test
    fun child_base_lies_on_parent_B_edge_side3() {
        val parent = base()
        val child = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 3)
        assertEdgeSameEndpoints(child.calcPoint().a, parent.getLine(3), "B辺")
    }

    /** setNode2(child, 1) で親の node.d が子を指す (新規約 1=D 右脚=node.d) */
    @Test
    fun setNode2_side1_links_child_into_node_d() {
        val parent = base()
        val child = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 1)
        child.calcPoint()
        assertSame(child, parent.node.d, "親 node.d が子を指していない")
        assertSame(parent, child.node.a, "子 node.a が親を指していない (A辺共有)")
        assertTrue(parent.node.b == null && parent.node.c == null, "side=1 が b/c を汚した")
    }

    /** side=2 (C 上辺) と side=3 (B 左脚) の結線も新規約で揃う */
    @Test
    fun setNode2_existing_sides_unchanged() {
        val parent = base()
        val childC = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 2).also { it.calcPoint() }
        val childB = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 3).also { it.calcPoint() }
        assertSame(childC, parent.node.c, "side=2 が node.c を結線していない")
        assertSame(childB, parent.node.b, "side=3 が node.b を結線していない (新規約 3=B 左脚)")
        assertTrue(parent.node.d == null, "side=2/3 で node.d が汚れた")
    }
}
