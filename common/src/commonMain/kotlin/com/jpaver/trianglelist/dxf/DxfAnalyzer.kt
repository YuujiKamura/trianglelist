package com.jpaver.trianglelist.dxf

/**
 * DXFファイルのサイズ分析
 */
object DxfAnalyzer {

    data class SectionSize(
        val name: String,
        val bytes: Int,
        val percentage: Double
    )

    data class EntityCount(
        val type: String,
        val count: Int,
        val totalBytes: Int
    )

    data class AnalysisResult(
        val totalBytes: Int,
        val sections: List<SectionSize>,
        val entities: List<EntityCount>
    ) {
        fun print() {
            println("=== DXF Size Analysis ===")
            println("Total: ${totalBytes / 1024} KB")
            println()
            println("Sections:")
            sections.forEach { s ->
                println("  ${s.name}: ${s.bytes / 1024} KB (${String.format("%.1f", s.percentage)}%)")
            }
            println()
            println("Entities:")
            entities.sortedByDescending { it.totalBytes }.take(10).forEach { e ->
                println("  ${e.type}: ${e.count}個, ${e.totalBytes / 1024} KB")
            }
            println("=========================")
        }
    }

    fun analyze(content: String): AnalysisResult {
        val totalBytes = content.length

        // セクション別サイズ
        val sections = mutableListOf<SectionSize>()
        val sectionPattern = Regex("""(?s)0\r?\n\s*SECTION\r?\n\s*2\r?\n\s*(\w+)(.*?)0\r?\n\s*ENDSEC""")

        sectionPattern.findAll(content).forEach { match ->
            val name = match.groupValues[1]
            val size = match.value.length
            sections.add(SectionSize(name, size, size * 100.0 / totalBytes))
        }

        // エンティティ別カウント（DXFフォーマット: 0\nTYPE\n で始まる）
        val entityCounts = mutableMapOf<String, Pair<Int, Int>>() // type -> (count, bytes)

        // ENTITIESセクションを抽出
        val entitiesSection = Regex("""(?s)0\r?\n\s*SECTION\r?\n\s*2\r?\n\s*ENTITIES(.*?)0\r?\n\s*ENDSEC""")
            .find(content)?.groupValues?.get(1) ?: ""

        // DXFフォーマット: "  0\nENTITY_TYPE\n" (グループコード0の前にスペースがある)
        // すべてのエンティティタイプを検出
        val entityPattern = Regex("""(?m)^\s*0\r?\n([A-Z][A-Z0-9_]*)\r?\n""")
        val allMatches = entityPattern.findAll(entitiesSection).toList()

        // エンティティの開始位置を記録
        data class EntityMatch(val type: String, val startPos: Int)
        val entityPositions = allMatches.mapNotNull { match ->
            val type = match.groupValues[1]
            if (type !in listOf("SECTION", "ENDSEC", "EOF")) {
                EntityMatch(type, match.range.first)
            } else null
        }.sortedBy { it.startPos }

        // 各エンティティタイプのサイズを計算
        for (i in entityPositions.indices) {
            val current = entityPositions[i]
            val nextPos = if (i < entityPositions.size - 1) {
                entityPositions[i + 1].startPos
            } else {
                entitiesSection.length
            }
            val size = nextPos - current.startPos

            val existing = entityCounts[current.type] ?: Pair(0, 0)
            entityCounts[current.type] = Pair(existing.first + 1, existing.second + size)
        }

        val entities = entityCounts.map { (type, pair) ->
            EntityCount(type, pair.first, pair.second)
        }

        return AnalysisResult(totalBytes, sections, entities)
    }

    fun analyzeFile(filePath: String): AnalysisResult? {
        return try {
            val content = java.io.File(filePath).readText()
            analyze(content)
        } catch (e: Exception) {
            println("Error analyzing DXF: ${e.message}")
            null
        }
    }
}
