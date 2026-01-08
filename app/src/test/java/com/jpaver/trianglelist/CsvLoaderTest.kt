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

    /** 後方互換: SfcWriterTest等から呼ばれる */
    fun loadTriangleList(): ReturnValues? = loadCsvFile("4.11.csv")

    private fun loadCsvFile(filename: String): ReturnValues? {
        val path = "src/test/resources/$filename"
        val charset = StandardCharsets.UTF_8
        val reader = BufferedReader(InputStreamReader(FileInputStream(path), charset))
        val showToast = mockk<(String) -> Unit>()
        val setAllTextSize = mockk<(Float) -> Unit>()
        val typeToInt = mockk<(String) -> Int>()
        val csvLoader = CsvLoader()

        every { showToast(any()) } just Runs
        every { setAllTextSize(any()) } just Runs
        every { typeToInt(any()) } returns 1

        val result: ReturnValues? = csvLoader.parseCSV(reader, showToast, setAllTextSize, typeToInt, 1.0f)
        if (result != null) result.trilist.recoverState()
        reader.close()
        return result
    }

    @Test
    fun `parseCSV correctly parses full format (26 columns)`() {
        val result = loadCsvFile("4.11.csv")

        assert(result != null) { "Result should not be null" }
        result!!.trilist.arrangePointNumbers()
        println("=== Full format (26 columns) ===")
        print_trilist(result.trilist)
        assert(result.trilist.size() > 0) { "Triangle list should not be empty" }
    }

    @Test
    fun `parseCSV correctly parses minimal format (4 columns)`() {
        val result = loadCsvFile("minimal.csv")

        assert(result != null) { "Result should not be null" }
        println("=== Minimal format (4 columns) ===")
        println("Triangle count: ${result!!.trilist.size()}")
        print_trilist(result.trilist)
        assert(result.trilist.size() == 3) { "Should have 3 triangles" }
    }

    @Test
    fun `parseCSV correctly parses connected format (6 columns)`() {
        val result = loadCsvFile("connected.csv")

        assert(result != null) { "Result should not be null" }
        println("=== Connected format (6 columns) ===")
        println("Triangle count: ${result!!.trilist.size()}")
        print_trilist(result.trilist)
        assert(result.trilist.size() == 3) { "Should have 3 triangles" }

        // 接続の確認
        val tri2 = result.trilist.getBy(2)
        println("Triangle 2 parent: ${tri2.parentnumber}")
    }
}
