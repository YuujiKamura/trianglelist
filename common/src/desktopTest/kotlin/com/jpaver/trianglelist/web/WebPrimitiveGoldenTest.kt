package com.jpaver.trianglelist.web

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * 見た目の冪等性 golden 層1 (task #13): primitives JSON の厳密固定。
 *
 * WebPrimitiveRenderer.renderCsv (CSV → line/text/circle の JSON プリミティブ) の出力を
 * golden 文字列と完全一致で比較する。線・寸法値座標・番号サークル位置・角度・align が
 * 全部 JSON に出るので、大手術 (editmodel/writer/placement の変更) が見た目の座標を
 * 1 桁でも動かせば即 fail する。フォント非依存・プラットフォーム非依存 (commonMain のみ) で
 * 層2 (desktop AWT 画像 golden) より堅牢、CI 向きはこちら。
 *
 * fixture は 2 種:
 * - default: app golden (dxf-golden-default.dxf) と同じ三角形リストの最小形式 CSV
 *   (WebDrawingExportGoldenTest.defaultFixtureCsv と同一)
 * - sample: web シェルの内蔵サンプル (web/src/main.ts SAMPLE_CSV、7 三角形) —
 *   B/C 接続・再接続が混ざる実戦形
 *
 * golden が無い初回実行は記録して fail する (記録内容を確認して再実行で green)。
 * 更新したいとき (意図した見た目変更) は golden ファイルを消して再実行。
 */
class WebPrimitiveGoldenTest {

    /** WebDrawingExportGoldenTest.defaultFixtureCsv と同一 (golden DXF と同じ三角形リスト) */
    private val defaultFixtureCsv = """
        市道○○号線 舗装打換工事
        市道○○号線
        ○○建設株式会社
        1/1
        1,10,2,10,-1,-1
        2,10,1,10,1,2
    """.trimIndent() + "\n"

    /** web/src/main.ts:34-45 の SAMPLE_CSV と同一 */
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

    /** :common:desktopTest の user.dir は common/ なので、settings.gradle.kts の在処 = repo root まで遡る */
    private fun goldenFile(name: String): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists()) {
                return File(dir, "common/src/desktopTest/resources/golden/$name")
            }
            dir = dir.parentFile
        }
        error("repo root not found (searched up from ${System.getProperty("user.dir")})")
    }

    private fun assertPrimitivesGolden(goldenName: String, csv: String) {
        val actual = WebPrimitiveRenderer.renderCsv(csv, 1.0f)
        val golden = goldenFile(goldenName)
        if (!golden.exists()) {
            golden.parentFile.mkdirs()
            golden.writeText(actual, Charsets.UTF_8)
            fail("golden 初回記録: ${golden.absolutePath} に書いた。内容を確認して再実行しろ")
        }
        val expected = golden.readText(Charsets.UTF_8)
        if (expected != actual) {
            // 1 行 JSON なので、最初の食い違い位置の前後を切り出して見せる
            val firstDiff = expected.indices.firstOrNull { it >= actual.length || expected[it] != actual[it] }
                ?: expected.length
            val from = (firstDiff - 60).coerceAtLeast(0)
            fail(
                "primitives JSON が golden と不一致 ($goldenName) — 見た目の座標が動いた。" +
                    "index=$firstDiff\n" +
                    "golden: …${expected.substring(from, (firstDiff + 60).coerceAtMost(expected.length))}…\n" +
                    "actual: …${actual.substring(from, (firstDiff + 60).coerceAtMost(actual.length))}…"
            )
        }
    }

    @Test
    fun primitivesDefaultFixtureMatchesGolden() {
        assertPrimitivesGolden("primitives-default.json", defaultFixtureCsv)
    }

    @Test
    fun primitivesSampleCsvMatchesGolden() {
        assertPrimitivesGolden("primitives-sample.json", sampleCsv)
    }
}
