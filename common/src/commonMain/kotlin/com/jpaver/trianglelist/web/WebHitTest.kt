package com.jpaver.trianglelist.web

import com.example.trilib.PointXY

/**
 * Web 段階2c (task #11): タップ点 (モデル座標) → 三角形番号 (1-based、0 = 無し)。
 *
 * 判定の実体は TriangleList.isCollide (TriangleList.kt:738) → Triangle.isCollide
 * (TriangleExtensions.kt:292、符号判定の内外テスト)。ここは WebCsvReader で
 * TriangleList を作って委譲するだけ — 幾何判定を JS 側に書かないための境界点
 * (ADR 0002 当たり判定の 3 層分離)。
 *
 * 座標は renderCsvToPrimitives(scale=1) が返すプリミティブと同じモデル座標系
 * (y 上向き)。px → モデル座標の逆変換は JS 側 ViewTransform の責務。
 */
object WebHitTest {

    fun hitTriangle(csv: String, x: Float, y: Float): Int =
        WebCsvReader.read(csv).isCollide(PointXY(x, y))
}
