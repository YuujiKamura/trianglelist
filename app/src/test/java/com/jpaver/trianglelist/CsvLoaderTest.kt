package com.jpaver.trianglelist
import com.jpaver.trianglelist.datamanager.CsvLoader
import com.jpaver.trianglelist.datamanager.ReturnValues
import com.jpaver.trianglelist.editmodel.TriangleList
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.Test
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class CsvloaderTest {

    fun loadTriangleList(): ReturnValues?{
        val FILENAME = "4.11.csv"
        val path = "src/test/resources/$FILENAME"
        val charset = StandardCharsets.UTF_8
        val reader =  BufferedReader(InputStreamReader(FileInputStream(path), charset))
        val showToast = mockk<(String) -> Unit>()
        val addTriangle = mockk<(TriangleList, List<String?>, com.example.trilib.PointXY, Float) -> Unit>()
        val setAllTextSize = mockk<(Float) -> Unit>()
        val typeToInt = mockk<(String) -> Int>()
        val csvLoader = CsvLoader()

        // モックの動作設定
        every { showToast(any()) } just Runs
        every { addTriangle(any(), any(), any(), any()) } just Runs
        every { setAllTextSize(any()) } just Runs
        every { typeToInt(any()) } returns 1

        // 関数のテスト実行
        val result: ReturnValues? = csvLoader.parseCSV(reader, showToast, setAllTextSize, typeToInt, 1.0f)

        if(result!=null) result.trilist.recoverState()

        // ファイルをクローズ
        reader.close()
        return result
    }

    @Test
    fun `parseCSV correctly parses valid input from file`() {

        val result = loadTriangleList()

        // 検証
        if(result != null){
            result.trilist.arrangePointNumbers()
            print_trilist(result.trilist)
        }
    }
}
