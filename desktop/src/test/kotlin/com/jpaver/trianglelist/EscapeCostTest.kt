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

    /** 図形数を増やした時のコスト。アプリはモデル更新のたびに走らせるので、実機で
     *  現実的な規模 (50〜100 図形) が耐えられるかを見る。 */
    @Test
    fun `図形数を増やした時の自動配置コスト`() {
        val nl = 10.toChar().toString()
        for (n in listOf(10, 25, 50, 100)) {
            val csv = buildString {
                append("1,4.0,3.0,3.5,-1,-1").append(nl)
                for (i in 2..n) append("$i,3.0,2.8,3.2,${i - 1},${if (i % 2 == 0) 1 else 2}").append(nl)
            }
            val list = com.jpaver.trianglelist.datamanager.CsvCodec
                .build(com.jpaver.trianglelist.datamanager.CsvCodec.parse(csv))
            list.arrangeLabelsWithoutCollision(0.525f) // ウォームアップ兼 1 回目
            val start = System.nanoTime()
            list.arrangeLabelsWithoutCollision(0.525f) // 2 回目 = 収束後の定常コスト
            val steadyMs = (System.nanoTime() - start) / 1e6

            val fresh = com.jpaver.trianglelist.datamanager.CsvCodec
                .build(com.jpaver.trianglelist.datamanager.CsvCodec.parse(csv))
            val coldStart = System.nanoTime()
            fresh.arrangeLabelsWithoutCollision(0.525f)
            val coldMs = (System.nanoTime() - coldStart) / 1e6

            println("[scale] 図形$n 初回=%.1fms 収束後=%.1fms".format(coldMs, steadyMs))
        }
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
