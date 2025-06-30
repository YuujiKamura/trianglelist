package com.jpaver.trianglelist.dxf

/**
 * DXF解析結果を格納するメインデータクラス
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
 */
data class DxfLine(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val color: Int = 7
)

/**
 * DXF CIRCLE エンティティ
 */
data class DxfCircle(
    val centerX: Double,
    val centerY: Double,
    val radius: Double,
    val color: Int = 7
)

/**
 * DXF LWPOLYLINE エンティティ
 */
data class DxfLwPolyline(
    val vertices: List<Pair<Double, Double>>,
    val isClosed: Boolean = false,
    val color: Int = 7
)

/**
 * DXF TEXT エンティティ
 */
data class DxfText(
    val x: Double,
    val y: Double,
    val text: String,
    val height: Double = 1.0,
    val rotation: Double = 0.0,
    val color: Int = 7,
    val alignH: Int = 0, // 水平アライメント（0=左、1=中央、2=右）
    val alignV: Int = 0  // 垂直アライメント（0=ベースライン、1=下、2=中央、3=上）
)

/**
 * DXF ヘッダー情報
 */
data class DxfHeader(
    val acadVer: String = "AC1015",
    val insUnits: Int = 6,
    val extMin: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0), // 図面範囲最小値
    val extMax: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0), // 図面範囲最大値
    val limMin: Pair<Double, Double> = Pair(0.0, 0.0), // 図面制限最小値
    val limMax: Pair<Double, Double> = Pair(0.0, 0.0), // 図面制限最大値
    val insBase: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0), // 挿入基点
    val dimScale: Double = 1.0 // 寸法尺度
)