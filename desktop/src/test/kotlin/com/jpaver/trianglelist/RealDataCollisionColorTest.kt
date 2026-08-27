package com.jpaver.trianglelist

import com.jpaver.trianglelist.dxf.DxfParser
import com.jpaver.trianglelist.label.DxfOverlapAnalyzer
import com.jpaver.trianglelist.label.ObstacleKind
import com.jpaver.trianglelist.web.WebDrawingExport
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * 実データ (samples/8.25_bad.csv) を本番の書き出し経路で DXF にし、CAD Viewer の
 * box overlay が使う分類 (OverlapReport.collisionKindByText) を実測する。
 *
 * 目的は 2 つ:
 *  1. 「衝突している寸法テキスト」がペアの両側とも色分け対象に入ること (片側だけ赤にならない)
 *  2. 実データで色が付く件数が「全部」でも「ゼロ」でもないこと ── 全部赤/全部青は
 *     色分けが機能していない兆候 (閾値や座標系の取り違え) なので、そこで止める
 */
class RealDataCollisionColorTest {

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile
        }
        error("repo root not found")
    }

    @Test
    fun realSampleHasBothSidesColored() {
        val ms932 = java.nio.charset.Charset.forName("MS932")
        val csvFile = File(repoRoot(), "samples/8.25_bad.csv")
        assertTrue(csvFile.exists(), "実データが無い: ${csvFile.absolutePath}")

        val dxf = WebDrawingExport.buildDxfText(csvFile.readText(ms932))
        val parsed = DxfParser().parse(dxf)
        val report = DxfOverlapAnalyzer.analyze(parsed)
        val kinds = report.collisionKindByText
        val boxes = DxfOverlapAnalyzer.textBoxes(parsed)

        val byKind = ObstacleKind.entries.associateWith { k -> kinds.count { it.value == k } }
        println("=== 8.25_bad: texts=${boxes.size} colored=${kinds.size} $byKind ===")
        report.intrusions.filter { it.otherKind == ObstacleKind.LABEL }.forEach {
            println("  label pair: ${it.textId} x ${it.otherId} depth=${it.depthMm}")
        }

        assertTrue(kinds.isNotEmpty(), "実データで衝突が 1 件も出ないのはおかしい")
        assertTrue(kinds.size < boxes.size, "全テキストが衝突扱い = 色分けが機能していない")
        // ラベル同士のペアは両側が色分け対象に入る (viewer で片側だけ赤くならない)
        for (pair in report.intrusions.filter { it.otherKind == ObstacleKind.LABEL }) {
            assertTrue(
                kinds[pair.textId] == ObstacleKind.LABEL && kinds[pair.otherId] == ObstacleKind.LABEL,
                "ラベル同士ペアの片側しか色が付いていない: ${pair.textId} x ${pair.otherId} → $kinds",
            )
        }
        // 色が付く box の id は必ず overlay が描く box の id 集合に含まれる
        // (= 判定と描画が同じ経路。ここがずれると「色は付くが枠が出ない」幽霊になる)
        val boxIds = boxes.map { it.first }.toSet()
        assertTrue(kinds.keys.all { it in boxIds }, "overlay に存在しない id に色が付いた: ${kinds.keys - boxIds}")
    }
}
