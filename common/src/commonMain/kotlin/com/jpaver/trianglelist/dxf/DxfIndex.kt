package com.jpaver.trianglelist.dxf

/**
 * DXF高速検索インデックス
 * パース結果をインデックス化して高速検索を可能にする
 */
class DxfIndex(private val parseResult: DxfParseResult) {

    // 測点マップ: "No.3+11" → (x, y)
    private val stationMap: Map<String, Pair<Double, Double>> by lazy { buildStationMap() }

    // レイヤー別LINE: "中心線" → List<DxfLine>
    private val linesByLayer: Map<String, List<DxfLine>> by lazy {
        parseResult.lines.groupBy { it.layer }
    }

    // レイヤー別TEXT
    private val textsByLayer: Map<String, List<DxfText>> by lazy {
        parseResult.texts.groupBy { it.layer }
    }

    /**
     * 測点名から座標を取得
     * @param stationName 測点名（例: "No.3+11", "No.0", "No.3+15"）
     * @return 座標 (x, y) または null
     */
    fun getStationCoord(stationName: String): Pair<Double, Double>? {
        return stationMap[stationName.uppercase()]
    }

    /**
     * 2つの測点間の距離を計算
     */
    fun getDistanceBetweenStations(station1: String, station2: String): Double? {
        val p1 = getStationCoord(station1) ?: return null
        val p2 = getStationCoord(station2) ?: return null
        return kotlin.math.sqrt(
            (p2.first - p1.first) * (p2.first - p1.first) +
            (p2.second - p1.second) * (p2.second - p1.second)
        )
    }

    /**
     * 2つの測点の中間点を取得
     */
    fun getMidpointBetweenStations(station1: String, station2: String): Pair<Double, Double>? {
        val p1 = getStationCoord(station1) ?: return null
        val p2 = getStationCoord(station2) ?: return null
        return Pair((p1.first + p2.first) / 2, (p1.second + p2.second) / 2)
    }

    /**
     * 中心線を取得（レイヤー名で部分一致検索）
     */
    fun getCenterlines(layerPattern: String = "中心"): List<DxfLine> {
        return linesByLayer.entries
            .filter { it.key.contains(layerPattern, ignoreCase = true) }
            .flatMap { it.value }
    }

    /**
     * 全測点リストを取得
     */
    fun getAllStations(): Map<String, Pair<Double, Double>> = stationMap

    /**
     * 全レイヤー名を取得
     */
    fun getAllLayers(): Set<String> {
        return (linesByLayer.keys + textsByLayer.keys).toSet()
    }

    /**
     * 測点マップを構築
     * TEXT要素から "No.X" または "No.X+Y" パターンを抽出
     */
    private fun buildStationMap(): Map<String, Pair<Double, Double>> {
        val map = mutableMapOf<String, Pair<Double, Double>>()
        val stationPattern = Regex("""No\.(\d+)(\+[\d.]+)?""", RegexOption.IGNORE_CASE)

        for (text in parseResult.texts) {
            val match = stationPattern.find(text.text)
            if (match != null) {
                val stationName = match.value.uppercase()
                // TEXT の位置を測点座標として使用
                map[stationName] = Pair(text.x, text.y)
            }
        }
        return map
    }

    /**
     * デバッグ情報を出力
     */
    fun printDebugInfo() {
        println("=== DxfIndex Debug ===")
        println("Stations: ${stationMap.size}")
        stationMap.entries.sortedBy { it.key }.forEach { (name, coord) ->
            println("  $name: (${coord.first.toInt()}, ${coord.second.toInt()})")
        }
        println("Layers: ${getAllLayers().size}")
        getAllLayers().sorted().forEach { layer ->
            val lineCount = linesByLayer[layer]?.size ?: 0
            val textCount = textsByLayer[layer]?.size ?: 0
            println("  $layer: $lineCount lines, $textCount texts")
        }
        println("======================")
    }

    companion object {
        /**
         * DXFファイルからインデックスを作成
         */
        fun fromFile(filePath: String): DxfIndex? {
            return try {
                val content = java.io.File(filePath).readText()
                val parser = DxfParser()
                val result = parser.parse(content)
                DxfIndex(result)
            } catch (e: Exception) {
                println("Error creating DxfIndex: ${e.message}")
                null
            }
        }
    }
}
