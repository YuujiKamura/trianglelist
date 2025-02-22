package com.jpaver.trianglelist.geometry

import com.example.trilib.PointXY
import org.junit.Test
import org.junit.Assert.*

class TriangleCalculatorTest {
    
    private val DELTA = 0.001f // 浮動小数点の比較用の許容誤差

    @Test
    fun calculatePoint_正三角形の頂点を計算() {
        val basepoint = com.example.trilib.PointXY(0f, 0f)
        val pointAB = com.example.trilib.PointXY(100f, 0f)
        val lengths = floatArrayOf(100f, 100f, 100f)

        val result = TriangleCalculator.calculatePoint(basepoint, pointAB, lengths)
        
        // デバッグ用の出力を追加
        println("Expected: x=50.0, y=86.6")
        println("Actual: x=${result.x}, y=${result.y}")
        
        assertEquals(50f, result.x, DELTA)
        assertEquals(-86.602f, result.y, DELTA)
    }

    @Test
    fun calculateInternalAngle_直角三角形のテスト() {
        val p1 = com.example.trilib.PointXY(0f, 0f)
        val p2 = com.example.trilib.PointXY(100f, 0f)
        val p3 = com.example.trilib.PointXY(100f, 100f)

        val angle = TriangleCalculator.calculateInternalAngle(p1, p2, p3)
        
        println("Expected angle: 90.0")
        println("Actual angle: $angle")
        
        assertEquals(90.0, angle, DELTA.toDouble())
    }

    @Test
    fun calculateCenter_正三角形の重心テスト() {
        val points = arrayOf(
            com.example.trilib.PointXY(0f, 0f),
            com.example.trilib.PointXY(100f, 0f),
            com.example.trilib.PointXY(50f, 86.6f)  // Y座標を正の値に
        )

        val center = TriangleCalculator.calculateCenter(points)

        assertEquals(50f, center.x, DELTA)
        assertEquals(28.866f, center.y, DELTA)  // Y座標の符号を反転（負から正へ）
    }

    @Test
    fun calculateInternalAngles_正三角形のテスト() {
        val p1 = com.example.trilib.PointXY(0f, 0f)
        val p2 = com.example.trilib.PointXY(100f, 0f)
        val p3 = com.example.trilib.PointXY(50f, 86.6f)  // Y座標を正の値に

        val (angle1, angle2, angle3) = TriangleCalculator.calculateInternalAngles(p1, p2, p3)

        // 正三角形なので全ての角が60度
        assertEquals(59.999f, angle1, DELTA)
        assertEquals(60.001f, angle2, DELTA)
        assertEquals(59.999f, angle3, DELTA)
    }

    @Test
    fun calculateInternalAngles_直角三角形のテスト() {
        val p1 = com.example.trilib.PointXY(0f, 0f)
        val p2 = com.example.trilib.PointXY(100f, 0f)
        val p3 = com.example.trilib.PointXY(100f, 100f)

        val (angle1, angle2, angle3) = TriangleCalculator.calculateInternalAngles(p1, p2, p3)

        println("=== 直角三角形の内角テスト ===")
        println("点1 (${p1.x}, ${p1.y})")
        println("点2 (${p2.x}, ${p2.y})")
        println("点3 (${p3.x}, ${p3.y})")
        println("角度1: $angle1° (期待値: 45°)")
        println("角度2: $angle2° (期待値: 90°)")
        println("角度3: $angle3° (期待値: 45°)")
        println("合計: ${angle1 + angle2 + angle3}° (期待値: 180°)")

        assertEquals(90f, angle1, DELTA)
        assertEquals(45f, angle2, DELTA)
        assertEquals(45f, angle3, DELTA)
    }
} 