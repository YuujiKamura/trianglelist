package com.jpaver.trianglelist

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.label.DimensionTextEscape
import com.jpaver.trianglelist.label.NumberCircleEscape
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * 従来ポリシーの踏襲確認: **user が自分で動かした配置は自動補正より優先** (2026-08-27
 * user 指示「図形の繋がり具合によっては、出来合いの旗揚げ補正がむしろ重なる方向に
 * 出したりするケースが出てくる、完全は最初から望めない。だから、ユーザーが旗揚げを
 * 動かした場合はそっちを優先するっていう、従来のポリシーを踏襲してるかを確認してほしい」)。
 *
 * 既存の autoAlign / autoDimHorizontalByAngle は isMovedByUser で降りる。自動退避も
 * 同じ条件で降りること ── 自動が完全でない以上、人が直した結果を上書きするのが
 * 一番damageが大きい。
 *
 * 実データ (8.25 を可読サイズ) は退避が実際に走るケースなので、ここで「印を付けた
 * 図形・辺だけが提案から消える」ことを確かめられる。
 */
class UserPlacementPriorityTest {

    private val textSize = 0.525f // JIS 3.5mm @1/150

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile
        }
        error("repo root not found")
    }

    private fun realList() = CsvCodec.parse(
        File(repoRoot(), "samples/8.25_bad.csv").readText(java.nio.charset.Charset.forName("MS932")),
    ).let { doc -> CsvCodec.buildMixed(doc, CsvCodec.build(doc), 1f) }

    @Test
    fun `user が動かした番号は自動退避が触らない`() {
        val baseline = NumberCircleEscape.solve(realList(), textSize)
        assertTrue(baseline.isNotEmpty(), "前提: 実データでは番号の退避が発生するはず")
        val target = baseline.first().shapeNumber

        val list = realList()
        list.forEachItemIndexed { num, shape ->
            if (num == target && shape is Triangle) shape.pointNumber.flag.isMovedByUser = true
        }
        val moves = NumberCircleEscape.solve(list, textSize)

        assertTrue(
            moves.none { it.shapeNumber == target },
            "user が動かした番号 #$target を自動退避が動かした: $moves",
        )
        assertTrue(moves.isNotEmpty(), "印を付けていない図形の退避まで止まっている: $moves")
    }

    @Test
    fun `user が操作した辺の寸法値は自動退避が触らない`() {
        val warm = realList()
        NumberCircleEscape.apply(warm, NumberCircleEscape.solve(warm, textSize))
        val baseline = DimensionTextEscape.solve(warm, textSize)
        assertTrue(baseline.isNotEmpty(), "前提: 実データでは寸法値の退避が発生するはず")
        val target = baseline.first()

        val list = realList()
        NumberCircleEscape.apply(list, NumberCircleEscape.solve(list, textSize))
        list.forEachItemIndexed { num, shape ->
            if (num == target.shapeNumber && shape is Triangle && target.side in 0..2) {
                shape.dim.flag[target.side].isMovedByUser = true
            }
        }
        val moves = DimensionTextEscape.solve(list, textSize)

        assertTrue(
            moves.none { it.shapeNumber == target.shapeNumber && it.side == target.side },
            "user が操作した #${target.shapeNumber} 辺${target.side} を自動退避が変えた: $moves",
        )
    }

    @Test
    fun `user が全部動かしていたら自動退避は何もしない`() {
        val list = realList()
        list.forEachItem { shape ->
            if (shape is Triangle) {
                shape.pointNumber.flag.isMovedByUser = true
                shape.dim.flag.forEach { it.isMovedByUser = true }
                shape.dim.flagS.isMovedByUser = true
            }
        }

        assertTrue(NumberCircleEscape.solve(list, textSize).isEmpty(), "番号を勝手に動かした")
        assertTrue(DimensionTextEscape.solve(list, textSize).isEmpty(), "寸法値を勝手に動かした")
    }
}
