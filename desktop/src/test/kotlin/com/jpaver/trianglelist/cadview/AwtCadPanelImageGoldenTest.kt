package com.jpaver.trianglelist.cadview

import com.jpaver.trianglelist.dxf.DxfParser
import com.jpaver.trianglelist.dxf.SfcParser
import com.jpaver.trianglelist.web.WebDrawingExport
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * 見た目の冪等性 golden 層2 (task #13): desktop AWT 描画の画像固定。
 *
 * パイプライン: CSV → WebDrawingExport.buildDxfText (common の DxfFileWriter、
 * app golden と同じ初期化列) → 日付正規化 → common の DxfParser → AwtCadPanel を
 * headless で 1200x900 の BufferedImage に paint → golden PNG と許容差つき比較。
 * 層1 (primitives JSON) が座標を厳密固定するのに対し、こちらは「実際に画面に出る絵」
 * (図枠・表・テキスト描画・ACI 色・Y 反転・fitToView 込み) の大崩れを検出する。
 *
 * 日付正規化: buildDxfText は currentDateStringJp() で実行日を図枠に書くため、
 * parse 前に固定文字列へ置換する (テキスト golden の #DATE# 正規化と同じ思想)。
 * これをしないと画像 golden が日替わりで割れる。
 *
 * 許容差: 同一マシン・同一 JVM では再描画は完全一致 (初回測定で diff 0 を確認済み)。
 * 閾値 0.5% は JVM/フォント更新でのアンチエイリアス端ゆらぎ吸収用。同一マシンでの
 * 大手術前後の回帰検出が目的で、CI のフォント差で割れるのは想定内 (CI 向きは層1)。
 *
 * golden が無い初回実行は記録して fail する (PNG を目視批評してから再実行で green)。
 * 意図した見た目変更時は golden PNG を消して再記録 + 再目視。
 */
class AwtCadPanelImageGoldenTest {

    companion object {
        const val WIDTH = 1200
        const val HEIGHT = 900

        /** 差分ピクセル (チャンネル差 > 8) の許容割合。初回測定: 同一環境の再描画 diff = 0 */
        const val MAX_DIFF_RATIO = 0.005

        /** 「差分ピクセル」判定: RGB いずれかのチャンネル差がこれを超えたら差分 (AA の微妙な端は無視) */
        const val CHANNEL_TOLERANCE = 8

        private val datePattern = Regex("""\d+ 年 \d+ 月 \d+ 日""")

        init {
            System.setProperty("java.awt.headless", "true")
        }
    }

    /** WebDrawingExportGoldenTest.defaultFixtureCsv と同一 (golden DXF と同じ三角形リスト) */
    private val defaultFixtureCsv = """
        市道○○号線 舗装打換工事
        市道○○号線
        ○○建設株式会社
        1/1
        1,10,2,10,-1,-1
        2,10,1,10,1,2
    """.trimIndent() + "\n"

    /** web/src/main.ts:34-45 の SAMPLE_CSV と同一 (7 三角形の実戦形) */
    private val sampleCsv = """
        テスト工事
        適当路線
        適当業者
        T-001
        1,6.0,5.0,4.0,-1,-1
        2,5.0,4.0,3.0,1,1
        3,4.0,3.5,3.0,1,2
        4,3.5,3.0,3.2,2,1
        5,3.0,2.8,3.0,3,2
        6,3.0,2.5,2.5,4,2
        7,2.8,2.2,2.5,5,1
    """.trimIndent() + "\n"

    /** :desktop:test の user.dir は desktop/ なので、settings.gradle.kts の在処 = repo root まで遡る */
    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile
        }
        error("repo root not found (searched up from ${System.getProperty("user.dir")})")
    }

    private fun goldenFile(name: String): File =
        File(repoRoot(), "desktop/src/test/resources/golden-img/$name")

    private fun renderToImage(csv: String): BufferedImage {
        val dxf = WebDrawingExport.buildDxfText(csv)
            .replace(datePattern, "#### 年 ## 月 ## 日")
        // writer は値が空欄の図枠セルで空 TEXT を出すが、AWT の TextLayout は空文字で
        // IllegalArgumentException を投げる。描画対象外 (見た目に出ない) なので除外する
        val parsed = DxfParser().parse(dxf).let { r ->
            r.copy(texts = r.texts.filter { it.text.isNotEmpty() })
        }
        val panel = AwtCadPanel()
        panel.setBounds(0, 0, WIDTH, HEIGHT) // fitToView が width/height を使うので paint 前に必須
        panel.setParseResult(parsed)
        val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        try {
            // paintComponent は protected (クラス final) なので public の paint() 経由で描く
            panel.paint(g)
        } finally {
            g.dispose()
        }
        return image
    }

    /** チャンネル差 > CHANNEL_TOLERANCE のピクセル割合 */
    private fun diffRatio(a: BufferedImage, b: BufferedImage): Double {
        require(a.width == b.width && a.height == b.height) { "サイズ不一致" }
        var diff = 0L
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                val pa = a.getRGB(x, y)
                val pb = b.getRGB(x, y)
                if (pa == pb) continue
                val dr = Math.abs(((pa shr 16) and 0xFF) - ((pb shr 16) and 0xFF))
                val dg = Math.abs(((pa shr 8) and 0xFF) - ((pb shr 8) and 0xFF))
                val db = Math.abs((pa and 0xFF) - (pb and 0xFF))
                if (dr > CHANNEL_TOLERANCE || dg > CHANNEL_TOLERANCE || db > CHANNEL_TOLERANCE) diff++
            }
        }
        return diff.toDouble() / (a.width.toLong() * a.height)
    }

    private fun assertImageGolden(goldenName: String, csv: String) {
        val actual = renderToImage(csv)
        val golden = goldenFile(goldenName)
        if (!golden.exists()) {
            golden.parentFile.mkdirs()
            ImageIO.write(actual, "png", golden)
            fail("golden 初回記録: ${golden.absolutePath} に書いた。PNG を目視批評してから再実行しろ")
        }
        val expected = ImageIO.read(golden)
        val ratio = diffRatio(expected, actual)
        println("image golden diff ($goldenName): ${"%.6f".format(ratio)} (許容 $MAX_DIFF_RATIO)")
        if (ratio > MAX_DIFF_RATIO) {
            // 診断用に実描画を build 配下へ残す (golden は触らない)
            val dump = File(repoRoot(), "desktop/build/golden-img-actual/$goldenName")
            dump.parentFile.mkdirs()
            ImageIO.write(actual, "png", dump)
            fail(
                "描画が golden と不一致 ($goldenName): 差分ピクセル割合 ${"%.6f".format(ratio)} > $MAX_DIFF_RATIO。" +
                    "実描画を ${dump.absolutePath} に残した"
            )
        }
    }

    @Test
    fun imageDefaultFixtureMatchesGolden() {
        assertImageGolden("awt-default.png", defaultFixtureCsv)
    }

    @Test
    fun imageSampleCsvMatchesGolden() {
        assertImageGolden("awt-sample.png", sampleCsv)
    }

    /** 同一 JVM 内の再描画が決定的であること (閾値の根拠の常時検証) */
    @Test
    fun renderingIsDeterministicInSameJvm() {
        val first = renderToImage(sampleCsv)
        val second = renderToImage(sampleCsv)
        val ratio = diffRatio(first, second)
        println("same-JVM re-render diff: ${"%.6f".format(ratio)}")
        assertTrue("同一 JVM の再描画で diff $ratio — 描画が非決定的", ratio == 0.0)
    }

    private fun renderToImageSfc(csv: String): BufferedImage {
        val sfc = WebDrawingExport.buildSfcText(csv, "awt-sample.sfc")
            .replace(datePattern, "#### 年 ## 月 ## 日")
        val parsed = SfcParser().parse(sfc).let { r ->
            r.copy(texts = r.texts.filter { it.text.isNotEmpty() })
        }
        val panel = AwtCadPanel()
        panel.setBounds(0, 0, WIDTH, HEIGHT) // fitToView が width/height を使うので paint 前に必須
        panel.setParseResult(parsed)
        val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        try {
            panel.paint(g)
        } finally {
            g.dispose()
        }
        return image
    }

    private fun assertImageGoldenSfc(goldenName: String, csv: String) {
        val actual = renderToImageSfc(csv)
        val golden = goldenFile(goldenName)
        if (!golden.exists()) {
            golden.parentFile.mkdirs()
            ImageIO.write(actual, "png", golden)
            fail("golden 初回記録 (SFC): ${golden.absolutePath} に書いた。PNG を目視批評してから再実行しろ")
        }
        val expected = ImageIO.read(golden)
        val ratio = diffRatio(expected, actual)
        println("image golden diff SFC ($goldenName): ${"%.6f".format(ratio)} (許容 $MAX_DIFF_RATIO)")
        if (ratio > MAX_DIFF_RATIO) {
            val dump = File(repoRoot(), "desktop/build/golden-img-actual/$goldenName")
            dump.parentFile.mkdirs()
            ImageIO.write(actual, "png", dump)
            fail(
                "SFC 描画が golden と不一致 ($goldenName): 差分ピクセル割合 ${"%.6f".format(ratio)} > $MAX_DIFF_RATIO。" +
                    "実描画を ${dump.absolutePath} に残した"
            )
        }
    }

    @Test
    fun imageSampleCsvSfcMatchesGolden() {
        assertImageGoldenSfc("awt-sample-sfc.png", sampleCsv)
    }
}
