package com.jpaver.trianglelist.datamanager

/** 図面枠内テキストの「箱に収める」計算をまとめる場所。
 *  箱のサイズを先に持ち、それに収まるサイズ/改行要否を逆算する ── 文字サイズを決め打ちして
 *  はみ出るかどうかは描いてから目視で気づく、という今までの運用の逆をやる (2026-08-12)。 */
object TextFit {

    /**
     * 実測できないプラットフォームで仮定するキャップハイト / em 比。
     *
     * 定数なのは「実測手段が無いから」であって補正値ではない ── 実測できる経路
     * (PlatformTextMetrics、web canvas の measureText、desktop Skia の 'A' 実測) は
     * 必ずそちらを使い、ここには落ちてこない。DXF の STYLE は "Standard" のままで
     * 開く側の CAD がどの実フォントで描くか確定できないため、DXF 経路だけは原理的に
     * 実測不能で、ここを通る。
     *
     * 値は「cap < em はどのフォントでも成り立ち、比は概ね 0.70〜0.80」から中央付近を取る
     * (実測値: sans-serif 0.74 / MS Gothic 0.77)。
     */
    const val ASSUMED_CAP_HEIGHT_RATIO: Float = 0.75f

    /**
     * 文字列の描画幅を返す。
     *
     * **capHeight はキャップハイト** (= DXF TEXT の group code 40 と同じ量、大文字の高さ)。
     * em ではない。返る幅は capHeight と同じ座標系 (呼び出し側の paper-cm) に乗る。
     *
     * 2026-08-25 修正: 旧実装は全角 1 文字の advance を `fs * 1.0`、つまり
     * 「1 全角 = 1 キャップハイト」と置いていたが、実際の全角 advance は 1.0 **em**。
     * em はキャップハイトより 3 割ほど大きいので、幅を約 26% 過小に見積もっていた
     * (表題欄の「図面番号」がセル幅 0.8999 に対し実描画 0.9459 ではみ出した)。
     * font-size を em と cap で取り違えていたのと同じ間違いが幅側にもあった形。
     *
     * PlatformTextMetrics が実フォントで計測できればそれを使う (2026-08-12、desktop のみ対応、
     * さわらびゴシック実測)。対応していないプラットフォーム (Android/Web、未実装) と
     * DXF 経路は、em に直してから半角/全角の粗い近似にフォールバックする。
     */
    fun estimateWidth(text: String, capHeight: CapHeight): Float {
        PlatformTextMetrics.measureWidthOrNull(text, capHeight)?.let { return it }
        // advance は em 基準の量なので、キャップハイトを em に直してから積む。
        // toEm を通さずに capHeight をそのまま積むと 26% 過小になる ── 型がそれを防ぐ。
        val em = capHeight.toEm(ASSUMED_CAP_HEIGHT_RATIO).value
        var w = 0f
        for (ch in text) {
            val isHalf = ch.code in 0x20..0x7E
            w += if (isHalf) em * 0.5f else em * 1.0f
        }
        return w
    }

    /** box 幅に収まる最大サイズ。収まらなければ minSize まで縮める。
     *  縮めても収まらない場合は wraps=true (呼び出し側が改行するかの判断材料)。 */
    data class FitResult(val size: CapHeight, val wraps: Boolean)

    /** estimateWidth は半角/全角の粗い近似 (= 自前ビューワの近似であって外部 CAD の実フォントの
     *  実測値ではない、2026-08-12 user 指摘)。DXF の STYLE は "Standard" のまま具体フォント指定が
     *  無く、開く側 (AutoCAD/Jw_cad/BricsCAD/V-nas...) がどの実フォントで描くかはこちらで確定
     *  できない ── ピクセル単位で一致させる決め手が無い。代わりに箱の 100% ではなく
     *  SAFETY_MARGIN 分を残して狙うことで、フォント差分の誤差を吸収する。 */
    //
    // 2026-08-25: 0.85 → 0.95。旧値は estimateWidth が全角幅を約 26% 過小に見積もっていた
    // 時期に目視で決めた数字で、実効的には 0.85 × 1.26 ≈ 1.07 (= 7% はみ出しを許す) だった
    // ── つまり「安全余裕」ではなく単位誤差の相殺として働いていた。幅が正しくなった今、
    // 0.85 のままだと表題欄のラベルが不要に縮む (「工 事 名」が 3.5mm → 2.6mm)。
    // 本来の意味 (開く側 CAD のフォント差を吸収する余裕) に戻す。
    private const val SAFETY_MARGIN = 0.95f

    fun fitSize(
        text: String,
        boxWidth: Float,
        baseSize: CapHeight,
        minSize: CapHeight = baseSize * 0.5f,
    ): FitResult {
        val targetWidth = boxWidth * SAFETY_MARGIN
        if (text.isEmpty() || boxWidth <= 0f) return FitResult(baseSize, false)
        val baseWidth = estimateWidth(text, baseSize)
        if (baseWidth <= targetWidth) return FitResult(baseSize, false)
        val scaled = baseSize * (targetWidth / baseWidth)
        val shrunk = if (scaled < minSize) minSize else scaled
        val stillOverflows = estimateWidth(text, shrunk) > targetWidth
        return FitResult(shrunk, stillOverflows)
    }
}
