package com.jpaver.trianglelist

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.label.ModelOverlapAnalyzer
import com.jpaver.trianglelist.label.ObstacleKind
import com.jpaver.trianglelist.scale.TextSizePolicy
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * 「紙面に対して視認できる最低限の文字サイズ」= JIS Z 8313 の寸法値 3.5mm。
 * その 1 点で実データ 8.25 がどうなるかを固定する (2026-08-27 user 指示
 * 「テキストを無限にでかくすればいいわけではない。紙面サイズに対して視認が出来る
 * 最低限のテキストサイズというのがまずあって、その時点でどうか？を考えればそれでいい」)。
 *
 * 現状: 寸法値の大きさは TextSizePolicy (JIS) ではなく TextScaleCalculator の
 * 固定表から来ていて、1/150 図面で 0.25 model = 紙 1.667mm ── JIS の 3.5mm に対し
 * 2.1 倍小さい (viewer の Inspector も「1.9 倍小」と出す)。つまり**今の図面は
 * まだ可読サイズに乗っていない**。可読サイズに乗せた時に初めて出る衝突がここの数字。
 */
class JisTextSizeCollisionTest {

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile
        }
        error("repo root not found")
    }

    @Test
    fun collisionsAtMinimumLegibleSize() {
        val csv = File(repoRoot(), "samples/8.25_bad.csv")
            .readText(java.nio.charset.Charset.forName("MS932"))
        val doc = CsvCodec.parse(csv)
        val scaleDenominator = 150f // 実データの図面縮尺 (図枠 "1/150 (A3)")

        // 現行の寸法値サイズ (TextScaleCalculator の 1/150 → 0.25) と、
        // JIS 3.5mm 紙を model に直した時のサイズの比
        val currentModel = 0.25f
        val currentPaperMm = TextSizePolicy.modelToPaper(currentModel * 1000f, scaleDenominator)
        val jisModel = currentModel * (TextSizePolicy.DIMENSION_PAPER_MM / currentPaperMm)

        println("現行: model=$currentModel → 紙 ${currentPaperMm}mm")
        println("JIS : model=$jisModel → 紙 ${TextSizePolicy.DIMENSION_PAPER_MM}mm (係数 ${jisModel / currentModel})")

        for ((label, size) in listOf("現行" to currentModel, "JIS最低可読" to jisModel)) {
            val trilist = CsvCodec.build(doc)
            val list = CsvCodec.buildMixed(doc, trilist, 1f)
            val report = ModelOverlapAnalyzer.analyze(
                list, textSize = size, scale = 1f, sokutenListVector = trilist.sokutenListVector,
            )
            val kinds = report.collisionKindByText
            val labelN = kinds.count { it.value == ObstacleKind.LABEL }
            val circleN = kinds.count { it.value == ObstacleKind.CIRCLE }
            println("$label size=$size → 全${report.totalTexts}寸法中 文字どうし=$labelN 円=$circleN 計=${kinds.size}")
            // どの図形の話かを id で出す (dim:<図形番号>:<辺 0=A/1=B/2=C>)
            kinds.entries.sortedBy { it.key }.forEach { (id, kind) ->
                val partner = report.pairs.firstOrNull {
                    (it.textId == id || it.otherId == id) && it.otherKind == kind
                }
                println("   $id ← $kind (相手 ${partner?.let { if (it.textId == id) it.otherId else it.textId }})")
            }
        }

        // 可読サイズに乗せると衝突が出る = 旗揚げによる解決が必要になる、が出発点の事実。
        // ここが 0 になったら (= 自動退避が効いたら) この assert を落として counts を pin し直す
        val trilist = CsvCodec.build(doc)
        val list = CsvCodec.buildMixed(doc, trilist, 1f)
        val jisReport = ModelOverlapAnalyzer.analyze(
            list, textSize = jisModel, scale = 1f, sokutenListVector = trilist.sokutenListVector,
        )
        assertTrue(
            jisReport.collisionKindByText.isNotEmpty(),
            "可読サイズで衝突が 0 なら、そもそも退避の議論が要らない ── 前提が変わったので数字を取り直せ",
        )
    }
}
