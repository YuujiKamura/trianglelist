package com.jpaver.trianglelist.model
/*
import com.example.trilib.PointXY
import kotlin.math.cos
import kotlin.math.sin


class GraphShape<T>(
    value: T,
    private val baseLength: Float,
    private val baseAngle: Float  // ラジアン単位
) : GraphNode<T>(value) {
    val vertices: MutableList<PointXY> = mutableListOf()

    init {
        calculateVertices()
    }

    private fun calculateVertices() {
        vertices.clear()

        val basePoint = if (neighbors.isNotEmpty() && neighbors[0] is GraphShape<*>) {
            (neighbors[0] as GraphShape<T>).vertices[0]
        } else {
            PointXY(0f, 0f)  // 基点が指定されていない場合、原点を基点とする
        }

        // 基点を追加
        vertices.add(basePoint)

        // 残りの頂点を計算
        for (i in 1..2) {  // 三角形なので2つの頂点を計算
            val angle = baseAngle + (Math.PI / 3) * (i - 1)  // 三角形の各内角は60度
            val x = basePoint.x + baseLength * cos(angle).toFloat()
            val y = basePoint.y + baseLength * sin(angle).toFloat()
            vertices.add(PointXY(x, y))
        }
    }

    // 三角形の面積を計算するメソッド（ヘロンの公式などを使用）
    fun calculateArea(): Float {
        // 面積計算ロジックを実装
        return 0f  // 仮の戻り値
    }
}*/