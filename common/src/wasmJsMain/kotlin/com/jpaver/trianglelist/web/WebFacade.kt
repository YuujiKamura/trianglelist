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
 * Web 段階2e (task #15): overrides 付き描画。overridesJson は WebOverrides の JSON 形式
 * ({"dims":[{tri,side,h,v}],"numbers":[{tri,x,y}]})。境界は文字列 1 本の既存方針のまま。
 * 空文字なら renderCsvToPrimitives と同一出力 (既存関数は互換のため残す)。
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun renderCsvToPrimitivesWithOverrides(csv: String, scale: Float, overridesJson: String): String =
    WebPrimitiveRenderer.renderCsv(csv, scale, overridesJson)

/** Web 段階2e (task #15): overrides 付き DXF。W/H フリップ・番号移動が書き出しにも乗る */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun buildDxfTextWithOverrides(csv: String, overridesJson: String): String =
    WebDrawingExport.buildDxfText(csv, overridesJson)

/** Web 段階2e (task #15): overrides 付き SFC */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun buildSfcTextWithOverrides(csv: String, filename: String, overridesJson: String): String =
    WebDrawingExport.buildSfcText(csv, filename, overridesJson)

/**
 * 番号逆順 (アプリ保存ダイアログの NumReverse、MainActivity.kt:2293) 付き DXF。
 * 効き方は DxfFileWriter.writeEntities:319-323 (resetNumReverse + 控除 reverse) が正。
 * CSV 保存には影響しない (ファイル仕様不変)
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun buildDxfTextNumReverse(csv: String, overridesJson: String, numReverse: Boolean): String =
    WebDrawingExport.buildDxfText(csv, overridesJson, numReverse)

/** 番号逆順付き SFC (SfcWriter.kt:49-53 と同経路) */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun buildSfcTextNumReverse(csv: String, filename: String, overridesJson: String, numReverse: Boolean): String =
    WebDrawingExport.buildSfcText(csv, filename, overridesJson, numReverse)

/**
 * ADR 0008: overrides 焼き込み済みの完全形式 28 列 CSV。保存 CSV にも手動配置
 * (W/H フリップ・番号移動) が乗る — アプリで開いても手動配置が失われない
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun buildCsvTextWithOverrides(csv: String, overridesJson: String): String =
    WebDrawingExport.buildCsvText(csv, overridesJson)

/**
 * 控除の配置 (web 控除編集): クリック位置 (モデル座標、y 上向き — hitTriangle と同系) に
 * 控除を置き、完成した 13 列 Deduction CSV 行を返す。pn=isCollide、pointFlag/shapeAngle=
 * flag(parent)、形状自動判定 (lenY>0 → Box)。不正パラメータは空文字列。
 * 幾何は common の WebDeduction (アプリ MainActivity.flagDeduction:1773-1820 と同式)
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun placeDeduction(csv: String, x: Double, y: Double, name: String, lenX: Double, lenY: Double, num: Int): String =
    WebDeduction.placeDeduction(csv, x.toFloat(), y.toFloat(), name, lenX.toFloat(), lenY.toFloat(), num)

/**
 * 全体回転 (fabRotate) への控除連動: Deduction CSV 行 1 本を degrees 回転して返す
 * (アプリ MainActivity.kt:1587 の dedlist.rotate(origin, -degrees) と同値)
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun rotateDeductionLine(line: String, degrees: Double): String =
    WebDeduction.rotateDeductionLine(line, degrees.toFloat())

/**
 * 控除モードの rot FAB: 選択控除を自身の中心回りに回す (位置不動、Box の shapeAngle のみ。
 * アプリ MainActivity.fabRotate の ded 分岐:1593-1600 と同式)
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun rotateDeductionShape(line: String, degrees: Double): String =
    WebDeduction.rotateDeductionShape(line, degrees.toFloat())

/**
 * 図面枠 (A3、DXF と同じ writeDrawingFrame) を prim JSON 配列で返す。layer "frame"。
 * 配置は「図形中心に枠中心を合わせる」(DXF が図形を枠へ動かすのと同じ相対配置)
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun renderFrame(csv: String): String = WebFrame.renderFrame(csv)

/**
 * binaries.executable() のリンクに entry point が要るための no-op。
 * 段階1 は @JsExport 関数を JS から呼ぶだけで、起動時処理は無い。
 */
fun main() {
}
