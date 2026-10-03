package com.jpaver.trianglelist.regression.eightmillion

import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.calcPoints
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 【エイトミリオンウェイストゥダイ：幾何破綻・極限入力編】
 *
 * 針状三角形、縮退三角形、三角不等式違反、ゼロ・負長、極大極小値など、
 * 数学・浮動小数点の極限境界における死に様を網羅し、
 * アプリが NaN やクラッシュで沈没せず生き残ることを検証する。
 */
class GeometryDeathTest {

    @Test
    fun `Death_01_針状三角形_極端なアスペクト比でNaN化しない`() {
        // a=1000, b=1000, c=0.01 の超極細三角形
        val t = Triangle(1000f, 1000f, 0.01f)
        t.calcPoints()

        assertFalse(t.pointAB.x.isNaN() || t.pointAB.y.isNaN(), "pointAB must not be NaN")
        assertFalse(t.pointBC.x.isNaN() || t.pointBC.y.isNaN(), "pointBC must not be NaN")
        assertFalse(t.pointcenter.x.isNaN() || t.pointcenter.y.isNaN(), "pointcenter must not be NaN")
        assertFalse(t.getArea().isNaN(), "Area must not be NaN")
        assertTrue(t.getArea() >= 0f, "Area must be non-negative")
    }

    @Test
    fun `Death_02_完全縮退三角形_一直線になってもNaNにならない`() {
        // a=6, b=4, c=10 (a + b == c)
        val t = Triangle(10f, 6f, 4f)
        t.calcPoints()

        assertFalse(t.pointBC.x.isNaN() || t.pointBC.y.isNaN(), "pointBC must not be NaN on degenerate triangle")
        assertFalse(t.pointcenter.x.isNaN() || t.pointcenter.y.isNaN(), "pointcenter must not be NaN on degenerate triangle")
        // 面積は 0 または正であること（負やNaNにならない）
        assertFalse(t.getArea().isNaN())
    }

    @Test
    fun `Death_03_三角不等式違反_届かない辺長でもNaN汚染を起こさない`() {
        // a=100, b=1, c=1 (1 + 1 < 100 で物理的に三角形が作れない)
        val t = Triangle(100f, 1f, 1f)
        t.calcPoints()

        // 余弦定理の分子/分母が 1.0 を超えても clamp されて NaN を防ぐべき
        assertFalse(t.pointBC.x.isNaN() || t.pointBC.y.isNaN(), "pointBC must not be NaN even if sides cannot form triangle")
        assertFalse(t.pointcenter.x.isNaN() || t.pointcenter.y.isNaN(), "pointcenter must not be NaN")
    }

    @Test
    fun `Death_04_微小辺長_1e-4以下でもゼロ除算クラッシュしない`() {
        val t = Triangle(0.0001f, 0.0001f, 0.0001f)
        t.calcPoints()

        assertFalse(t.pointBC.x.isNaN() || t.pointBC.y.isNaN(), "pointBC must not be NaN for micro-triangle")
        assertTrue(t.getArea() >= 0f)
    }

    @Test
    fun `Death_05_巨大辺長_Floatオーバーフロー領域でも安全`() {
        // 1e7 は二乗で 1e14 となり Float(max 3.4e38) の範囲内
        val t = Triangle(1000000f, 1000000f, 1000000f)
        t.calcPoints()

        assertFalse(t.pointBC.x.isNaN() || t.pointBC.y.isNaN(), "pointBC must not be NaN for large triangle")
        assertFalse(t.pointBC.x.isInfinite() || t.pointBC.y.isInfinite(), "pointBC must not be Infinite")
        assertTrue(t.getArea() > 0f)
    }

    @Test
    fun `Death_06_負の辺長が渡されても即座の致命的クラッシュを防ぐ`() {
        val t = Triangle(-5f, 4f, 3f)
        // 例外で即死するか、NaN を出さずに処理できるか
        try {
            t.calcPoints()
            assertFalse(t.pointcenter.x.isNaN() || t.pointcenter.y.isNaN())
        } catch (_: IllegalArgumentException) {
            // 安全に例外で拒否されるならそれも正しい挙動
        }
    }
}
