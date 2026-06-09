package com.jpaver.trianglelist

import com.jpaver.trianglelist.viewmodel.TitleParamStr
import com.jpaver.trianglelist.datamanager.DxfFileWriter
import com.jpaver.trianglelist.datamanager.DxfValidator
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
}
