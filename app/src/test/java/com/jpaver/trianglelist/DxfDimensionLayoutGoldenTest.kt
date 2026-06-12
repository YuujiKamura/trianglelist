package com.jpaver.trianglelist

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.DxfFileWriter
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
 * ADR 0003 Phase 2a の golden diff テスト。
 *
 * DxfFileWriter の寸法座標の出所を Triangle のキャッシュ (dimpoint / dimOnPath) から
 * common の DimensionLayout (gapPaperMm=0) に切り替えても、出力 DXF が
 * 切替前 (commit 1742c96 時点の HEAD) と全文字列一致することを固定する。
 *
 * golden は src/test/resources/golden/ にコミット済み。存在しない場合は現挙動から
 * 採取して fail するので、再実行して一致を確認する (採取と照合を同じ実行で済ませない)。
 *
 * 比較は Shift_JIS で読んだ全文の意味的 diff (2026-06-10 orchestrator 判定基準):
 * - 行数と非数値 token は完全一致を要求 (構造差・テキスト差は 1 件でも fail)
 * - 数値 token は abs 差 <= 1e-3 (出力単位 = model mm。紙換算 0.02 micron 未満で CAD 的に無意味)
 *   なら一致扱い。バイト同一を要求しないのは、旧経路の Triangle.dimpoint キャッシュが
 *   move() の累積加算 (TriangleExtensions.kt:78-81) を経た値で、式のクリーン再計算とは
 *   float 末桁が一致し得ないため。
 * - 図枠の作成日 (LocalDate.now() 由来で実行日に揺れる) だけ #DATE# に正規化する。
 */
class DxfDimensionLayoutGoldenTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val charset = Charset.forName("Shift_JIS")
    private val datePattern = Regex("""\d+ 年 \d+ 月 \d+ 日""")

    // ---- fixtures ----

    /** DxfFileWriterTest と同じ 2 三角形 (C 接続) の既定 fixture */
    private fun defaultTriList(): TriangleList {
        val tList = TriangleList()
        tList.add(Triangle(10f, 2f, 10f), true)
        tList.add(Triangle(tList[1], 2, 1f, 10f), true)
        return tList
    }

    /**
     * 寸法配置のバリエーション fixture:
     * - #1 独立 (mynumber==1 なので A 寸法も書かれる) + B=INRIGHT(右寄せ) / C=INLEFT(左寄せ) + 測点名
     * - #2 親1 の B 辺接続 + B=OUTERRIGHT(旗揚げ右) + 測点名
     * - #3 親1 の C 辺接続 + C=OUTERLEFT(旗揚げ左) + vertical=3(内側) の辺
     */
    private fun dimVariantsTriList(): TriangleList {
        val tList = TriangleList()
        tList.add(Triangle(8f, 6f, 7f), true)
        tList.add(Triangle(tList[1], 1, 5f, 6f), true)
        tList.add(Triangle(tList[1], 2, 6f, 5f), true)

        // Dims.setAligns(sa..sc, ha..hc): 前半 3 つが horizontal、後半 3 つが vertical
        tList[1].setDimAligns(0, 1, 2, 1, 1, 3)
        tList[1].setDimPath()
        tList[1].setDimPoint()
        tList[2].setDimAligns(0, 3, 0, 1, 1, 1)
        tList[2].setDimPath()
        tList[2].setDimPoint()
        tList[3].setDimAligns(0, 0, 4, 3, 1, 1)
        tList[3].setDimPath()
        tList[3].setDimPoint()

        tList[1].name = "No.0"
        tList[2].name = "No.1"
        return tList
    }

    /**
     * 控除 fixture: Circle 1 + Box 1。writeDeduction の旗線・円・矩形・情報テキストの
     * 各パスを通す。point/pointFlag は三角形 fixture と同じ座標系 (m) で明示。
     */
    private fun dedFixture(): DeductionList {
        val d = DeductionList()
        d.add(Deduction(1, "仕切弁", 0.23f, 0f, 0, "Circle", 0f, PointXY(5f, 5f), PointXY(7f, 6f)))
        d.add(Deduction(2, "桝", 0.5f, 0.5f, 0, "Box", 0f, PointXY(4f, 4f), PointXY(2f, 3f)))
        return d
    }

    private fun newWriter(tList: TriangleList): DxfFileWriter {
        val w = DxfFileWriter(tList).apply {
            dedlist_ = DeductionList()
            startTriNumber_ = 1
        }
        w.titleTri_ = TitleParamStr()
        w.titleDed_ = TitleParamStr()
        w.zumeninfo = ZumenInfo(
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
        w.setNames("市道○○号線 舗装打換工事", "市道○○号線", "○○建設株式会社", "1/1")
        return w
    }

    // ---- golden 機構 ----

    private fun goldenDir(): File {
        val projectDir = File(System.getProperty("user.dir"))
        return File(projectDir, "src/test/resources/golden").apply { mkdirs() }
    }

    private fun writeDxfString(writer: DxfFileWriter): String {
        val outFile = tempFolder.newFile("out.dxf")
        writer.saveTo(outFile)
        return outFile.readText(charset)
    }

    private fun normalize(s: String): String = s.replace(datePattern, "#DATE#")

    /** 数値 token の許容差。10 進文字列→double 変換の誤差ぶんだけ僅かに緩める */
    private val numericTolerance = 1e-3 + 1e-9

    /**
     * 行の意味的一致判定。空白で token 分割し、
     * 数値 token は abs 差 <= 1e-3、非数値 token は完全一致。token 数が違えば不一致。
     */
    private fun isSemanticallyEqualLine(expected: String, actual: String): Boolean {
        if (expected == actual) return true
        val e = expected.trim().split(Regex("\\s+"))
        val a = actual.trim().split(Regex("\\s+"))
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
        assertGolden("dxf-golden-default.dxf", writeDxfString(newWriter(defaultTriList())))
    }

    @Test
    fun goldenDimVariantsFixture() {
        assertGolden("dxf-golden-dimvariants.dxf", writeDxfString(newWriter(dimVariantsTriList())))
    }

    @Test
    fun goldenDeductionsFixture() {
        val w = newWriter(defaultTriList())
        w.dedlist_ = dedFixture()
        assertGolden("dxf-golden-deductions.dxf", writeDxfString(w))
    }
}
