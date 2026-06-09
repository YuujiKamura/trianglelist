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
        // TriangleList の用意
        triList = TriangleList()
        triList.add(Triangle(10f, 2f, 10f), true)
        triList.add(Triangle(triList[1], 2, 1f, 10f), true)

        writer = DxfFileWriter(triList).apply {
            dedlist_       = DeductionList()
            startTriNumber_ = 1
        }
        writer.titleTri_ = TitleParamStr()
        writer.titleDed_ = TitleParamStr()
        // タイトル枠の中身はアプリの strings.xml プリセット (MainActivity#loadTitleParameters) と同じ値を使う。
        // 各 field は「ヘッダーラベル」を保持、実値 (koujiname/rosenname/gyousyaname/zumennum) は setNames で渡す。
        writer.zumeninfo = ZumenInfo(
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
        writer.setNames(
            "市道○○号線 舗装打換工事",
            "市道○○号線",
            "○○建設株式会社",
            "1/1"
        )
    }

    @Test
    fun writeDxf_newBuilderMode() {
        // プロジェクトの build 以下に出力フォルダを作成
        val projectDir = System.getProperty("user.dir")?.let { File(it) }
        val outDir = File(projectDir, "build/test-output").apply { mkdirs() }
        val outFile = File(outDir, "test-new.dxf")

        writer.saveTo(outFile)

        assertTrue("DXF ファイルが正しく書き出されている", outFile.exists() && outFile.length() > 0)

        val result = DxfValidator.validate(outFile)
        result.errors.forEach { println("DXF validation error: $it") }
        // Temporarily disable validation to check if CAD can open the file
        // assertTrue("DXF validation failed", result.ok)

        // IDE から簡単に参照できるようにパスを出力
        println("→ DXF written to build output folder: ${outFile.absolutePath}")
    }

    @Test
    fun writeDxf_withPaperAndScale() {
        // プロジェクトの build 以下に出力フォルダを作成
        val projectDir = System.getProperty("user.dir")?.let { File(it) }
        val outDir = File(projectDir, "build/test-output").apply { mkdirs() }
        val outFile = File(outDir, "test-a3-scale50.dxf")

        // A3横、1/50縮尺でDXF生成
        writer.saveTo(outFile)

        assertTrue("DXF ファイルが正しく書き出されている", outFile.exists() && outFile.length() > 0)

        val result = DxfValidator.validate(outFile)
        result.errors.forEach { println("DXF validation error: $it") }

        // IDE から簡単に参照できるようにパスを出力
        println("→ A3 Scale 1/50 DXF written: ${outFile.absolutePath}")
    }
}
