package com.jpaver.trianglelist.dxf

/**
 * CADViewで描画する図形の色付きバージョン
 * DXF図形をCompose描画用に変換したもの
 */
sealed class ColoredShape {
    /**
     * 線分
     */
    data class Line(
        val x1: Double,
        val y1: Double,
        val x2: Double,
        val y2: Double,
        val color: Any // プラットフォーム固有の色型
    ) : ColoredShape()

    /**
     * 円
     */
    data class Circle(
        val centerX: Double,
        val centerY: Double,
        val radius: Double,
        val color: Any // プラットフォーム固有の色型
    ) : ColoredShape()

    /**
     * ポリライン（複数の点を結んだ線）
     */
    data class Polyline(
        val vertices: List<Pair<Double, Double>>,
        val isClosed: Boolean,
        val color: Any // プラットフォーム固有の色型
    ) : ColoredShape()

    /**
     * テキスト
     */
    data class Text(
        val x: Double,
        val y: Double,
        val text: String,
        val height: Double,
        val rotation: Double,
        val color: Any // プラットフォーム固有の色型
    ) : ColoredShape()
} 