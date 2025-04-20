package com.jpaver.trianglelist

import org.junit.Assert.*
import org.junit.Test

class DimsTest {

    @Test
    fun `autoDimHorizontalByAngle detects sharp angle correctly`() {
        // 鋭角のある三角形を準備 (例: A角が非常に鋭角になる場合)
        val triangle = Triangle(10f,1f,10f)

        val dims = Dims(triangle).apply {
            enableAutoHorizontal = true
        }

        dims.arrangeDims(isHorizontal = true)

        // A辺は今回処理外。B,Cどちらかが鋭角判定されているはず
        val aligned = dims.horizontal.b == dims.OUTERLEFT || dims.horizontal.c == dims.OUTERLEFT ||
                dims.horizontal.b == dims.OUTERRIGHT || dims.horizontal.c == dims.OUTERRIGHT

        assertTrue("鋭角時に辺Bまたは辺Cが外側配置されるべき", aligned)
    }

    @Test
    fun `autoDimVertical returns outer if no node connected`() {
        val triangle = Triangle()
        val dims = Dims(triangle)

        assertEquals(dims.OUTER, dims.autoDimVertical(dims.SIDEB))
        assertEquals(dims.OUTER, dims.autoDimVertical(dims.SIDEC))
    }

    @Test
    fun `flipVertical flips side correctly`() {
        val triangle = Triangle()
        val dims = Dims(triangle)

        assertEquals(dims.INNER, dims.flipVertical(dims.OUTER))
        assertEquals(dims.OUTER, dims.flipVertical(dims.INNER))
    }
}
