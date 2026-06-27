package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TriangleRotationTest {

    @Test
    fun testRotationConsistency_ShadowDoesNotDrift() {
        // 1. Setup a chain of triangles
        val trilist = TriangleList()
        trilist.add(Triangle(10f, 10f, 10f, PointXY(0f, 0f), 180f), true)
        trilist.add(Triangle(trilist.getBy(1), 2, 10f, 10f), true) // Add to Side C

        // 記録: 回転 0 度の時の 2 の座標
        trilist.angle = 0f
        trilist.recoverState()
        val posBefore = trilist.getBy(2).point[0].clone()

        // 2. Rotate list
        trilist.angle = 90f
        trilist.recoverState()
        
        // 3. Add a temporary "shadow" triangle to the same side
        val shadowList = trilist.clone()
        val parent = shadowList.getBy(2)
        shadowList.add(Triangle(parent, 1, 5f, 5f), true)
        
        // 重要: recoverState を呼んでも重心移動がないので、親(2)の座標は動かないはず
        shadowList.recoverState()
        
        val posAfter = shadowList.getBy(2).point[0]
        
        // 幾何学的に、回転後の世界で親の座標が維持されているか
        // (以前の重心移動ハックがあると、ここで図形が増えた分重心がズレて親の座標も変わっていた)
        assertEquals(trilist.getBy(2).point[0].x, posAfter.x, 0.001, "Parent position should be stable when shadow is added")
        assertEquals(trilist.getBy(2).point[0].y, posAfter.y, 0.001, "Parent position should be stable when shadow is added")
    }

    @Test
    fun testCenteringLogic_CenteringMovesEverything() {
        val trilist = TriangleList()
        trilist.add(Triangle(10f, 10f, 10f, PointXY(100f, 100f), 180f), true)
        
        // 最初は原点から離れている
        val cBefore = trilist.center
        assertTrue(kotlin.math.abs(cBefore.x) > 1.0)
        
        // 手動 centering ロジックの検証
        val gc = trilist.center
        trilist.move(PointXY(-gc.x, -gc.y))
        
        val newC = trilist.center
        // 誤差を許容 (0.01以内)
        assertTrue(kotlin.math.abs(newC.x) < 0.01, "Centered X should be near zero, got ${newC.x}")
        assertTrue(kotlin.math.abs(newC.y) < 0.01, "Centered Y should be near zero, got ${newC.y}")
    }
}
