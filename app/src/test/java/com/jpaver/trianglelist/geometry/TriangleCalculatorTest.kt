package com.jpaver.trianglelist.geometry

import com.example.trilib.PointXY
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class TriangleCalculatorTest {
    
    private val DELTA = 0.001f // 浮動小数点の比較用の許容誤差

    @Test
    fun calculatePoint_正三角形の頂点を計算() {
        val basepoint = PointXY(0f, 0f)
        val pointAB = PointXY(1f, 0f)
        val lengths = floatArrayOf(1f, 1f, 1f) // 辺の長さが全て1の正三角形

        val result = TriangleCalculator.calculatePoint(basepoint, pointAB, lengths)

        // 正三角形なので、y座標は√3/2になるはず
        assertEquals(0.5f, result.x, DELTA)
        assertEquals(-0.8660254f, result.y, DELTA) // 符号を反転し、精度も合わせる
    }

    @Test
    fun calculateInternalAngle_直角三角形の角度を計算() {
        val p1 = PointXY(0f, 0f)
        val p2 = PointXY(1f, 0f)
        val p3 = PointXY(1f, 1f)

        val angle = TriangleCalculator.calculateInternalAngle(p1, p2, p3)
        
        assertEquals(90.0, angle, 0.1) // 90度になるはず
    }

    @Test
    fun calculateCenter_正三角形の重心を計算() {
        val points = arrayOf(
            PointXY(0f, 0f),
            PointXY(1f, 0f),
            PointXY(0.5f, 0.866f)
        )

        val center = TriangleCalculator.calculateCenter(points)

        assertEquals(0.5f, center.x, DELTA)
        assertEquals(0.289f, center.y, DELTA) // 高さの1/3の位置
    }

    @Test
    fun calculatePoint_一般的な三角形の頂点を計算() {
        val basepoint = PointXY(0f, 0f)
        val pointAB = PointXY(3f, 0f)
        val lengths = floatArrayOf(3f, 4f, 5f) // 3-4-5の直角三角形

        val result = TriangleCalculator.calculatePoint(basepoint, pointAB, lengths)

        assertEquals(3f, result.x, DELTA)
        assertEquals(-4f, result.y, DELTA) // 符号を反転
    }
} 