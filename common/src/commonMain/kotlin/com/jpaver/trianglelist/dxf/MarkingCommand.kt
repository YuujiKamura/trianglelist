package com.jpaver.trianglelist.dxf

/**
 * 区画線コマンド（エージェントから送信）
 */
data class MarkingCommand(
    val type: String,  // "crosswalk", "stopline", "centerline" など
    val params: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * シンプルなJSONパース（外部ライブラリ不要）
         * 形式: {"type": "crosswalk", "params": {"startOffset": "11000", ...}}
         */
        fun fromJson(jsonString: String): MarkingCommand? {
            return try {
                val type = extractJsonValue(jsonString, "type") ?: return null
                val paramsStr = extractJsonObject(jsonString, "params")
                val params = if (paramsStr != null) parseParams(paramsStr) else emptyMap()
                MarkingCommand(type, params)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * コマンドリストをパース
         * 形式: {"commands": [{...}, {...}]}
         */
        fun listFromJson(jsonString: String): List<MarkingCommand> {
            val commands = mutableListOf<MarkingCommand>()
            val commandsArray = extractJsonArray(jsonString, "commands") ?: return commands

            // 各オブジェクトを抽出
            var depth = 0
            var start = -1
            for (i in commandsArray.indices) {
                when (commandsArray[i]) {
                    '{' -> {
                        if (depth == 0) start = i
                        depth++
                    }
                    '}' -> {
                        depth--
                        if (depth == 0 && start >= 0) {
                            val objStr = commandsArray.substring(start, i + 1)
                            fromJson(objStr)?.let { commands.add(it) }
                            start = -1
                        }
                    }
                }
            }
            return commands
        }

        private fun extractJsonValue(json: String, key: String): String? {
            val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\""
            val regex = Regex(pattern)
            return regex.find(json)?.groupValues?.get(1)
        }

        private fun extractJsonObject(json: String, key: String): String? {
            val keyPattern = "\"$key\"\\s*:\\s*\\{"
            val keyMatch = Regex(keyPattern).find(json) ?: return null
            val startIndex = keyMatch.range.last
            var depth = 1
            var endIndex = startIndex + 1
            while (endIndex < json.length && depth > 0) {
                when (json[endIndex]) {
                    '{' -> depth++
                    '}' -> depth--
                }
                endIndex++
            }
            return if (depth == 0) json.substring(startIndex, endIndex) else null
        }

        private fun extractJsonArray(json: String, key: String): String? {
            val keyPattern = "\"$key\"\\s*:\\s*\\["
            val keyMatch = Regex(keyPattern).find(json) ?: return null
            val startIndex = keyMatch.range.last
            var depth = 1
            var endIndex = startIndex + 1
            while (endIndex < json.length && depth > 0) {
                when (json[endIndex]) {
                    '[' -> depth++
                    ']' -> depth--
                }
                endIndex++
            }
            return if (depth == 0) json.substring(startIndex, endIndex) else null
        }

        private fun parseParams(paramsJson: String): Map<String, String> {
            val params = mutableMapOf<String, String>()
            val pattern = "\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\""
            Regex(pattern).findAll(paramsJson).forEach { match ->
                val (k, v) = match.destructured
                params[k] = v
            }
            return params
        }
    }
}

/**
 * コマンド実行結果
 */
data class CommandResult(
    val success: Boolean,
    val message: String,
    val linesAdded: Int = 0
)
