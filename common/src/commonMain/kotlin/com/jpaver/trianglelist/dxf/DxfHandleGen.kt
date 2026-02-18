package com.jpaver.trianglelist.dxf

/**
 * 連番で10進ハンドル文字列を生成する。
 * AutoCAD の DXF ではハンドルを一意にする必要があるため、書き出し側で使い回す。
 */
class DxfHandleGen(start: Int = 0x100) {
    private var next = start

    fun new(): String = (next++).toString()

    fun current(): String = next.toString()
}

/**
 * シートサイズと向きをまとめたデータクラス。
 */
data class DxfPaper(val width: Float, val height: Float, val name: String) {
    companion object {
        val A3_LAND = DxfPaper(420f, 297f, "A3")
    }

    val isoName: String get() = "ISO_${name}_(${height.toInt()}.00_x_${width.toInt()}.00_MM)"
}

/**
 * DXF PlotSettings の Numerator/Denominator 分数倍率。
 */
data class DxfPrintScale(val model: Float, val paper: Float)
