package com.jpaver.trianglelist

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.label.ModelOverlapAnalyzer
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class RealDataOverlapTest {
    @Test
    fun analyzeRealDataOverlaps() {
        val samplesDir = File("../samples")
        val files = listOf("8.4_bad.csv", "8.17_bad.csv", "8.25_bad.csv")

        for (filename in files) {
            val file = File(samplesDir, filename)
            if (!file.exists()) {
                println("Skipping $filename (not found)")
                continue
            }
            
            val csv = file.readText(Charsets.UTF_8) // Or Shift_JIS if needed
            val doc = CsvCodec.parse(csv)
            val trilist = CsvCodec.build(doc)
            val list = CsvCodec.buildMixed(doc, trilist, 1f)
            val report = ModelOverlapAnalyzer.analyze(list, textSize = 0.25f)
            val json = com.jpaver.trianglelist.web.WebOverlap.overlayJson(csv, 1.0f, "[]", 125f)
            val intrusions = Regex("\\{[^}]*\"intrusion\":\\s*true[^}]*\\}").findAll(json).map { it.value }.toList()
            println("=== Report for $filename ===")
            println("Total texts: ${report.totalTexts}")
            val lblPairs = report.pairs.filter { it.otherKind == com.jpaver.trianglelist.label.ObstacleKind.LABEL }
            println("Label vs Label pairs: ${lblPairs.size}")
            lblPairs.forEach { hit ->
                println("  Label hit: textId=${hit.textId} other=${hit.otherId} depth=${hit.depthMm}")
            }
            println("Intrusions found via WebOverlap: ${intrusions.size}")
            intrusions.forEach {
                println("  $it")
            }
            println()
        }
        
        // Just fail the test so we can see the output easily if needed, or pass it.
        assertTrue(true)
    }
}
