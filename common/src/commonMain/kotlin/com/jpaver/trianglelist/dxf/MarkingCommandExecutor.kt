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
            else -> ExecutionResult(
                lines = emptyList(),
                texts = emptyList(),
                message = "Unknown command type: ${command.type}"
            )
        }
    }

    /**
     * 横断歩道コマンドを実行
     */
    private fun executeCrosswalk(command: MarkingCommand, parseResult: DxfParseResult): ExecutionResult {
        val params = command.params

        // パラメータ取得（デフォルト値付き）
        val startOffset = params["startOffset"]?.toDoubleOrNull() ?: 11000.0
        val stripeLength = params["stripeLength"]?.toDoubleOrNull() ?: 4000.0
        val stripeWidth = params["stripeWidth"]?.toDoubleOrNull() ?: 450.0
        val stripeCount = params["stripeCount"]?.toIntOrNull() ?: 7
        val stripeSpacing = params["stripeSpacing"]?.toDoubleOrNull() ?: 450.0
        val layer = params["layer"] ?: "横断歩道"
        val centerlineLayer = params["centerlineLayer"] ?: "中心"
        val anchor = params["anchor"] ?: "center"

        // 中心線をフィルタリング
        val centerlines = crosswalkGenerator.filterCenterlinesByLayer(parseResult.lines, centerlineLayer)
            .ifEmpty { parseResult.lines }

        if (centerlines.isEmpty()) {
            return ExecutionResult(
                lines = emptyList(),
                texts = emptyList(),
                message = "中心線が見つかりません"
            )
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
