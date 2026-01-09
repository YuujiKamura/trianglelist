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
        assert(result.trilist.size() == 7) { "Should have 7 triangles" }

        // 全三角形の接続情報を出力・検証
        for (i in 1..7) {
            val tri = result.trilist.getBy(i)
            println("Triangle $i: parent=${tri.parentnumber}, connectionSide=${tri.connectionSide}, area=${tri.getArea()}")
            assert(tri.getArea() > 0) { "Triangle $i should have positive area" }
        }

        // 各接続タイプの検証 (connectionSide: 1=B辺, 2=C辺)
        val tri1 = result.trilist.getBy(1)
        assert(tri1.connectionSide == -1) { "Triangle 1 should be independent" }

        // 三角形1からB辺/C辺接続
        assert(result.trilist.getBy(2).connectionSide == 1) { "Triangle 2: B edge from 1" }
        assert(result.trilist.getBy(3).connectionSide == 2) { "Triangle 3: C edge from 1" }

        // 三角形2からB辺/C辺接続
        assert(result.trilist.getBy(4).connectionSide == 1) { "Triangle 4: B edge from 2" }
        assert(result.trilist.getBy(5).connectionSide == 2) { "Triangle 5: C edge from 2" }

        // 三角形3からB辺/C辺接続
        assert(result.trilist.getBy(6).connectionSide == 1) { "Triangle 6: B edge from 3" }
        assert(result.trilist.getBy(7).connectionSide == 2) { "Triangle 7: C edge from 3" }

        // 面積計算が可能か確認（図形生成の成功を確認）
        val totalArea = result.trilist.getArea()
        println("Total area: $totalArea")
        assert(totalArea > 0) { "Total area should be positive" }

        // 座標検証: 接続辺の端点が一致しているか
        println("\n=== 座標検証 ===")
        verifyConnectionCoordinates(result.trilist)
    }

    /**
     * 接続された三角形の共有辺が実際に座標一致しているか検証
     * A辺接続: 親のA辺(CA-AB) = 子のA辺
     * B辺接続: 親のB辺(AB-BC) = 子のA辺
     * C辺接続: 親のC辺(BC-CA) = 子のA辺
     */
    private fun verifyConnectionCoordinates(trilist: TriangleList) {
        for (i in 1..7) {
            val tri = trilist.getBy(i)
            val parentNum = tri.parentnumber
            println("Triangle $i: parentnumber=$parentNum, nodeA=${tri.nodeA?.mynumber}, lengthA=${tri.lengthA_}")
            if (parentNum < 1) continue  // 独立三角形はスキップ

            val parent = trilist.getBy(parentNum)
            val connType = tri.connectionSide
            println("  Parent ${parent.mynumber}: lengthA=${parent.lengthA_}, pointCA=${parent.pointCA}")

            // 子の接続辺（常にA辺=CA-AB）
            val childP1 = tri.pointCA
            val childP2 = tri.pointAB_()

            // 親の対応する辺を取得
            // connectionSide: 1=B辺接続, 2=C辺接続 (Triangle.kt:137参照)
            val (parentP1, parentP2) = when (connType) {
                1 -> parent.pointAB_() to parent.pointBC_()  // B辺(AB-BC)
                2 -> parent.pointBC_() to parent.pointCA     // C辺(BC-CA)
                3 -> parent.pointCA to parent.pointAB_()     // A辺(CA-AB)? 要確認
                else -> continue
            }

            // 座標出力
            println("Triangle $i (→$parentNum via ${edgeName(connType)}):")
            println("  子のA辺: (${childP1.x}, ${childP1.y}) - (${childP2.x}, ${childP2.y})")
            println("  親の辺:  (${parentP1.x}, ${parentP1.y}) - (${parentP2.x}, ${parentP2.y})")

            // 距離計算（順方向 or 逆方向で一致するはず）
            val dist1 = childP1.lengthTo(parentP1) + childP2.lengthTo(parentP2)
            val dist2 = childP1.lengthTo(parentP2) + childP2.lengthTo(parentP1)
            val minDist = minOf(dist1, dist2)

            println("  誤差: $minDist")
            assert(minDist < 0.01f) { "Triangle $i: 接続辺が一致しない (誤差=$minDist)" }
        }
    }

    private fun edgeName(type: Int) = when(type) {
        1 -> "B辺"
        2 -> "C辺"
        3 -> "A辺"
        else -> "不明"
    }
}
