package com.jpaver.trianglelist.datamanager

/**
 * 小さなユーティリティ: 連番で 10 進ハンドル文字列を生成する。
 * AutoCAD の DXF ではハンドルを一意にする必要があるため、書き出し側で使い回す。
 */
class HandleGen(start: Int = 0x30) {
    private var next = start

    /** returns next handle (decimal string) and increments the counter */
    fun new(): String = (next++).toString()

    /** returns the current handle without incrementing the counter */
    fun current(): String = next.toString()
}

/**
 * シートサイズと向きをまとめたシンプルなデータクラス。
 */
data class Paper(val width: Float, val height: Float, val name: String) {
    companion object {
        val A3_LAND = Paper(420f, 297f, "A3")
    }
    
    /** ISO用紙名を生成（CAD互換） */
    val isoName: String get() = "ISO_${name}_(${height.toInt()}.00_x_${width.toInt()}.00_MM)"
}

/**
 * DXF の PlotSettings は Numerator/Denominator の分数で倍率を表す。
 * アプリ側では 0.5, 2.0 といった実倍率で扱いたいので、両者を仲立ちする
 * データクラスを用意しておく。
 *
 * `model`   … 分子 (グループコード 40)
 * `paper`   … 分母 (グループコード 41)
 */
data class PrintScale(val model: Float, val paper: Float) {
    companion object {
    }
} 