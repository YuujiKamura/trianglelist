package com.jpaver.trianglelist

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.label.ModelOverlapAnalyzer
import com.jpaver.trianglelist.label.ObstacleKind
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * アプリ画面と同じ経路 (MyView.setTriangleList → clone → attachToTheView) で
 * 自動配置が実際に効くこと。
 *
 * 2026-08-27 user 報告「スマホアプリ上のビューだと判定補正表示が機能していない」の回帰테스트。
 * 原因は 2 つとも clone/サイズの取り違えだった:
 *   1. Dims.clone() が元の三角形を掴んだままで、クローンに書いた配置が読み出されない
 *   2. 判定用の余白込みサイズ (ts × 1.10) が dimHeight に残り、描画の配置計算がずれる
 *
 * アプリの実値: viewscale = 11.9 × 4 = 47.6 (MainActivity.kt:216-218)、
 * 文字サイズ = paintTexS.textSize (既定 30f、MyView.kt:115)。
 */
class AppScreenArrangeTest {

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile
        }
        error("repo root not found")
    }

    private fun realCsv() = File(repoRoot(), "samples/8.25_bad.csv")
        .readText(java.nio.charset.Charset.forName("MS932"))

    private fun collisions(ts: Float, arrange: Boolean): Map<String, ObstacleKind> {
        // MyView.setTriangleList と同じ: 元リストを clone してから view へ attach する
        val source = CsvCodec.build(CsvCodec.parse(realCsv()))
        val viewCopy = source.clone()
        viewCopy.attachToTheView(PointXY(0f, 0f), 47.6f, ts, isArrangePointNumbers = arrange)
        return ModelOverlapAnalyzer.analyze(viewCopy, textSize = ts).collisionKindByText
    }

    @Test
    fun `アプリ画面の条件で衝突が減る`() {
        val ts = 30f
        val before = collisions(ts, arrange = false)
        val after = collisions(ts, arrange = true)

        println("[app] ts=$ts 前=${before.size} 後=${after.size}")
        assertTrue(before.isNotEmpty(), "前提: この条件では衝突が出るはず")
        assertTrue(
            after.size < before.size,
            "アプリ画面の条件で自動配置が効いていない: ${before.size} → ${after.size}",
        )
    }

    @Test
    fun `判定用サイズがモデルに残らない`() {
        // 描画は tri.dimHeight で寸法位置を計算する (MyViewDimensionSource)。判定で使う
        // 余白込みサイズが残ると、塗る大きさと配置計算の大きさがずれる
        val ts = 30f
        val list = CsvCodec.build(CsvCodec.parse(realCsv())).clone()
        list.attachToTheView(PointXY(0f, 0f), 47.6f, ts)

        list.forEachItem { shape ->
            assertTrue(
                kotlin.math.abs(shape.dimHeight - ts) < 1e-3f,
                "dimHeight が描画サイズと違う: ${shape.dimHeight} (期待 $ts)",
            )
        }
    }

    @Test
    fun `クローン元は書き換わらない`() {
        val ts = 30f
        val source = CsvCodec.build(CsvCodec.parse(realCsv()))
        val beforeH = source.getBy(4).dimHorizontal.c
        val beforeNumber = source.getBy(4).pointnumber.clone()

        source.clone().attachToTheView(PointXY(0f, 0f), 47.6f, ts)

        assertTrue(source.getBy(4).dimHorizontal.c == beforeH, "元リストの寸法配置が書き換わった")
        assertTrue(
            source.getBy(4).pointnumber.lengthTo(beforeNumber) < 1e-6,
            "元リストの番号位置が書き換わった",
        )
    }
}
