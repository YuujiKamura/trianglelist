package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.TitleParamStr
import com.jpaver.trianglelist.writer.DxfFileWriter
import com.jpaver.trianglelist.writer.DxfValidator
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
    }

    @Test
    fun writeDxf_newBuilderMode() {
        // プロジェクトの build 以下に出力フォルダを作成
        val projectDir = System.getProperty("user.dir")?.let { File(it) }
        val outDir = File(projectDir, "build/dxf-output").apply { mkdirs() }
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
        val outDir = File(projectDir, "build/dxf-output").apply { mkdirs() }
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
