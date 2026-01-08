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
        assert(result.trilist.size() == 10) { "Should have 10 triangles" }

        // 全三角形の接続情報を出力・検証
        for (i in 1..10) {
            val tri = result.trilist.getBy(i)
            println("Triangle $i: parent=${tri.parentnumber}, connectionSide=${tri.connectionSide}, area=${tri.getArea()}")
            assert(tri.getArea() > 0) { "Triangle $i should have positive area" }
        }

        // 各接続タイプの検証
        // 三角形1: 独立
        val tri1 = result.trilist.getBy(1)
        assert(tri1.connectionSide == -1) { "Triangle 1 should be independent" }

        // 三角形2-4: 三角形1からの各辺接続
        assert(result.trilist.getBy(2).connectionSide == 1) { "Triangle 2: A edge from 1" }
        assert(result.trilist.getBy(3).connectionSide == 2) { "Triangle 3: B edge from 1" }
        assert(result.trilist.getBy(4).connectionSide == 3) { "Triangle 4: C edge from 1" }

        // 三角形5-7: 三角形2からの各辺接続
        assert(result.trilist.getBy(5).connectionSide == 1) { "Triangle 5: A edge from 2" }
        assert(result.trilist.getBy(6).connectionSide == 2) { "Triangle 6: B edge from 2" }
        assert(result.trilist.getBy(7).connectionSide == 3) { "Triangle 7: C edge from 2" }

        // 三角形8-10: 三角形3からの各辺接続
        assert(result.trilist.getBy(8).connectionSide == 1) { "Triangle 8: A edge from 3" }
        assert(result.trilist.getBy(9).connectionSide == 2) { "Triangle 9: B edge from 3" }
        assert(result.trilist.getBy(10).connectionSide == 3) { "Triangle 10: C edge from 3" }

        // 面積計算が可能か確認（図形生成の成功を確認）
        val totalArea = result.trilist.getArea()
        println("Total area: $totalArea")
        assert(totalArea > 0) { "Total area should be positive" }
    }
}
