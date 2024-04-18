package com.jpaver.trianglelist
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.*
import java.io.BufferedReader
import java.io.FileReader

class CSVParserTest {

    @Test
    fun `parseCSV correctly parses valid input from file`() {
        val path = "src/test/resources/test.csv"
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
        val result = csvLoader.parseCSV(reader, showToast, addTriangle, setAllTextSize, typeToInt, 1.0f)

        // ファイルをクローズ
        reader.close()

        // 検証
        if(result != null)
            assertEquals(9, result.trilist.size())
    }
}
