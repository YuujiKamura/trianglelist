package com.jpaver.trianglelist
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.Test
import java.io.BufferedReader
import java.io.FileReader

class CsvloaderTest {

    @Test
    fun `parseCSV correctly parses valid input from file`() {
        val FILENAME = "4.11.csv"
        val path = "src/test/resources/$FILENAME"
        val fileReader = FileReader(path)
        val reader = BufferedReader(fileReader)
        val showToast = mockk<(String) -> Unit>()
        val addTriangle = mockk<(TriangleList, List<String?>, PointXY, Float) -> Unit>()
        val setAllTextSize = mockk<(Float) -> Unit>()
        val typeToInt = mockk<(String) -> Int>()
        val csvLoader = CsvLoader()

        // モックの動作設定
        every { showToast(any()) } just Runs
        every { addTriangle(any(), any(), any(), any()) } just Runs
        every { setAllTextSize(any()) } just Runs
        every { typeToInt(any()) } returns 1

        // 関数のテスト実行
        val result = csvLoader.parseCSV(reader, showToast, setAllTextSize, typeToInt, 1.0f)

        // ファイルをクローズ
        reader.close()

        // 検証
        if(result != null){
            result.trilist.arrangePointNumbers()
            print_trilist(result.trilist)
        }
    }
}
