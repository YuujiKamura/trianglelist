package com.jpaver.trianglelist

import org.junit.Assert.*
import org.junit.Test

/** 簡易生成ヘルパー */
private fun tri(a: Float = 3f, b: Float = 4f, c: Float = 5f) = Triangle(a, b, c)

class DimsTriangleTest {

    // ---------- clone ------------------------------

    @Test
    fun clone_makesIndependentFlagArray() {
        val dims = tri().dim
        dims.flag[1].isMovedByUser = true

        val cloned = dims.clone()

        assertNotSame(dims.flag, cloned.flag)           // 配列自体は別
        assertTrue(cloned.flag[1].isMovedByUser)        // 値はコピー済み

        // 元を変更しても clone 側は変わらない
        dims.flag[1].isMovedByUser = false
        assertTrue(cloned.flag[1].isMovedByUser)
    }

    // ---------- autoDimVertical --------------------

    @Test
    fun sideA_connectionTypeLessThan3_returnsOUTER() {
        val d = tri().dim
        assertEquals(d.OUTER, d.autoDimVertical(d.SIDEA))
    }

    @Test
    fun sideB_noChild_returnsOUTER() {
        val d = tri().dim
        assertEquals(d.OUTER, d.autoDimVertical(d.SIDEB))
    }

    @Test
    fun sideB_childAreaLarger_returnsOUTER() {
        val parent = tri()                  // 面積 6
        val big    = Triangle(6f, 8f, 10f)  // 面積 24
        parent.nodeTriangleB = big

        assertEquals(parent.dim.OUTER,
            parent.dim.autoDimVertical(parent.dim.SIDEB))
    }

    @Test
    fun sideB_childAreaSmaller_returnsINNER() {
        val parent = Triangle(6f, 8f, 10f)  // 面積 24
        val small  = tri()                  // 面積 6
        parent.nodeTriangleB = small

        assertEquals(parent.dim.INNER,
            parent.dim.autoDimVertical(parent.dim.SIDEB))
    }

    // ---------- ユーティリティ ----------------------

    @Test
    fun cycleIncrement_wrapsAroundMax() {
        val d = tri().dim
        assertEquals(0, d.cycleIncrement(d.HORIZONTAL_OPTIONMAX))
    }

    @Test
    fun flipVertical_invertsValue() {
        val d = tri().dim
        assertEquals(d.INNER, d.flipVertical(d.OUTER))
        assertEquals(d.OUTER, d.flipVertical(d.INNER))
    }

    // ---------- controlVertical --------------------

    @Test
    fun controlVertical_setsFlagAndFlips() {
        val t = tri()
        val before = t.dim.vertical.c

        t.controlDimVertical(t.dim.SIDEC)

        assertTrue(t.dim.flag[2].isMovedByUser)
        assertEquals(t.dim.flipVertical(before), t.dim.vertical.c)
    }

    // ---------- getunconnectedSide -----------------

    @Test
    fun getunconnectedSide_noNodeC_returnsOuterRight() {
        val d = tri().dim
        assertEquals(3, d.getunconnectedSide(3, 4))
    }

    @Test
    fun getunconnectedSide_withNodeC_returnsOuterLeft() {
        val p = tri()
        p.nodeTriangleC = tri()
        assertEquals(4, p.dim.getunconnectedSide(3, 4))
    }
}
