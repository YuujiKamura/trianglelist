package com.jpaver.trianglelist

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DimsTest {

    private lateinit var tri: Triangle
    private lateinit var dims: Dims

    @Before
    fun setUp() {
        // テスト毎に初期化する
        tri = Triangle(20f, 2f, 19f)
        dims = Dims(tri)
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
    fun autoDimHorizontal_skipsWhenNotSharp() {
        // 長さ比 B/A=5/3>0.1 かつ C/A=5/3>0.1 なら自動配置しない
        tri = Triangle(3f, 5f, 5f)
        dims = Dims(tri)

        dims.horizontal.b = 0
        dims.horizontal.c = 0

        dims.autoDimHorizontal()

        assertEquals(0, dims.horizontal.b)
        assertEquals(0, dims.horizontal.c)
    }

    @Test
    fun autoDimHorizontal_setsC_whenBAngleIsSmallAndCIsLargest() {
        // ratioB=2/20=0.1, ratioC=19/20=0.95 -> 自動処理実行
        // ∠B~6°, ∠C~58° → C側を選択
        tri = Triangle(20f, 2f, 19f)
        dims = Dims(tri)

        // nodeTriangleC==null なので OUTERRIGHT = 3
        tri.nodeTriangleC = null

        dims.horizontal.b = 0
        dims.horizontal.c = 0
        dims.autoDimHorizontal()

        assertEquals(3, dims.horizontal.c)
        assertTrue(dims.flag[2].isAutoAligned)
        assertEquals(0, dims.horizontal.b)
    }

    @Test
    fun autoDimHorizontal_usesLeftWhenNodeCExists() {
        // nodeTriangleC != null により OUTERLEFT = 4 を使用
        tri = Triangle(20f, 2f, 19f)
        dims = Dims(tri)
        tri.nodeTriangleC = Triangle(3f,4f,5f)

        dims.horizontal.c = 0
        dims.autoDimHorizontal()

        assertEquals(4, dims.horizontal.c)
    }

    @Test
    fun autoDimHorizontal_skipsIfUserMovedTargetSide() {
        // C側ターゲットがユーザー操作済の場合 スキップ
        tri = Triangle(20f, 2f, 19f)
        dims = Dims(tri)

        // C側が選択されるケース
        dims.flag[2].isMovedByUser = true

        dims.horizontal.c = 0
        dims.autoDimHorizontal()

        assertEquals(0, dims.horizontal.c)
    }

    @Test
    fun autoDimHorizontal_doesNotTouchSmallSideForThinTriangle() {
        // 「20:2:20」のような細長い三角形では、小さいB側は動かない
        tri = Triangle(20f, 2f, 20f)
        dims = Dims(tri)

        // nodeTriangleC==null で C側が選ばれる
        tri.nodeTriangleC = null

        dims.horizontal.b = 0
        dims.autoDimHorizontal()

        // B側(小辺)は変更しない
        assertEquals(0, dims.horizontal.b)
    }
}