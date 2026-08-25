package com.jpaver.trianglelist.datamanager

/**
 * プラットフォーム実フォントでの文字幅計測。TextFit.estimateWidth の半角/全角近似は
 * 「自前ビューワーの近似であって外部 CAD の実フォントの実測値ではない」という限界を持つ
 * (2026-08-12 user 指摘)。実測できるプラットフォームではこちらを優先する。
 *
 * 実測できないプラットフォーム (未実装、または実行時にフォント読み込みに失敗した場合) は
 * null を返し、呼び出し側 (TextFit) が既存の半角/全角近似にフォールバックする。
 */
expect object PlatformTextMetrics {
    /**
     * text の実測 advance 幅を返す。null = 未対応/失敗。
     *
     * 引数は [CapHeight] (= 大文字の高さ、DXF group code 40 と同じ量) であって em ではない。
     * 実装はプラットフォームのフォント API に渡す前に必ず [CapHeight.toEm] を通すこと ──
     * AWT の Font size も Skia の Font size も CSS の font-size も em なので、そのまま
     * 渡すと advance を約 25% 過小に返す (2026-08-25 に desktop 実装で実際に踏んだ)。
     *
     * 返る幅は引数と同じ座標系 (呼び出し側の paper-cm / model) に乗る。
     */
    fun measureWidthOrNull(text: String, capHeight: CapHeight): Float?
}
