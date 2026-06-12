package com.jpaver.trianglelist

import com.jpaver.trianglelist.viewmodel.TitleParamStr
import com.jpaver.trianglelist.datamanager.DxfFileWriter
import com.jpaver.trianglelist.datamanager.DxfValidator
import com.jpaver.trianglelist.datamanager.saveTo
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.ZumenInfo
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DxfFileWriterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var triList: TriangleList
    private lateinit var writer: DxfFileWriter

    @Before
    fun setUp() {
        writer = newWriter(sideLength = 10f)
        triList = writer.trilist_
    }

    /**
     * 指定の最長辺長で TriangleList + DxfFileWriter を構築する。
     * 辺長を変えることで TriangleList.getPrintScale() が選ぶ縮尺 (1/50, 1/100, 1/200...)
     * が変わり、タイトル枠の text scale も追従する。
     */
    private fun newWriter(sideLength: Float): DxfFileWriter {
        val tList = TriangleList()
        // 三角形 2 つを連結。 setUp 当時の比率 (a=10, b=2, c=10) と
        // (親, 接続=2, b=1, c=10) を sideLength でスケール
        val s = sideLength / 10f
        tList.add(Triangle(10f * s, 2f * s, 10f * s), true)
        tList.add(Triangle(tList[1], 2, 1f * s, 10f * s), true)

        val w = DxfFileWriter(tList).apply {
            dedlist_       = DeductionList()
            startTriNumber_ = 1
        }
        w.titleTri_ = TitleParamStr()
        w.titleDed_ = TitleParamStr()
        // タイトル枠の中身はアプリの strings.xml プリセット (MainActivity#loadTitleParameters) と同じ値を使う。
        // 各 field は「ヘッダーラベル」を保持、実値 (koujiname/rosenname/gyousyaname/zumennum) は setNames で渡す。
        w.zumeninfo = ZumenInfo(
            zumentitle  = "面 積 展 開 図",
            rosenname   = "路線1",
            koujiname   = "工 事 名",
            tDtype_     = "図 面 名",
            tDname_     = "路 線 名",
            tScale_     = "縮    尺",
            tNum_       = "図面番号",
            tDateHeader_ = "作 成 日",
            tDate_      = "    年  月  日",
            tAname_     = "施 工 者",
            menseki_    = "面積",
            mTitle_     = "面 積 計 算 書",
            mCname_     = "工事名：",
            mSyoukei_   = " 小計",
            mGoukei_    = "合計",
            tCredit_    = "http://trianglelist.home.blog"
        )
        // 値 (アプリの実運用相当): 工事名 / 路線名 / 施工者 / 図面番号
        w.setNames(
            "市道○○号線 舗装打換工事",
            "市道○○号線",
            "○○建設株式会社",
            "1/1"
        )
        return w
    }

    private fun outputDir(): File {
        val projectDir = System.getProperty("user.dir")?.let { File(it) }
        return File(projectDir, "build/test-output").apply { mkdirs() }
    }

    private fun writeAndValidate(w: DxfFileWriter, name: String) {
        val outFile = File(outputDir(), name)
        w.saveTo(outFile)
        assertTrue("DXF ファイルが正しく書き出されている", outFile.exists() && outFile.length() > 0)
        val result = DxfValidator.validate(outFile)
        result.errors.forEach { println("DXF validation error: $it") }
        println("→ DXF written: ${outFile.absolutePath} (printscale_=${w.printscale_}, textscale_=${w.textscale_})")
    }

    @Test
    fun writeDxf_newBuilderMode() {
        writeAndValidate(writer, "test-new.dxf")
    }

    @Test
    fun writeDxf_withPaperAndScale() {
        writeAndValidate(writer, "test-a3-scale50.dxf")
    }

    // ---- 可変縮尺テスト ----
    // 辺長を変えると getPrintScale() が選ぶ縮尺が変わり、 タイトル枠の text scale も
    // それに追従する想定。 viewer (desktop モジュール) で 1 つずつ開いて目視確認する。

    @Test
    fun writeDxf_scale_small() {
        // 辺長 10m 前後 → 1/50 (default) 想定
        writeAndValidate(newWriter(sideLength = 10f), "test-scale-small.dxf")
    }

    @Test
    fun writeDxf_scale_medium() {
        // 辺長 50m 前後 → 1/100〜1/200 想定
        writeAndValidate(newWriter(sideLength = 50f), "test-scale-medium.dxf")
    }

    @Test
    fun writeDxf_scale_large() {
        // 辺長 200m 前後 → 1/500 程度の想定
        writeAndValidate(newWriter(sideLength = 200f), "test-scale-large.dxf")
    }

    // ---- user 実用域 fixtures (2026-06-10 追加) ----
    // user 発話「200分の1〜250くらいまでが、 実用域。 最大で500くらい」 を
    // viewer で目視照合するため、 1/200, 1/250, 1/500 周辺の fixture を出す。

    @Test
    fun writeDxf_scale_practical_200() {
        // 辺長 70m 前後 → 1/200 想定 (printscale_ ≈ 2.0)
        writeAndValidate(newWriter(sideLength = 70f), "test-scale-practical-200.dxf")
    }

    @Test
    fun writeDxf_scale_practical_250() {
        // 辺長 90m 前後 → 1/250 想定 (printscale_ ≈ 2.5)
        writeAndValidate(newWriter(sideLength = 90f), "test-scale-practical-250.dxf")
    }

    @Test
    fun writeDxf_scale_practical_500() {
        // 辺長 170m 前後 → 1/500 想定 (printscale_ ≈ 5.0)
        writeAndValidate(newWriter(sideLength = 170f), "test-scale-practical-500.dxf")
    }

    // ---- ハンドル一意性 (2026-06-12 追加) ----
    // ezdxf recover が "Non-unique entity handle #74" を警告していた件。
    // TablesBuilder/DxfTable の LAYER テーブルで C-COL-COL1 と C-TTL-FRAM が
    // 両方ハンドル 74 を持つコピペバグが原因 → C-TTL-FRAM を 75 に分離。
    // group code 5 (entity handle) は DXF 全体で一意でなければならない。

    @Test
    fun writeDxf_entityHandles_areUnique() {
        val w = newWriter(sideLength = 10f)
        val sb = StringBuilder()
        w.writer = sb
        w.save()
        // code/value を厳密に対で読み、group code 5 (実体ハンドル) の値を集める。
        // HEADER の $HANDSEED も group code 5 だが実体ハンドルではない (次に割り当てる
        // 値を指す変数) ので、TABLES 以降だけを対象にする。DXF は code/value が交互。
        val tokens = sb.toString().split("\n").map { it.trim() }
        val start = tokens.indexOfFirst { it == "0" }  // 最初の code でパリティを合わせる
        val handles = mutableListOf<String>()
        var started = false
        var i = start
        while (i + 1 < tokens.size) {
            val code = tokens[i]; val value = tokens[i + 1]
            if (code == "2" && value == "TABLES") started = true  // HEADER を抜けた
            if (started && code == "5") handles.add(value)
            i += 2
        }
        val dups = handles.groupingBy { it }.eachCount().filter { it.value > 1 }
        assertTrue("entity handle が重複している: $dups", dups.isEmpty())
    }

    // ---- ペーパー空間の縮尺 (2026-06-12 追加) ----
    // DXF のモデル空間は縮尺を持てないため、レイアウトの VIEWPORT (紙上高さ 41 /
    // モデル表示高さ 45 = 1/分母) と PLOTSETTINGS (142/143) で運ぶ。
    // CADWe'll 土木 11 等の「モデル・レイアウト・ビューポート・縮尺を保持して読む」CAD が対象。

    @Test
    fun writeDxf_paperSpaceViewport_carriesScale() {
        for (sideLength in listOf(10f, 70f)) {  // 1/50 と 1/200 (practical) の両域
            val w = newWriter(sideLength)
            val sb = StringBuilder()
            w.writer = sb
            w.save()
            val text = sb.toString()
            val den = w.printscale_ * 100f  // getPrintScale: 0.5→1/50, 2.0→1/200

            // メイン (id=1) + 内容 (id=2) の 2 枚が必須。id=1 が無いと CADWe'll が
            // 尺度付きビューポートシートを構築しない (2026-06-12 実機検証)
            val vpCount = Regex("\\nVIEWPORT\\n").findAll(text).count()
            assertTrue("VIEWPORT が 2 枚 (メイン+内容、1/${den.toInt()}) — 実際 $vpCount",
                vpCount == 2)
            assertTrue("メインビューポート id=1/status=1 がある (1/${den.toInt()})",
                text.contains("\n 68\n1\n 69\n1\n"))
            assertTrue("内容ビューポートのモデル表示高さ 45 = 297×分母 (1/${den.toInt()})",
                text.contains("\n 45\n${297f * den}\n"))
            assertTrue("PLOTSETTINGS の印刷縮尺分母 143 = ${den} (1/${den.toInt()})",
                text.contains("\n143\n$den\n"))
        }
    }
}
