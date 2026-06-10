package com.jpaver.trianglelist.web

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Web 段階2c (task #11): WebHitTest.hitTriangle の委譲テスト。
 *
 * 判定本体 (Triangle.isCollide の符号判定) は app 側の既存テストの守備範囲なので、
 * ここは「CSV → WebCsvReader → TriangleList.isCollide の配線が正しい」ことだけ pin する:
 * 既知 CSV の三角形の重心 → その番号、全三角形の外 → 0。
 * 重心は同じ WebCsvReader 経路の頂点から計算する (レイアウト座標の手計算に依存しない)。
 */
class WebHitTestTest {

    private val csv = """
        1,6.0,5.0,4.0,-1,-1
        2,5.0,4.0,3.0,1,1
    """.trimIndent() + "\n"

    private fun centroidOf(number: Int): Pair<Float, Float> {
        val tri = WebCsvReader.read(csv).getBy(number)
        val cx = ((tri.point[0].x + tri.pointAB.x + tri.pointBC.x) / 3.0).toFloat()
        val cy = ((tri.point[0].y + tri.pointAB.y + tri.pointBC.y) / 3.0).toFloat()
        return cx to cy
    }

    @Test
    fun `triangle1 centroid hits 1`() {
        val (cx, cy) = centroidOf(1)
        assertEquals(1, WebHitTest.hitTriangle(csv, cx, cy))
    }

    @Test
    fun `triangle2 centroid hits 2`() {
        val (cx, cy) = centroidOf(2)
        assertEquals(2, WebHitTest.hitTriangle(csv, cx, cy))
    }

    @Test
    fun `far outside point hits 0`() {
        assertEquals(0, WebHitTest.hitTriangle(csv, 1000f, 1000f))
    }
}
