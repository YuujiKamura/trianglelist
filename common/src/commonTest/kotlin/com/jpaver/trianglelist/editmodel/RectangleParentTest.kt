package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * R3 (段4-1): 台形を「親」にする幾何基盤の単体テスト。
 * - Rectangle.getLine(side) が 4 辺 (A/B/C/D) を正しい端点で返す。
 * - initByParent の Rectangle 分岐が side を尊重し、子 Rectangle の底辺が親の選んだ辺にぴったり乗る。
 * - Node.d スロットと setNode2 side=3 (D辺) が機能する。
 *
 * 基準形: 独立 Rectangle(length=5, widthA=10, widthB=4, align=0, angle=0)。水平底辺で
 *   bl=(0,0) br=(10,0) tl=(0,5) tr=(4,5)  (calcPoint の頂点、WebTrapezoidTest の align テストと同系)。
 * 出典の辺マッピング: trap-design.md 段4 / WebPrimitiveRenderer.kt:184-207。
 */
class RectangleParentTest {

    private fun base() = Rectangle(5.0, 10.0, 4.0, alignment = 0)

    private fun assertPoint(exp: PointXY, act: PointXY, msg: String) {
        assertEquals(exp.x, act.x, 0.001, "$msg x")
        assertEquals(exp.y, act.y, 0.001, "$msg y")
    }

    /** getLine(0..3) の端点が bl/br/tl/tr の組合せに一致 (上表どおり) */
    @Test
    fun getLine_returns_correct_endpoints_for_each_side() {
        val r = base()
        val bl = PointXY(0f, 0f)
        val br = PointXY(10f, 0f)
        val tl = PointXY(0f, 5f)
        val tr = PointXY(4f, 5f)

        // side=0 = A 底辺 (bl→br) ── 子には出さない契約だが getLine 自体は底辺を返す (定義の健全性)
        val a = r.getLine(0)
        assertPoint(bl, a.left, "A.left(bl)")
        assertPoint(br, a.right, "A.right(br)")

        // side=1 = B 延長/左脚 (bl→tl)
        val b = r.getLine(1)
        assertPoint(bl, b.left, "B.left(bl)")
        assertPoint(tl, b.right, "B.right(tl)")

        // side=2 = C 上辺 (tl→tr)
        val c = r.getLine(2)
        assertPoint(tl, c.left, "C.left(tl)")
        assertPoint(tr, c.right, "C.right(tr)")

        // side=3 = D 右脚 (tr→br): 時計回り展開の原則で tr 起点
        val d = r.getLine(3)
        assertPoint(tr, d.left, "D.left(tr)")
        assertPoint(br, d.right, "D.right(br)")
    }

    /** getLine(2) は旧 initByParent が固定で返していた上辺C (= calcPoint().b) と同一であること (後方互換の核) */
    @Test
    fun getLine_2_equals_legacy_top_edge() {
        val r = base()
        val top = r.calcPoint().b
        val c = r.getLine(2)
        assertPoint(top.left, c.left, "上辺C.left")
        assertPoint(top.right, c.right, "上辺C.right")
    }

    // initByParent は親辺を必ず逆走する (時計回り展開の原則)。
    // よって childBase.left == parentEdge.right, childBase.right == parentEdge.left となる。
    // 「底辺が親辺に乗る」= 両端点が一致 (順序不問) でチェックする。
    private fun assertEdgeSameEndpoints(childBase: Line, parentEdge: Line, tag: String) {
        val forward = childBase.left.nearBy(parentEdge.left, 0.001) && childBase.right.nearBy(parentEdge.right, 0.001)
        val reversed = childBase.left.nearBy(parentEdge.right, 0.001) && childBase.right.nearBy(parentEdge.left, 0.001)
        assertTrue(forward || reversed,
            "$tag 底辺 [${childBase.left}..${childBase.right}] が親辺 [${parentEdge.left}..${parentEdge.right}] に乗らない")
    }

    /** 子 Rectangle(nodeA=親, side=1) の底辺が親の B辺にぴったり乗る */
    @Test
    fun child_base_lies_on_parent_B_edge() {
        val parent = base()
        val child = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 1)
        assertEdgeSameEndpoints(child.calcPoint().a, parent.getLine(1), "B辺")
    }

    /** 子 Rectangle(nodeA=親, side=2) の底辺が親の C辺(上辺)にぴったり乗る */
    @Test
    fun child_base_lies_on_parent_C_edge() {
        val parent = base()
        val child = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 2)
        assertEdgeSameEndpoints(child.calcPoint().a, parent.getLine(2), "C辺")
    }

    /** 子 Rectangle(nodeA=親, side=3) の底辺が親の D辺(右脚)にぴったり乗る */
    @Test
    fun child_base_lies_on_parent_D_edge() {
        val parent = base()
        val child = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 3)
        assertEdgeSameEndpoints(child.calcPoint().a, parent.getLine(3), "D辺")
    }

    /** setNode2(child, 3) で親の node.d が子を指す (D辺スロットの結線) */
    @Test
    fun setNode2_side3_links_child_into_node_d() {
        val parent = base()
        val child = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 3)
        child.calcPoint()   // initByParent → parent.setNode2(child, 3) を発火
        assertSame(child, parent.node.d, "親 node.d が子を指していない")
        assertSame(parent, child.node.a, "子 node.a が親を指していない (A辺共有)")
        // 既存スロットを汚していないこと
        assertTrue(parent.node.b == null && parent.node.c == null, "side=3 が b/c を汚した")
    }

    /** side=1/2 の既存結線が D辺追加で壊れていないこと */
    @Test
    fun setNode2_existing_sides_unchanged() {
        val parent = base()
        val childB = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 1).also { it.calcPoint() }
        val childC = Rectangle(2.0, 3.0, 2.0, nodeA = parent, side = 2).also { it.calcPoint() }
        assertSame(childB, parent.node.b, "side=1 が node.b を結線していない")
        assertSame(childC, parent.node.c, "side=2 が node.c を結線していない")
        assertTrue(parent.node.d == null, "side=1/2 で node.d が汚れた")
    }
}
