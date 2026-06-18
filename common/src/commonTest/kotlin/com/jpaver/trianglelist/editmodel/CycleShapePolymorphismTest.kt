package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CycleShape の多態 (sideCount / vertices / getLine) の単体 pin。
 * user 方針 2026-06-14「あらゆる限定操作を基底クラスに寄せろ / 場合分けを吸収しろ」。
 * これが守られていれば、混在リストの上位 (bbox / 辺タップ / 描画) は kind 分岐を持たずに動く。
 */
class CycleShapePolymorphismTest {

    // lengthTo は Double を返すので tolerance も Double で受ける (Float キャスト連発を避ける)
    private fun nearF(expected: Double, actual: Double, tolerance: Double = 1e-3, msg: String = "") {
        assertTrue(abs(expected - actual) < tolerance, "$msg: expected $expected, actual $actual")
    }

    @Test
    fun triangle_sideCount_is_3_and_vertices_match_corners() {
        val t = Triangle(6f, 5f, 4f)
        assertEquals(3, t.sideCount)
        val v = t.vertices()
        assertEquals(3, v.size)
        assertTrue(v.contains(t.point[0]) && v.contains(t.pointAB) && v.contains(t.pointBC))
    }

    @Test
    fun triangle_getLine_each_side_has_correct_length() {
        // 3-4-5 直角三角形: A=3, B=4, C=5 で全辺長が pin できる
        val t = Triangle(3f, 4f, 5f)
        val a = t.getLine(0); val b = t.getLine(1); val c = t.getLine(2)
        nearF(3.0, a.left.lengthTo(a.right), msg = "A辺 (side=0)")
        nearF(4.0, b.left.lengthTo(b.right), msg = "B辺 (side=1)")
        nearF(5.0, c.left.lengthTo(c.right), msg = "C辺 (side=2)")
        // side 範囲外は空 Line
        nearF(0.0, t.getLine(3).left.lengthTo(t.getLine(3).right), msg = "範囲外 side")
    }

    @Test
    fun rectangle_sideCount_is_4_and_vertices_are_4_corners() {
        val r = Rectangle(length = 3.0, widthA = 10.0, widthB = 7.0, basepoint = PointXY(0f, 0f))
        assertEquals(4, r.sideCount)
        assertEquals(4, r.vertices().size)
    }

    @Test
    fun rectangle_getLine_each_side_has_expected_length() {
        // 台形: 底辺=10, 上辺=7, 延長 (左脚 align=0)=3。右脚 (D) は派生。
        val r = Rectangle(length = 3.0, widthA = 10.0, widthB = 7.0, basepoint = PointXY(0f, 0f))
        nearF(10.0, r.getLine(0).left.lengthTo(r.getLine(0).right), msg = "A底辺 (side=0)")
        // 環閉合順 (2026-06-18) で side index 物理意味を再割当: 1=D 右脚, 2=C 上辺, 3=B 左脚。
        // D 右脚 (派生): align=0 で bl=(0,0), br=(10,0), tl=(0,3), tr=(7,3) → 右脚 (10,0)→(7,3) = √18 ≈ 4.243。
        nearF(4.2426, r.getLine(1).left.lengthTo(r.getLine(1).right), tolerance = 0.001, msg = "D右脚 (side=1) 派生長 ≈ √18")
        nearF(7.0, r.getLine(2).left.lengthTo(r.getLine(2).right), msg = "C上辺 (side=2)")
        nearF(3.0, r.getLine(3).left.lengthTo(r.getLine(3).right), msg = "B左脚=延長 (side=3, align=0)")
        // 範囲外は空 Line
        nearF(0.0, r.getLine(4).left.lengthTo(r.getLine(4).right), msg = "範囲外 side")
    }

    @Test
    fun base_editobject_default_dispatch_returns_empty() {
        // 親 CycleShape を直接 new した場合は空契約 (上位コードが多態 dispatch で困らない既定値)
        val e = CycleShape()
        assertEquals(0, e.sideCount)
        assertTrue(e.vertices().isEmpty())
        nearF(0.0, e.getLine(0).left.lengthTo(e.getLine(0).right), msg = "default empty line")
    }
}
