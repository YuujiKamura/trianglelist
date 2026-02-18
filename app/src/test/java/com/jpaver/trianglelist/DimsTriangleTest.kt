package com.jpaver.trianglelist

import com.jpaver.trianglelist.editmodel.Dims
import com.jpaver.trianglelist.editmodel.Triangle
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DimsTriangleTest {

    private lateinit var tri: Triangle
    private lateinit var dims: Dims

    @Before
    fun setUp() {
        // area <= 5 の小さい三角形で autoDimHorizontalByAngle が動作する
        tri = Triangle(3f, 1f, 3f)
        dims = Dims(tri)
        dims.enableAutoHorizontal = true
    }

    @Test
    fun clone_makesIndependentFlagArray() {
        dims.flag[1].isMovedByUser = true

        val clone = dims.clone()

        // flag 配列そのものは別インスタンス
        assertNotSame(dims.flag, clone.flag)
        // 値はコピー済み
        assertTrue(clone.flag[1].isMovedByUser)

        // 元を書き換えてもクローン側に影響しない
        dims.flag[1].isMovedByUser = false
        assertTrue(clone.flag[1].isMovedByUser)
    }

    @Test
    fun arrangeDims_skipsWhenNoSharpAngle() {
        // 正三角形: 全角60° > SHARP_THRESHOLD(20°) → 自動配置しない
        tri = Triangle(2f, 2f, 2f)
        dims = Dims(tri)
        dims.enableAutoHorizontal = true

        dims.horizontal.b = 0
        dims.horizontal.c = 0

        dims.arrangeDims()

        assertEquals(0, dims.horizontal.b)
        assertEquals(0, dims.horizontal.c)
    }

    @Test
    fun arrangeDims_setsC_whenAngleCAIsSharp() {
        // Triangle(3, 1, 3): angleCA ≈ 19° < 20° → SIDEC が選択される
        tri = Triangle(3f, 1f, 3f)
        dims = Dims(tri)
        dims.enableAutoHorizontal = true

        dims.horizontal.b = 0
        dims.horizontal.c = 0
        dims.arrangeDims()

        // C側に配置が設定される（OUTERRIGHT=3）
        assertEquals(3, dims.horizontal.c)
        assertTrue(dims.flag[2].isAutoAligned)
        // B側は変更しない
        assertEquals(0, dims.horizontal.b)
    }

    @Test
    fun arrangeDims_skipsIfUserMovedTargetSide() {
        // Triangle(3, 1, 3): SIDEC がターゲットだがユーザー操作済み → スキップ
        tri = Triangle(3f, 1f, 3f)
        dims = Dims(tri)
        dims.enableAutoHorizontal = true

        dims.flag[2].isMovedByUser = true

        dims.horizontal.c = 0
        dims.arrangeDims()

        assertEquals(0, dims.horizontal.c)
    }

    @Test
    fun arrangeDims_skipsWhenAreaIsLarge() {
        // area > 5 の三角形では autoDimHorizontalByAngle が即 return
        tri = Triangle(20f, 2f, 19f)
        dims = Dims(tri)
        dims.enableAutoHorizontal = true

        dims.horizontal.b = 0
        dims.horizontal.c = 0
        dims.arrangeDims()

        // 大面積は処理スキップ
        assertEquals(0, dims.horizontal.b)
        assertEquals(0, dims.horizontal.c)
    }
}
