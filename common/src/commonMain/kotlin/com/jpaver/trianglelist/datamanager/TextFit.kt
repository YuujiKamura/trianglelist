package com.jpaver.trianglelist.datamanager

/** 図面枠内テキストの「箱に収める」計算をまとめる場所。
 *  箱のサイズを先に持ち、それに収まるサイズ/改行要否を逆算する ── 文字サイズを決め打ちして
 *  はみ出るかどうかは描いてから目視で気づく、という今までの運用の逆をやる (2026-08-12)。 */
object TextFit {

    /** PlatformTextMetrics が実フォントで計測できればそれを使う (2026-08-12、desktop のみ対応、
     *  さわらびゴシック実測)。対応していないプラットフォーム (Android/Web、未実装) は
     *  半角/全角の粗い近似にフォールバックする (dumptexts で実測検証済みの旧 heuristic)。
     *  fs 基準の相対単位。呼び出し側の座標系 (paper-cm) にそのまま乗る。 */
    fun estimateWidth(text: String, fs: Float): Float {
        PlatformTextMetrics.measureWidthOrNull(text, fs)?.let { return it }
        var w = 0f
        for (ch in text) {
            val isHalf = ch.code in 0x20..0x7E
            w += if (isHalf) fs * 0.5f else fs * 1.0f
        }
        return w
    }

    /** box 幅に収まる最大サイズ。収まらなければ minSize まで縮める。
     *  縮めても収まらない場合は wraps=true (呼び出し側が改行するかの判断材料)。 */
    data class FitResult(val size: Float, val wraps: Boolean)

    /** estimateWidth は半角/全角の粗い近似 (= 自前ビューワの近似であって外部 CAD の実フォントの
     *  実測値ではない、2026-08-12 user 指摘)。DXF の STYLE は "Standard" のまま具体フォント指定が
     *  無く、開く側 (AutoCAD/Jw_cad/BricsCAD/V-nas...) がどの実フォントで描くかはこちらで確定
     *  できない ── ピクセル単位で一致させる決め手が無い。代わりに箱の 100% ではなく
     *  SAFETY_MARGIN 分を残して狙うことで、フォント差分の誤差を吸収する。 */
    private const val SAFETY_MARGIN = 0.85f

    fun fitSize(text: String, boxWidth: Float, baseSize: Float, minSize: Float = baseSize * 0.5f): FitResult {
        val targetWidth = boxWidth * SAFETY_MARGIN
        if (text.isEmpty() || boxWidth <= 0f) return FitResult(baseSize, false)
        val baseWidth = estimateWidth(text, baseSize)
        if (baseWidth <= targetWidth) return FitResult(baseSize, false)
        val shrunk = (baseSize * targetWidth / baseWidth).coerceAtLeast(minSize)
        val stillOverflows = estimateWidth(text, shrunk) > targetWidth
        return FitResult(shrunk, stillOverflows)
    }
}
