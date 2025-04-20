package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.TitleParamStr
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class DxfFileWriterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var triList: TriangleList
    private lateinit var writer: DxfFileWriter

    @Before
    fun setUp() {
        // TriangleList の用意
        triList = TriangleList()
        triList.add(Triangle(20f, 2f, 20f), true)
        triList.add(Triangle(triList[1], 2, 1f, 20f), true)

        writer = DxfFileWriter(triList).apply {
            dedlist_       = DeductionList()
            startTriNumber_ = 1
            // ← ここで必ず route/title 文字列をセットする
            rStr_ = ResStr()     // frame に描くルート文字列
        }
        writer.titleTri_ = TitleParamStr()
        writer.titleDed_ = TitleParamStr()
    }

    @Test
    fun writeDxf_toBuildOutputFolder() {
        // プロジェクトの build 以下に出力フォルダを作成
        val projectDir = System.getProperty("user.dir")?.let { File(it) }
        val outDir = File(projectDir, "build/dxf-output").apply { mkdirs() }
        val outFile = File(outDir, "test.dxf")

        writer.writer = BufferedWriter(FileWriter(outFile))
        writer.save()
        writer.writer.flush()
        writer.writer.close()

        assertTrue("DXF ファイルが正しく書き出されている", outFile.exists() && outFile.length() > 0)

        // IDE から簡単に参照できるようにパスを出力
        println("→ DXF written to build output folder: ${outFile.absolutePath}")
    }
}
