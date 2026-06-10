package com.jpaver.trianglelist.web

/**
 * Web 段階1 の JS 境界 (insight #61)。
 * @JsExport は experimental で複雑型が通らないため、境界は文字列 1 本に限定する。
 * 中身は commonMain の WebPrimitiveRenderer (= desktopTest でテスト可能な層) に委譲。
 *
 * @param csv   三角形 CSV (番号,辺A,辺B,辺C[,親番号,接続タイプ] 形式。WebCsvReader 参照)
 * @param scale モデル座標への倍率 (TriangleList.setScale)。通常 1.0f
 * @return 描画プリミティブ (line/text/circle) のフラット JSON 配列。
 *         座標はモデル座標系 (y 上向き)。y 反転とキャンバス fit は JS 側で行う
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun renderCsvToPrimitives(csv: String, scale: Float): String =
    WebPrimitiveRenderer.renderCsv(csv, scale)

/**
 * binaries.executable() のリンクに entry point が要るための no-op。
 * 段階1 は @JsExport 関数を JS から呼ぶだけで、起動時処理は無い。
 */
fun main() {
}
