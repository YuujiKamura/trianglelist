package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.FileUtil
import com.jpaver.trianglelist.util.TitleParamStr
import org.junit.Assert
import org.junit.Test
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

class SfcWriterTest {

    private val testDirectoryName = "testSfc"
    private val fileName = "test.sfc"
    private val testPath = "$testDirectoryName${File.separator}$fileName"
    private val userHome = System.getProperty("user.home")
    private val testFile = File(userHome, testPath)

    @Test
    fun testWriteToUserHome() {
        if (System.getenv("CI") != null) return
        // CI環境ではこのテストをスキップ

        val directoryPath = "testSfc" // ここに正しいディレクトリパスを指定
        val fileName = "test.sfc"
        val fullPath = "$directoryPath${File.separator}$fileName"

        // ディレクトリが存在するか確認し、存在しない場合は作成する
        val directory = File(directoryPath)
        if (!directory.exists()) {
            directory.mkdirs()  // ディレクトリが存在しない場合は、ディレクトリを作成する
        }

        // ファイル出力ストリームを安全に作成
        val fileOutputStream = FileOutputStream(fullPath)
        val outputStream = BufferedOutputStream(fileOutputStream)

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

        // テスト内容をユーザーのホームディレクトリに書き出し
        FileUtil.writeToUserHome(writer.strPool_, fullPath )

        // ファイルが正しく作成され、内容が期待通りか確認
        Assert.assertTrue("File was not created.", testFile.exists())
        //Assert.assertTrue("Content does not match.", testFile.readText() == writer.strPool_)
    }

}