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
    /** text の実測 advance 幅を fs (呼び出し側の単位、通常 paper-cm) 基準で返す。null = 未対応/失敗。 */
    fun measureWidthOrNull(text: String, fs: Float): Float?
}
