package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.FileUtil
import org.junit.Assert
import org.junit.Test
import java.io.File

class SfcWriterTest {

    private val testDirectoryName = "testSfc"
    private val fileName = "test.sfc"
    private val testContent = "Hello, BufferedWriter in User Home Test!!"
    private val testPath = "$testDirectoryName${File.separator}$fileName"
    private val userHome = System.getProperty("user.home")
    private val testFile = File(userHome, testPath)

    @Test
    fun testWriteToUserHome() {
        if (System.getenv("CI") != null) return
        // CI環境ではこのテストをスキップ

        // テスト用のディレクトリとファイルが既に存在する場合は削除
        testFile.parentFile?.deleteRecursively()

        // テスト内容をユーザーのホームディレクトリに書き出し
        FileUtil.writeToUserHome(testContent, testPath)

        // ファイルが正しく作成され、内容が期待通りか確認
        Assert.assertTrue("File was not created.", testFile.exists())
        Assert.assertTrue("Content does not match.", testFile.readText() == testContent)
    }

}