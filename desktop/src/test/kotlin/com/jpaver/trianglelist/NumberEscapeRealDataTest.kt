package com.jpaver.trianglelist

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.label.ModelOverlapAnalyzer
import com.jpaver.trianglelist.label.NumberCircleEscape
import com.jpaver.trianglelist.label.ObstacleKind
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File

/** 実データ 8.25 を可読サイズ (JIS 3.5mm @1/150 = 0.525) で退避させた時の実測。 */
class NumberEscapeRealDataTest {

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile
        }
        error("repo root not found")
    }

    @Test
    fun escapeOnRealData() {
        val csv = File(repoRoot(), "samples/8.25_bad.csv")
            .readText(java.nio.charset.Charset.forName("MS932"))
        val ts = 0.525f
        val doc = CsvCodec.parse(csv)
        val list = CsvCodec.buildMixed(doc, CsvCodec.build(doc), 1f)

        fun counts(): Triple<Int, Int, Int> {
            val r = ModelOverlapAnalyzer.analyze(list, textSize = ts)
            val k = r.collisionKindByText
            return Triple(
                k.count { it.value == ObstacleKind.LABEL },
                k.count { it.value == ObstacleKind.CIRCLE },
                k.size,
            )
        }

        println("before: 文字どうし=${counts().first} 円=${counts().second} 計=${counts().third}")
        val moves = NumberCircleEscape.solve(list, textSize = ts)
        println("moves=${moves.size}")
        moves.forEach { println("  #${it.shapeNumber} ${it.from} → ${it.to} 旗揚げ=${it.isFlagOut} 距離=${it.from.lengthTo(it.to)}") }
        val (labelBefore, circleBefore, _) = counts()
        NumberCircleEscape.apply(list, moves)
        val (labelAfter, circleAfter, _) = counts()
        println("after : 文字どうし=$labelAfter 円=$circleAfter 計=${counts().third}")

        // 実データ 8.25 の番号干渉は「図形内でスライドさせれば済む」規模 (user 2026-08-27)。
        // 全件が内側で解け、外へ出す (旗揚げ) 必要が無いことをここで固定する
        assertTrue(moves.none { it.isFlagOut }, "図形内で解けるはずが外へ出た: ${moves.filter { it.isFlagOut }}")
        assertEquals(0, circleAfter, "番号サークルの衝突が残った (before=$circleBefore)")
        assertTrue(labelAfter <= labelBefore, "退避で文字どうしが増えた: $labelBefore → $labelAfter")
    }
}
