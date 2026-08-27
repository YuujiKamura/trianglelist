package com.jpaver.trianglelist

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.label.DimensionTextEscape
import com.jpaver.trianglelist.label.ModelOverlapAnalyzer
import com.jpaver.trianglelist.label.NumberCircleEscape
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * 走らせるタイミングを決めるための実測 (2026-08-27 user「アレンジを走らせるタイミングも
 * 工夫が必要で、毎フレームとかは全然要らない」)。
 *
 * 検出 (analyze) と 補正 (solve) はコストが 2 桁違う。検出は編集のたびに回しても
 * 気付かれない安さ、補正は明示操作 (ボタン) 向きの重さ ── この差が
 * 「常時検出 + 押した時だけ補正」という形の根拠。
 */
class EscapeCostTest {

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile
        }
        error("repo root not found")
    }

    @Test
    fun `検出は編集ごとに回せる安さ 補正はボタン向きの重さ`() {
        val csv = File(repoRoot(), "samples/8.25_bad.csv")
            .readText(java.nio.charset.Charset.forName("MS932"))
        val ts = 0.525f
        val doc = CsvCodec.parse(csv)

        // 検出のみ (画面の色分けが使うのはこれ)
        val detectList = CsvCodec.buildMixed(doc, CsvCodec.build(doc), 1f)
        ModelOverlapAnalyzer.analyze(detectList, textSize = ts) // JIT ウォームアップ
        val detectStart = System.nanoTime()
        repeat(10) { ModelOverlapAnalyzer.analyze(detectList, textSize = ts) }
        val detectMs = (System.nanoTime() - detectStart) / 1e6 / 10

        // 補正 (探索)
        val fixList = CsvCodec.buildMixed(doc, CsvCodec.build(doc), 1f)
        val fixStart = System.nanoTime()
        NumberCircleEscape.apply(fixList, NumberCircleEscape.solve(fixList, ts))
        DimensionTextEscape.apply(fixList, DimensionTextEscape.solve(fixList, ts))
        val fixMs = (System.nanoTime() - fixStart) / 1e6

        println("[cost] 図形25/寸法57: 検出=%.2fms 補正=%.1fms".format(detectMs, fixMs))

        // 検出が編集ごとに回せない重さになったら設計が変わる (色分けを常時出せなくなる)
        assertTrue(detectMs < 20.0, "検出が重くなりすぎ: ${detectMs}ms")
    }
}
