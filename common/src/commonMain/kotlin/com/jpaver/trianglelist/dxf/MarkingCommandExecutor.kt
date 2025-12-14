package com.jpaver.trianglelist.dxf

/**
 * 区画線コマンド実行エンジン
 * エージェントからのコマンドを解釈して区画線を生成
 */
class MarkingCommandExecutor {
    private val crosswalkGenerator = CrosswalkGenerator()

    /**
     * コマンドを実行し、生成された線とテキストを返す
     */
    fun execute(command: MarkingCommand, parseResult: DxfParseResult): ExecutionResult {
        return when (command.type.lowercase()) {
            "crosswalk" -> executeCrosswalk(command, parseResult)
            "info" -> executeInfo(parseResult)
            else -> ExecutionResult(
                lines = emptyList(),
                texts = emptyList(),
                message = "Unknown command type: ${command.type}"
            )
        }
    }

    /**
     * DXF情報を出力（デバッグ用）
     */
    private fun executeInfo(parseResult: DxfParseResult): ExecutionResult {
        val index = DxfIndex(parseResult)
        index.printDebugInfo()
        return ExecutionResult(
            lines = emptyList(),
            texts = emptyList(),
            message = "DXF info printed to console"
        )
    }

    /**
     * 横断歩道コマンドを実行
     *
     * 配置方法:
     * 1. startOffset指定: 中心線始点からのオフセット（mm）
     * 2. fromStation/toStation指定: 測点名から中間点を計算
     */
    private fun executeCrosswalk(command: MarkingCommand, parseResult: DxfParseResult): ExecutionResult {
        val params = command.params
        val index = DxfIndex(parseResult)

        // 中心線をフィルタリング
        val centerlineLayer = params["centerlineLayer"] ?: "中心"
        val centerlines = crosswalkGenerator.filterCenterlinesByLayer(parseResult.lines, centerlineLayer)
            .ifEmpty { parseResult.lines }

        if (centerlines.isEmpty()) {
            return ExecutionResult(
                lines = emptyList(),
                texts = emptyList(),
                message = "中心線が見つかりません"
            )
        }

        // startOffsetを決定
        val startOffset: Double = when {
            // 測点名指定の場合
            params["fromStation"] != null && params["toStation"] != null -> {
                val from = index.getStationCoord(params["fromStation"]!!)
                val to = index.getStationCoord(params["toStation"]!!)
                if (from == null || to == null) {
                    return ExecutionResult(
                        lines = emptyList(),
                        texts = emptyList(),
                        message = "測点が見つかりません: fromStation=${params["fromStation"]}, toStation=${params["toStation"]}"
                    )
                }
                // 中間点のX座標を中心線始点からのオフセットとして計算
                val midX = (from.first + to.first) / 2
                val centerlineStartX = centerlines.first().x1
                println("測点配置: from=${params["fromStation"]}(${from.first}), to=${params["toStation"]}(${to.first})")
                println("中間点X: $midX, 中心線始点X: $centerlineStartX")
                println("計算オフセット: ${midX - centerlineStartX}")
                midX - centerlineStartX
            }
            // 単一測点指定の場合
            params["station"] != null -> {
                val station = index.getStationCoord(params["station"]!!)
                if (station == null) {
                    return ExecutionResult(
                        lines = emptyList(),
                        texts = emptyList(),
                        message = "測点が見つかりません: ${params["station"]}"
                    )
                }
                val centerlineStartX = centerlines.first().x1
                station.first - centerlineStartX
            }
            // 直接オフセット指定
            else -> params["startOffset"]?.toDoubleOrNull() ?: 11000.0
        }

        // その他のパラメータ
        val stripeLength = params["stripeLength"]?.toDoubleOrNull() ?: 4000.0
        val stripeWidth = params["stripeWidth"]?.toDoubleOrNull() ?: 450.0
        val stripeCount = params["stripeCount"]?.toIntOrNull() ?: 7
        val stripeSpacing = params["stripeSpacing"]?.toDoubleOrNull() ?: 450.0
        val layer = params["layer"] ?: "横断歩道"
        val anchor = params["anchor"] ?: "center"

        // 横断歩道を生成
        val crosswalkLines = crosswalkGenerator.generateCrosswalk(
            centerlineLines = centerlines,
            startOffset = startOffset,
            stripeLength = stripeLength,
            stripeWidth = stripeWidth,
            stripeCount = stripeCount,
            stripeSpacing = stripeSpacing,
            layer = layer,
            anchor = anchor
        )

        // ラベルテキスト（オプション）
        val labelText = params["label"]
        val texts = if (labelText != null) {
            val labelX = params["labelX"]?.toDoubleOrNull() ?: startOffset
            val labelY = params["labelY"]?.toDoubleOrNull() ?: 3500.0
            listOf(DxfText(labelX, labelY, labelText, 300.0, 0.0, 5, 1, 0, layer))
        } else {
            emptyList()
        }

        return ExecutionResult(
            lines = crosswalkLines,
            texts = texts,
            message = "横断歩道を生成: ${crosswalkLines.size}本の線"
        )
    }

    data class ExecutionResult(
        val lines: List<DxfLine>,
        val texts: List<DxfText>,
        val message: String
    )
}
