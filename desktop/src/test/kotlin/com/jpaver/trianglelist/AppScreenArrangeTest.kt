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
    fun `図面の文字サイズで確定した配置は衝突を減らす`() {
        // 配置の正は紙 (図面の文字サイズ)。画面はその結果を描くだけ
        val source = CsvCodec.build(CsvCodec.parse(realCsv()))
        val paperTs = source.getPrintTextScale(1f, "dxf")
        val before = ModelOverlapAnalyzer.analyze(source, textSize = paperTs).collisionKindByText

        source.arrangeLabelsForDrawing()
        val after = ModelOverlapAnalyzer.analyze(source, textSize = paperTs).collisionKindByText

        println("[paper] ts=$paperTs 前=${before.size} 後=${after.size}")
        assertTrue(after.size <= before.size, "図面基準で衝突が増えた: ${before.size} → ${after.size}")
    }

    /**
     * 画面と紙で「文字と図形の比」がどれだけ違うかを数字で残す。
     *
     * 配置は紙基準で確定するので画面と図面で一致する (ScreenExportParityTest) が、
     * 画面はこの比の分だけ文字が大きく描かれるため、**紙では重なっていない所が
     * 画面では重なって見える**。画面を図面の忠実なプレビューにしたい場合は、
     * ここの比を 1.0 に近づける (= 画面の文字サイズを縮尺に合わせる) 必要がある。
     */
    @Test
    fun `画面と紙の文字比を記録する`() {
        val source = CsvCodec.build(CsvCodec.parse(realCsv()))
        val paperTs = source.getPrintTextScale(1f, "dxf")   // モデル単位
        val screenTs = 30f / 47.6f                           // 画面 30px ÷ viewscale
        val ratio = screenTs / paperTs

        val paper = ModelOverlapAnalyzer.analyze(source.also { it.arrangeLabelsForDrawing() }, textSize = paperTs)
        val screen = ModelOverlapAnalyzer.analyze(source, textSize = screenTs)
        println("[ratio] 紙=%.3f 画面=%.3f 比=%.2f倍 / 衝突 紙=%d 画面=%d".format(
            paperTs, screenTs, ratio, paper.collisionKindByText.size, screen.collisionKindByText.size))

        assertTrue(ratio > 1f, "画面の方が文字が小さいなら前提が変わっている: $ratio")
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
