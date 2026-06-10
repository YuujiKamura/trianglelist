package com.jpaver.trianglelist.label

/**
 * テキスト実寸の供給元 (rev3/rev4)。判定 (DxfOverlapAnalyzer) の箱を「実際に描かれる
 * グリフのインク範囲」に一致させるための境界。実装は platform 側に置き、desktop は
 * 描画コードと同じ計測 (同じ TextStyle・同じ整列式) を写す ── 描画が真実、箱はその鏡。
 *
 * heightMm は DXF TEXT height (group code 40)。このアプリの意味論では em サイズ
 * (Android 本体の paint.textSize 系と一貫)。
 */
interface LabelMetrics {

    /**
     * アンカー基準・無回転テキストローカル座標 (DXF Y-up、model mm) のインク矩形。
     * leftMm/rightMm は X、bottomMm/topMm は Y (top > bottom)。
     * 回転は呼び出し側 (DxfOverlapAnalyzer) がアンカー回りに適用する。
     */
    data class InkBox(val leftMm: Double, val rightMm: Double, val bottomMm: Double, val topMm: Double)

    fun inkBoxLocal(text: String, heightMm: Double, alignH: Int, alignV: Int): InkBox

    /**
     * MS Gothic 等幅係数の fallback (platform 非依存)。テストと未較正環境用。
     * この DXF は STYLE で MS Gothic を指定しており (TablesBuilder.kt:194-270)、
     * MS Gothic は完全等幅 (半角 = 0.5em / 全角 = 1.0em) なので係数近似でもかなり正確。
     *
     * heightMm はキャップハイト (DXF group code 40)。em = heightMm × EM_PER_CAP で
     * 幅 = Σ(全角 1.0 / 半角 0.5) × em、高さ = 数字インク実高 (heightMm × DIGIT_INK_PER_CAP)。
     * 半角判定は char.code <= 0xFF (自前 writer の出力対象では十分な粗さ)。
     */
    object Approximate : LabelMetrics {
        /**
         * em / 較正基準高。較正基準は QCAD 互換の「'A' インク実高」(RTextRenderer.cpp の
         * アルゴリズム参照 ── テーブル値でなく 'A' の bbox 実測で正規化)。
         * fontTools 実測 (C:/Windows/Fonts/msgothic.ttc): 'A'/'5' インク実高 0.770em → 1/0.770 = 1.299。
         */
        const val EM_PER_CAP: Double = 1.299

        /** 数字インク実高 / 較正基準高。較正基準が 'A' インク実高そのものなので 1.0 (数字の見え高 = DXF height)。 */
        const val DIGIT_INK_PER_CAP: Double = 1.0

        override fun inkBoxLocal(text: String, heightMm: Double, alignH: Int, alignV: Int): InkBox {
            val units = text.sumOf { ch -> if (ch.code <= 0xFF) 0.5 else 1.0 }
            val widthMm = units * heightMm * EM_PER_CAP
            val inkHeightMm = heightMm * DIGIT_INK_PER_CAP
            val leftMm = when (alignH) {
                1 -> -widthMm / 2.0 // 中央: アンカーが中心
                2 -> -widthMm       // 右寄せ: アンカーは右端
                else -> 0.0         // 左寄せ (0): アンカーは左端
            }
            val bottomMm = when (alignV) {
                2 -> -inkHeightMm / 2.0 // 中央: アンカーが中心
                3 -> -inkHeightMm       // 上: アンカーは上端
                else -> 0.0             // ベースライン (0)・下 (1): アンカーは下端
            }
            return InkBox(leftMm, leftMm + widthMm, bottomMm, bottomMm + inkHeightMm)
        }
    }
}
