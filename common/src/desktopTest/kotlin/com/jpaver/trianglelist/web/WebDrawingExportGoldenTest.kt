package com.jpaver.trianglelist.web

import java.io.File
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.fail

/**
 * Web 段階2b (task #10) の golden 同値テスト。
 *
 * web 経路 (CSV 文字列 → WebCsvReader → WebDrawingExport.buildDxfText/buildSfcText) の出力が、
 * app の golden fixture (app/src/test/resources/golden/、DxfDimensionLayoutGoldenTest /
 * SfcDimensionLayoutGoldenTest が固定したもの) と日付以外一致することを assert する。
 * スマホアプリと Web で同じ図面ファイルが出る = 部品が本当に共通である証明。
 *
 * 入力 CSV は golden の defaultTriList() fixture
 * (Triangle(10,2,10) + Triangle(tList[1], 2, 1f, 10f)) の最小形式 CSV 表現。
 * 経路差: golden は Triangle(parent, pbc, B, C) コンストラクタ、CSV は
 * ConnParam(side, type=0, lcr=2, lenA) 経由 (TriangleSetters.kt:109)。この同値性こそが
 * 本テストの検証対象。
 *
 * dimvariants fixture は対象外: setDimAligns の値 (完全形式 CSV の 11-16 列) は
 * WebCsvReader の最小形式では表現できない (完全形式読みは未実装、段階2b スコープ外)。
 *
 * 比較器は app の golden テストと同じ意味的 diff:
 * - 行数と非数値 token は完全一致
 * - 数値 token は abs 差 <= 1e-3 を許容 (float 経路差・プラットフォーム文字列化差を吸収)
 * - 図枠の作成日 (実行日で揺れる) は #DATE# に正規化
 * - DXF は空白区切り token、SFC はカンマ/引用符/括弧も区切り (app 側テストと同一)
 */
class WebDrawingExportGoldenTest {

    private val sjis = Charset.forName("Shift_JIS")
    private val datePattern = Regex("""\d+ 年 \d+ 月 \d+ 日""")
    private val numericTolerance = 1e-3 + 1e-9

    /** DxfDimensionLayoutGoldenTest.defaultTriList + setNames と同じ内容の最小形式 CSV */
    private val defaultFixtureCsv = """
        市道○○号線 舗装打換工事
        市道○○号線
        ○○建設株式会社
        1/1
        1,10,2,10,-1,-1
        2,10,1,10,1,2
    """.trimIndent() + "\n"

    // ---- golden 機構 ----

    /** :common:desktopTest の user.dir は common/ なので、repo root まで遡って app の golden を引く */
    private fun goldenFile(name: String): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val f = File(dir, "app/src/test/resources/golden/$name")
            if (f.exists()) return f
            dir = dir.parentFile
        }
        error("golden fixture not found: $name (searched up from ${System.getProperty("user.dir")})")
    }

    private fun normalize(s: String): String = s.replace(datePattern, "#DATE#")

    private fun isSemanticallyEqualLine(expected: String, actual: String, tokenize: (String) -> List<String>): Boolean {
        if (expected == actual) return true
        val e = tokenize(expected)
        val a = tokenize(actual)
        if (e.size != a.size) return false
        for (i in e.indices) {
            if (e[i] == a[i]) continue
            val en = e[i].toDoubleOrNull() ?: return false
            val an = a[i].toDoubleOrNull() ?: return false
            if (kotlin.math.abs(en - an) > numericTolerance) return false
        }
        return true
    }

    private fun assertGolden(goldenName: String, actual: String, tokenize: (String) -> List<String>) {
        val expectedLines = normalize(goldenFile(goldenName).readText(sjis)).lines()
        val gotLines = normalize(actual).lines()

        val realDiffs = StringBuilder()
        var realDiffCount = 0
        var toleratedCount = 0

        if (expectedLines.size != gotLines.size) {
            realDiffs.append("行数差: golden=${expectedLines.size} actual=${gotLines.size}\n")
            realDiffCount++
        }
        for (i in 0 until maxOf(expectedLines.size, gotLines.size)) {
            val e = expectedLines.getOrNull(i)
            val g = gotLines.getOrNull(i)
            if (e == g) continue
            if (e != null && g != null && isSemanticallyEqualLine(e, g, tokenize)) {
                toleratedCount++
                continue
            }
            if (++realDiffCount <= 30) realDiffs.append("L${i + 1}: golden=[$e] actual=[$g]\n")
        }

        println("golden semantic diff ($goldenName): 数値末桁差 (許容) $toleratedCount 行 / 実差分 $realDiffCount 行")
        if (realDiffCount > 0) {
            fail(
                "web 経路の出力が golden と意味的に不一致 ($goldenName): 実差分 $realDiffCount 行 " +
                    "(数値末桁差 $toleratedCount 行は許容済み):\n$realDiffs"
            )
        }
    }

    /** DXF: 空白区切り (DxfDimensionLayoutGoldenTest と同一) */
    private fun tokenizeDxf(line: String): List<String> =
        line.trim().split(Regex("\\s+"))

    /** SFC: 引用符・カンマ・括弧・空白すべて区切り (SfcDimensionLayoutGoldenTest と同一) */
    private fun tokenizeSfc(line: String): List<String> =
        line.split(Regex("[\\s,'()]+")).filter { it.isNotEmpty() }

    // ---- tests ----

    @Test
    fun dxfFromCsvMatchesAppGolden() {
        assertGolden(
            "dxf-golden-default.dxf",
            WebDrawingExport.buildDxfText(defaultFixtureCsv),
            ::tokenizeDxf,
        )
    }

    @Test
    fun sfcFromCsvMatchesAppGolden() {
        // golden は filename="test.sfc" で採取されている (SfcDimensionLayoutGoldenTest.writeSfcString)
        assertGolden(
            "sfc-golden-default.sfc",
            WebDrawingExport.buildSfcText(defaultFixtureCsv, "test.sfc"),
            ::tokenizeSfc,
        )
    }

    @Test
    fun parseHeaderReadsMinimalForm() {
        val h = WebDrawingExport.parseHeader(defaultFixtureCsv)
        kotlin.test.assertEquals("市道○○号線 舗装打換工事", h.koujiname)
        kotlin.test.assertEquals("市道○○号線", h.rosenname)
        kotlin.test.assertEquals("○○建設株式会社", h.gyousyaname)
        kotlin.test.assertEquals("1/1", h.zumennum)
    }

    @Test
    fun parseHeaderReadsLabeledForm() {
        val labeled = "koujiname,工事A\nrosenname,路線B\ngyousyaname,業者C\nzumennum,Z-1\n1,3,3,3,-1,-1\n"
        val h = WebDrawingExport.parseHeader(labeled)
        kotlin.test.assertEquals("工事A", h.koujiname)
        kotlin.test.assertEquals("路線B", h.rosenname)
        kotlin.test.assertEquals("業者C", h.gyousyaname)
        kotlin.test.assertEquals("Z-1", h.zumennum)
    }
}
