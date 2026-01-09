package com.jpaver.trianglelist.dxf

// 4つの値を返すためのヘルパークラス
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * 区画線コマンド実行エンジン
 * エージェントからのコマンドを解釈して区画線を生成
 */
class MarkingCommandExecutor {
    private val crosswalkGenerator = CrosswalkGenerator()

    /**
     * コマンドを実行し、生成された線とテキストを返す
     */
    fun execute(command: MarkingCommand, parseResult: DxfParseResult, dxfPath: String? = null): ExecutionResult {
        return when (command.type.lowercase()) {
            "crosswalk" -> executeCrosswalk(command, parseResult)
            "info" -> executeInfo(parseResult)
            "analyze" -> executeAnalyze(dxfPath)
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
     * DXFサイズ分析
     */
    private fun executeAnalyze(dxfPath: String?): ExecutionResult {
        if (dxfPath == null) {
            return ExecutionResult(
                lines = emptyList(),
                texts = emptyList(),
                message = "DXFパスが指定されていません"
            )
        }
        DxfAnalyzer.analyzeFile(dxfPath)?.print()
        return ExecutionResult(
            lines = emptyList(),
            texts = emptyList(),
            message = "DXF analysis printed to console"
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

        // パラメータのデフォルト値
        val stripeLength = params["stripeLength"]?.toDoubleOrNull() ?: 4000.0
        val defaultStripeWidth = 450.0
        val defaultStripeSpacing = 450.0
        val layer = params["layer"] ?: "横断歩道"
        val anchor = params["anchor"] ?: "center"

        // 測点指定に応じてオフセットとストライプ数を決定
        val (startOffset, stripeCount, stripeWidth, stripeSpacing) = when {
            // fromStation + toStation: 2点間の区間全体をカバー
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

                val centerlineStartX = centerlines.first().x1
                // fromStation を起点とする
                val offset = from.first - centerlineStartX
                // 2点間の距離 = 横断歩道の幅
                val distance = kotlin.math.abs(to.first - from.first)

                // 距離に合わせてストライプ数を計算
                // distance = n * width + (n-1) * spacing = n * (width + spacing) - spacing
                // n = (distance + spacing) / (width + spacing)
                val width = params["stripeWidth"]?.toDoubleOrNull() ?: defaultStripeWidth
                val spacing = params["stripeSpacing"]?.toDoubleOrNull() ?: defaultStripeSpacing
                val calculatedCount = ((distance + spacing) / (width + spacing)).toInt().coerceAtLeast(1)

                println("測点配置: from=${params["fromStation"]}(${from.first}) → to=${params["toStation"]}(${to.first})")
                println("区間距離: ${distance}mm, ストライプ数: $calculatedCount")

                // 横断歩道の中心位置にオフセットを調整
                val actualWidth = calculatedCount * width + (calculatedCount - 1) * spacing
                val adjustedOffset = offset + actualWidth / 2

                Quadruple(adjustedOffset, calculatedCount, width, spacing)
            }

            // 単一測点: 測点を起点として指定長さ分
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
                val offset = station.first - centerlineStartX

                val width = params["stripeWidth"]?.toDoubleOrNull() ?: defaultStripeWidth
                val spacing = params["stripeSpacing"]?.toDoubleOrNull() ?: defaultStripeSpacing
                val count = params["stripeCount"]?.toIntOrNull() ?: 5  // デフォルト5本で約4m

                // 測点を起点にするため、横断歩道幅の半分を加算
                val totalWidth = count * width + (count - 1) * spacing
                val adjustedOffset = offset + totalWidth / 2

                println("測点配置: station=${params["station"]}(${station.first})")
                println("横断歩道幅: ${totalWidth}mm (${count}本)")

                Quadruple(adjustedOffset, count, width, spacing)
            }

            // 直接オフセット指定
            else -> {
                val offset = params["startOffset"]?.toDoubleOrNull() ?: 11000.0
                val width = params["stripeWidth"]?.toDoubleOrNull() ?: defaultStripeWidth
                val spacing = params["stripeSpacing"]?.toDoubleOrNull() ?: defaultStripeSpacing
                val count = params["stripeCount"]?.toIntOrNull() ?: 7
                Quadruple(offset, count, width, spacing)
            }
        }

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
