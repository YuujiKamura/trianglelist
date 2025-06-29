package com.jpaver.trianglelist.parser

import com.example.trilib.PointXY

/**
 * DXF解析結果を格納するメインデータクラス
 * CADView.ktが期待する構造に対応
 */
data class DxfParseResult(
    val lines: List<DxfLine> = emptyList(),
    val circles: List<DxfCircle> = emptyList(),
    val lwPolylines: List<DxfLwPolyline> = emptyList(),
    val texts: List<DxfText> = emptyList(),
    val header: DxfHeader? = null
)

/**
 * DXF LINE エンティティ
 * Android版 DxfEntity.writeLine() の出力に対応
 */
data class DxfLine(
    val start: PointXY,
    val end: PointXY,
    val color: Int = 0,
    val layer: String = "0",
    val handle: String = ""
)

/**
 * DXF CIRCLE エンティティ
 * Android版 DxfEntity.writeCircle() の出力に対応
 */
data class DxfCircle(
    val center: PointXY,
    val radius: Double,
    val color: Int = 0,
    val layer: String = "0",
    val handle: String = ""
)

/**
 * DXF LWPOLYLINE エンティティ
 * Android版 DxfEntity.writeLwPolyline() の出力に対応
 */
data class DxfLwPolyline(
    val vertices: List<PointXY>,
    val closed: Boolean = false,
    val color: Int = 0,
    val layer: String = "0",
    val handle: String = ""
)

/**
 * DXF TEXT エンティティ
 * Android版 DxfEntity.writeText() の出力に対応
 */
data class DxfText(
    val text: String,
    val insertionPoint: PointXY,
    val height: Double = 0.2,
    val rotationAngle: Double = 0.0,
    val horizontalAlignment: Int = 0,
    val verticalAlignment: Int = 0,
    val color: Int = 0,
    val layer: String = "0",
    val handle: String = "",
    val textStyle: String = "DIMSTANDARD"
)

/**
 * DXF HATCH エンティティ（表示用、読み込みのみ）
 * Android版 DxfEntity.writeDXFTriHatch() の出力に対応
 */
data class DxfHatch(
    val vertices: List<PointXY>,
    val color: Int = 0,
    val layer: String = "0",
    val handle: String = "",
    val pattern: String = "SOLID"
)

/**
 * DXF ヘッダー情報（簡略版）
 * 必要に応じて拡張可能
 */
data class DxfHeader(
    val acadVer: String = "AC1015",
    val insUnits: Int = 6,
    val extMin: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0),
    val extMax: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
)

/**
 * DXF座標系設定
 */
data class DxfCoordinateSystem(
    val insBase: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0),
    val extMin: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0),
    val extMax: Triple<Double, Double, Double>,
    val limMin: Pair<Double, Double> = Pair(0.0, 0.0),
    val limMax: Pair<Double, Double>,
    val pExtMax: Triple<Double, Double, Double> // 用紙サイズ
)

/**
 * DXF寸法設定
 */
data class DxfDimensionSettings(
    val dimScale: Double,        // 寸法尺度 (重要)
    val dimTxt: Double = 0.18,   // 寸法テキスト高さ
    val dimAsz: Double = 0.18,   // 矢印サイズ
    val dimGap: Double = 0.09    // テキストギャップ
)

/**
 * DXFパース時の設定
 */
data class DxfParseSettings(
    val ignoreHatch: Boolean = false,     // ハッチングを無視
    val scaleCoordinates: Boolean = true, // 座標をスケール変換
    val unitScale: Float = 1000f,         // 単位スケール
    val filterLayers: List<String> = emptyList() // フィルターするレイヤー
)

/**
 * DXF解析エラー情報
 */
data class DxfParseError(
    val lineNumber: Int,
    val message: String,
    val entityType: String = "",
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

/**
 * エラーの重要度
 */
enum class ErrorSeverity {
    WARNING,
    ERROR, 
    FATAL
}

/**
 * 詳細なDXF解析結果（エラー情報付き）
 */
data class DxfParseResultDetailed(
    val result: DxfParseResult,
    val errors: List<DxfParseError> = emptyList(),
    val warnings: List<String> = emptyList(),
    val parseTime: Long = 0,
    val entityCounts: Map<String, Int> = emptyMap()
) 