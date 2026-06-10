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
 * Web 段階2b (task #10): CSV → DXF 全文。SJIS バイト化とダウンロードは JS 側
 * (encoding-japanese + Blob)。中身は WebDrawingExport (desktopTest の golden 同値テストで
 * app の golden fixture と同一出力を固定済み) に委譲。
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun buildDxfText(csv: String): String =
    WebDrawingExport.buildDxfText(csv)

/**
 * Web 段階2b (task #10): CSV → SFC 全文。filename は SFC ヘッダの FILE_NAME に入る。
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun buildSfcText(csv: String, filename: String): String =
    WebDrawingExport.buildSfcText(csv, filename)

/**
 * Web 段階2c (task #11): タップ点 (モデル座標、y 上向き) → 三角形番号 (1-based、0 = 無し)。
 * px → モデル座標の逆変換は JS 側 (ViewTransform)。判定は common の
 * TriangleList.isCollide に委譲 (WebHitTest、desktopTest でテスト済み)。
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun hitTriangle(csv: String, x: Float, y: Float): Int =
    WebHitTest.hitTriangle(csv, x, y)

/**
 * binaries.executable() のリンクに entry point が要るための no-op。
 * 段階1 は @JsExport 関数を JS から呼ぶだけで、起動時処理は無い。
 */
fun main() {
}
