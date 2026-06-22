package com.jpaver.trianglelist

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.SfcWriter
import com.jpaver.trianglelist.datamanager.saveTo
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.ZumenInfo
import com.jpaver.trianglelist.viewmodel.TitleParamStr
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.Charset

/**
 * ADR 0003 Phase 2b の golden diff テスト (Phase 2a DxfDimensionLayoutGoldenTest の SFC 版)。
 *
 * SfcWriter の寸法座標の出所を Triangle のキャッシュ (dimpoint / dimOnPath / pathS) から
 * common の DimensionLayout (gapPaperMm=0) に切り替えても、出力 SFC (SXF テキスト) が
 * 切替前と意味的に一致することを固定する。
 *
 * SFC 固有の事情:
 * - 寸法値テキストの垂直アライメントは SfcWriter.alignVByVector (2=下/8=上) で計算され、
 *   今回の切替対象外 (座標の出所のみを式に替える)。よって DXF の verticalDxf 揺れ (73) は
 *   SFC には現れない。
 * - 出力は BufferedOutputStream に SJIS (CP932) で書かれる。読み戻しも SJIS。
 *
 * 比較は Phase 2a と同じ意味的 diff:
 * - 行数と非数値 token は完全一致 (構造差・テキスト差は 1 件でも fail)
 * - 数値 token は abs 差 <= 1e-3 (出力単位 = model mm) を許容。旧経路の dimpoint キャッシュは
 *   move() 累積加算を経た値で、式のクリーン再計算と float 末桁が一致し得ないため。
 * - 図枠の作成日 (LocalDate.now() 由来) だけ #DATE# に正規化する。
 */
class SfcDimensionLayoutGoldenTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val charset = Charset.forName("SJIS")
    private val datePattern = Regex("""\d+ 年 \d+ 月 \d+ 日""")

    // ---- fixtures (DxfDimensionLayoutGoldenTest と同一形状) ----

    /** 2 三角形 (C 接続) の既定 fixture */
    private fun defaultTriList(): TriangleList {
        val tList = TriangleList()
        tList.add(Triangle(10f, 2f, 10f), true)
        tList.add(Triangle(tList[1], 2, 1f, 10f), true)
        return tList
    }

    /**
     * 寸法配置のバリエーション fixture:
     * - #1 独立 (mynumber==1 なので A 寸法も書かれる) + B=INRIGHT / C=INLEFT + 測点名
     * - #2 親1 の B 辺接続 + B=OUTERRIGHT(旗揚げ右) + 測点名
     * - #3 親1 の C 辺接続 + C=OUTERLEFT(旗揚げ左) + vertical=3(内側) の辺
     */
    private fun dimVariantsTriList(): TriangleList {
        val tList = TriangleList()
        tList.add(Triangle(8f, 6f, 7f), true)
        tList.add(Triangle(tList[1], 1, 5f, 6f), true)
        tList.add(Triangle(tList[1], 2, 6f, 5f), true)

        tList[1].setDimAligns(0, 1, 2, 1, 1, 3)
        tList[1].setDimPath(0.05f)
        tList[1].setDimPoint()
        tList[2].setDimAligns(0, 3, 0, 1, 1, 1)
        tList[2].setDimPath(0.05f)
        tList[2].setDimPoint()
        tList[3].setDimAligns(0, 0, 4, 3, 1, 1)
        tList[3].setDimPath(0.05f)
        tList[3].setDimPoint()

        tList[1].name = "No.0"
        tList[1].setDimPath(0.05f)
        tList[2].name = "No.1"
        tList[2].setDimPath(0.05f)
        return tList
    }

    private fun newZumenInfo() = ZumenInfo(
        zumentitle = "面 積 展 開 図",
        rosenname = "路線1",
        koujiname = "工 事 名",
        tDtype_ = "図 面 名",
        tDname_ = "路 線 名",
        tScale_ = "縮    尺",
        tNum_ = "図面番号",
        tDateHeader_ = "作 成 日",
        tDate_ = "    年  月  日",
        tAname_ = "施 工 者",
        menseki_ = "面積",
        mTitle_ = "面 積 計 算 書",
        mCname_ = "工事名：",
        mSyoukei_ = " 小計",
        mGoukei_ = "合計",
        tCredit_ = "http://trianglelist.home.blog"
    )

    /**
     * 控除 fixture: Circle 1 + Box 1。writeDeduction の旗線・円・矩形・情報テキストの
     * 各パスを通す (DxfDimensionLayoutGoldenTest.dedFixture と同一座標)。
     */
    private fun dedFixture(): DeductionList {
        val d = DeductionList()
        d.add(Deduction(1, "仕切弁", 0.23f, 0f, 0, "Circle", 0f, PointXY(5f, 5f), PointXY(7f, 6f)))
        d.add(Deduction(2, "桝", 0.5f, 0.5f, 0, "Box", 0f, PointXY(4f, 4f), PointXY(2f, 3f)))
        return d
    }

    private fun writeSfcString(tList: TriangleList, dedlist: DeductionList = DeductionList()): String {
        val outFile = tempFolder.newFile("out.sfc")
        val os = outFile.outputStream().buffered()
        val w = SfcWriter(tList, dedlist, "test.sfc", 1, 1f)
        w.titleTri_ = TitleParamStr()
        w.titleDed_ = TitleParamStr()
        w.zumeninfo = newZumenInfo()
        // textscale は内部値 (getPrintTextScale) を使う。単位モデル統一後は DXF と同じ実寸基準で、
        // 旧来の 500 (mm 直値) 上書きは primitive の ×unitscale と二重になるため外す。
        w.setNames("市道○○号線 舗装打換工事", "市道○○号線", "○○建設株式会社", "1/1")
        w.setStartNumber(1)
        w.isReverse_ = false
        w.saveTo(os)
        os.close()
        return outFile.readText(charset)
    }

    // ---- golden 機構 (Phase 2a と同一) ----

    private fun goldenDir(): File {
        val projectDir = File(System.getProperty("user.dir"))
        return File(projectDir, "src/test/resources/golden").apply { mkdirs() }
    }

    private fun normalize(s: String): String = s.replace(datePattern, "#DATE#")

    private val numericTolerance = 1e-3 + 1e-9

    /**
     * SFC (SXF) はカンマ+引用符区切りの 1 行形式 (`text_string_feature('1','8','1',' 6.0',...)`)。
     * 空白だけで割ると座標値が `6.0','9192.903','7933.329'` のように癒着して数値判定が効かないため、
     * 引用符・カンマ・括弧・空白すべてを区切りとして token を切り出す (空 token は捨てる)。
     */
    private fun tokenize(line: String): List<String> =
        line.split(Regex("[\\s,'()]+")).filter { it.isNotEmpty() }

    private fun isSemanticallyEqualLine(expected: String, actual: String): Boolean {
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

    private fun assertGolden(goldenName: String, actual: String) {
        val goldenFile = File(goldenDir(), goldenName)
        if (!goldenFile.exists()) {
            goldenFile.writeText(actual, charset)
            fail(
                "golden が無かったので現挙動から採取した: ${goldenFile.absolutePath}\n" +
                    "再実行して一致を確認すること (このまま green にはしない)"
            )
        }
        val expectedLines = normalize(goldenFile.readText(charset)).lines()
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
            if (e != null && g != null && isSemanticallyEqualLine(e, g)) {
                toleratedCount++
                continue
            }
            if (++realDiffCount <= 30) realDiffs.append("L${i + 1}: golden=[$e] actual=[$g]\n")
        }

        println("golden semantic diff ($goldenName): 数値末桁差 (許容) $toleratedCount 行 / 実差分 $realDiffCount 行")
        if (realDiffCount > 0) {
            fail(
                "golden と意味的に不一致 ($goldenName): 実差分 $realDiffCount 行 " +
                    "(数値末桁差 $toleratedCount 行は許容済み):\n$realDiffs"
            )
        }
    }

    // ---- tests ----

    @Test
    fun goldenDefaultFixture() {
        assertGolden("sfc-golden-default.sfc", writeSfcString(defaultTriList()))
    }

    @Test
    fun goldenDimVariantsFixture() {
        assertGolden("sfc-golden-dimvariants.sfc", writeSfcString(dimVariantsTriList()))
    }

    @Test
    fun goldenDeductionsFixture() {
        assertGolden("sfc-golden-deductions.sfc", writeSfcString(defaultTriList(), dedFixture()))
    }
}
