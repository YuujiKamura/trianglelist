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
    data class InkBox(val leftMm: Float, val rightMm: Float, val bottomMm: Float, val topMm: Float)

    fun inkBoxLocal(text: String, heightMm: Float, alignH: Int, alignV: Int): InkBox

    /**
     * 全半角の係数近似 fallback (platform 非依存)。テストと未較正環境用。
     * 幅 = Σ(全角 1.0 / 半角 0.5) × heightMm、高さ = heightMm。
     * 半角判定は char.code <= 0xFF (自前 writer の出力対象では十分な粗さ)。
     */
    object Approximate : LabelMetrics {
        override fun inkBoxLocal(text: String, heightMm: Float, alignH: Int, alignV: Int): InkBox {
            val units = text.sumOf { ch -> if (ch.code <= 0xFF) 0.5 else 1.0 }.toFloat()
            val widthMm = units * heightMm
            val leftMm = when (alignH) {
                1 -> -widthMm / 2f // 中央: アンカーが中心
                2 -> -widthMm      // 右寄せ: アンカーは右端
                else -> 0f         // 左寄せ (0): アンカーは左端
            }
            val bottomMm = when (alignV) {
                2 -> -heightMm / 2f // 中央: アンカーが中心
                3 -> -heightMm      // 上: アンカーは上端
                else -> 0f          // ベースライン (0)・下 (1): アンカーは下端
            }
            return InkBox(leftMm, leftMm + widthMm, bottomMm, bottomMm + heightMm)
        }
    }
}
