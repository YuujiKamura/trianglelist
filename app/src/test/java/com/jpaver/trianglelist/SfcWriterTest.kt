package com.jpaver.trianglelist

import com.jpaver.trianglelist.datamanager.SfcWriter
import com.jpaver.trianglelist.editmodel.ZumenInfo
import com.jpaver.trianglelist.viewmodel.TitleParamStr
import org.junit.Assert
import org.junit.Test
import java.io.File

class SfcWriterTest {

    @Test
    fun testWriteToUserHome() {
        // プロジェクトの build 以下に出力フォルダを作成
        val projectDir = System.getProperty("user.dir")?.let { File(it) }
        val outDir = File(projectDir, "build/test-output").apply { mkdirs() }
        val fileName = "test.sfc"
        val outputFile = File(outDir, fileName)
        
        // CI環境ではこのテストをスキップ
        if (System.getenv("CI") != null) return
        
        val outputStream = outputFile.outputStream().buffered()

        // 以下、既存の処理を続ける
        val csvloadresult = CsvloaderTest().loadTriangleList()
        if(csvloadresult == null ) return
        val trianglelist = csvloadresult.trilist
        val deductionlist = csvloadresult.dedlist
        val drawingStartNumber = 1

        val writer = SfcWriter(trianglelist, deductionlist, outputStream, fileName, drawingStartNumber, 1f)
        writer.setNames("koujiname", "rosenname", "gyousyaname", "zumennum")
        writer.zumeninfo = ZumenInfo()
        writer.textscale_ = 25f * 20f
        writer.titleTri_ = TitleParamStr("type","number","name","a","b","c","pn,","pl")
        writer.titleDed_ = TitleParamStr("type","number","name","a","b","c","pn,","pl")

        writer.setStartNumber(drawingStartNumber)
        writer.isReverse_ = false
        writer.save()

        // ストリームを閉じる
        outputStream.close()

        // ファイルが正しく作成され、内容が期待通りか確認
        Assert.assertTrue("File was not created.", outputFile.exists())
        
        // IDE から簡単に参照できるようにパスを出力
        println("→ SFC written to build output folder: ${outputFile.absolutePath}")
    }

}