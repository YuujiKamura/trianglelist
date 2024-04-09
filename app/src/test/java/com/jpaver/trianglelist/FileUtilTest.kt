import com.jpaver.trianglelist.util.FileUtil
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File

class FileUtilTest {

    private val testDirectoryName = "testFiles"
    private val fileName = "testFile.txt"
    private val fileNameDxf = "testFile.dxf"
    private val testContent = "Hello, BufferedWriter in User Home Test!!"
    private val testPath = "$testDirectoryName${File.separator}$fileName"
    private val userHome = System.getProperty("user.home")
    private val testFile = File(userHome, testPath)

    @Before
    fun setUp() {
        // テスト用のディレクトリとファイルが既に存在する場合は削除
        testFile.parentFile?.deleteRecursively()
    }

    @Test
    @Ignore("This test is only for local development.")
    fun testWriteToUserHome() {
        // テスト内容をユーザーのホームディレクトリに書き出し
        FileUtil.writeToUserHome(testContent, testPath)

        // ファイルが正しく作成され、内容が期待通りか確認
        assertTrue("File was not created.", testFile.exists())
        assertTrue("Content does not match.", testFile.readText() == testContent)
    }

    @Test
    fun testDxfWriterToUserHome() {
        // テスト内容をユーザーのホームディレクトリに書き出し
        FileUtil.writeToUserHome(testContent, "$testDirectoryName${File.separator}$fileNameDxf")

    }

    @After
    fun tearDown() {
        // テストで作成したファイルとディレクトリを削除してクリーンアップ
        //testFile.delete()
        //testDirectory.delete() // ディレクトリが空であることを確認してから削除
    }
}
