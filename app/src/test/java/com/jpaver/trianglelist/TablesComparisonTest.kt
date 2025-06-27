package com.jpaver.trianglelist

import com.jpaver.trianglelist.datamanager.DxfTable
import com.jpaver.trianglelist.datamanager.TablesBuilder
import com.jpaver.trianglelist.datamanager.HandleGen
import org.junit.Test
import java.io.StringWriter
import java.io.BufferedWriter

class TablesComparisonTest {

    @Test
    fun compareTables_OriginalVsBuilder() {
        // オリジナル版の出力
        val originalOutput = StringWriter()
        val originalWriter = BufferedWriter(originalOutput)
        DxfTable().tables(originalWriter)
        originalWriter.flush()
        val originalText = originalOutput.toString()

        // ビルダー版の出力
        val builderOutput = StringWriter()
        val builderWriter = BufferedWriter(builderOutput)
        HandleGen(0x30)
        val tablesBuilder = TablesBuilder()
        tablesBuilder.writeMinimalTables(builderWriter, listOf("0", "C-COL-COL1", "C-TTL-FRAM"))
        builderWriter.flush()
        val builderText = builderOutput.toString()

        // 行ごとに比較
        val originalLines = originalText.lines()
        val builderLines = builderText.lines()

        println("=== オリジナル版 ===")
        originalLines.forEachIndexed { index, line ->
            println("${index + 1}: $line")
        }

        println("\n=== ビルダー版 ===")
        builderLines.forEachIndexed { index, line ->
            println("${index + 1}: $line")
        }

        // ファイルに出力
        java.io.File("build/test-output").mkdirs()
        java.io.File("build/test-output/tables-comparison.txt").writeText("""
=== オリジナル版 ===
${originalLines.mapIndexed { index, line -> "${index + 1}: $line" }.joinToString("\n")}

=== ビルダー版 ===  
${builderLines.mapIndexed { index, line -> "${index + 1}: $line" }.joinToString("\n")}

=== 差分 ===
${(0 until maxOf(originalLines.size, builderLines.size)).mapNotNull { i ->
    val originalLine = originalLines.getOrNull(i) ?: "<missing>"
    val builderLine = builderLines.getOrNull(i) ?: "<missing>"
    if (originalLine != builderLine) "行${i + 1}: オリジナル='$originalLine' ビルダー='$builderLine'" else null
}.joinToString("\n")}

オリジナル版行数: ${originalLines.size}
ビルダー版行数: ${builderLines.size}
        """.trimIndent())

        println("比較結果をbuild/test-output/tables-comparison.txtに出力しました")
    }
} 