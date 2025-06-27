import com.jpaver.trianglelist.datamanager.FileUtil
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.nio.charset.Charset

class FileUtilTest {

    private val testDirectoryName = "testFiles"
    private val fileName = "testFile.txt"
    private val testContent = "Hello, BufferedWriter in User Home Test!!"
    private val testPath = "$testDirectoryName${File.separator}$fileName"
    private val userHome = System.getProperty("user.home")
    private val testFile = File(userHome, testPath)

    @Before
    @Ignore("This test is only for local development.")
    fun setUp() {
        // テスト用のディレクトリとファイルが既に存在する場合は削除
        testFile.parentFile?.deleteRecursively()
    }

    @Test
    @Ignore("This test is only for local development.")
    fun testWriteToUserHome() {
        // テスト内容をユーザーのホームディレクトリに書き出し
        FileUtil.writeToUserHome(testContent, testPath, Charset.defaultCharset())

        // ファイルが正しく作成され、内容が期待通りか確認
        assertTrue("File was not created.", testFile.exists())
        assertTrue("Content does not match.", testFile.readText() == testContent)
    }

    @After
    @Ignore("This test is only for local development.")
    fun tearDown() {
        // テストで作成したファイルとディレクトリを削除してクリーンアップ
        //testFile.delete()
        //testDirectory.delete() // ディレクトリが空であることを確認してから削除
    }
}
