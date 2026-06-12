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
        // ISO A 判 横向き (mm)。図面枠・タイトル欄・ビューポートはすべてこの 1 値から導出される。
        val A0_LAND = Paper(1189f, 841f, "A0")
        val A1_LAND = Paper(841f, 594f, "A1")
        val A2_LAND = Paper(594f, 420f, "A2")
        val A3_LAND = Paper(420f, 297f, "A3")
        val A4_LAND = Paper(297f, 210f, "A4")

        /** 用紙名 ("A0".."A4") から横向き Paper を引く。未知名は A3 にフォールバック */
        fun ofName(name: String): Paper = when (name.uppercase()) {
            "A0" -> A0_LAND; "A1" -> A1_LAND; "A2" -> A2_LAND; "A4" -> A4_LAND
            else -> A3_LAND
        }
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