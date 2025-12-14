import com.jpaver.trianglelist.dxf.DxfIndex
import com.jpaver.trianglelist.dxf.DxfLine
import com.jpaver.trianglelist.dxf.DxfParser
import com.jpaver.trianglelist.dxf.DxfParseResult
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * DXFから区画線を抽出するデータクラス
 */
data class ExtractedMarking(
    val type: String,           // 横断歩道, ダイヤマーク, etc.
    val startStation: String,   // 起点測点
    val endStation: String,     // 終点測点
    val length: Int,            // 延長(m)
    val count: Int,             // 本数
    val remarks: String         // 備考
)

/**
 * DXFから区画線を抽出してリスト化
 */
class DxfMarkingExtractor(
    private val parseResult: DxfParseResult,
    private val index: DxfIndex
) {
    private val stations = index.getAllStations().entries.sortedBy { it.value.first }

    /**
     * 全区画線を抽出
     */
    fun extractAll(): List<ExtractedMarking> {
        val markings = mutableListOf<ExtractedMarking>()

        // 横断歩道を検出
        extractCrosswalks()?.let { markings.add(it) }

        // ダイヤマークを検出
        extractDiamondMarks().forEach { markings.add(it) }

        return markings
    }

    /**
     * 横断歩道を検出
     * 路面標示レイヤーの矩形パターンを検索
     */
    private fun extractCrosswalks(): ExtractedMarking? {
        // 路面標示レイヤーの線を取得
        val markingLines = parseResult.lines.filter {
            it.layer.contains("路面標示") || it.layer.contains("横断")
        }

        if (markingLines.isEmpty()) return null

        // X座標の範囲を取得
        val minX = markingLines.minOf { minOf(it.x1, it.x2) }
        val maxX = markingLines.maxOf { maxOf(it.x1, it.x2) }

        // ストライプ数をカウント（水平線の数 / 2）
        val horizontalLines = markingLines.filter {
            abs(it.y1 - it.y2) < 1.0 // 水平線
        }
        val stripeCount = horizontalLines.size / 2  // 上下2本で1ストライプ

        // 測点に変換
        val startStation = findNearestStation(minX)
        val endStation = findNearestStation(maxX)
        val length = ((maxX - minX) / 1000).roundToInt()

        return ExtractedMarking(
            type = "横断歩道",
            startStation = startStation,
            endStation = endStation,
            length = length,
            count = stripeCount,
            remarks = "施5号 W=45cm"
        )
    }

    /**
     * ダイヤマークを検出
     * 5000×1500mmのひし形パターンを検索
     */
    private fun extractDiamondMarks(): List<ExtractedMarking> {
        // ダイヤマーク用レイヤーまたは補助線から検索
        val candidates = parseResult.lwPolylines.filter {
            it.layer.contains("ダイヤ") || it.layer.contains("路面標示")
        }

        // 簡易的に: 5000mm幅の図形を検索
        val diamonds = mutableListOf<ExtractedMarking>()

        // TODO: 実際のダイヤマーク検出ロジック

        return diamonds
    }

    /**
     * X座標から最も近い測点を検索
     */
    private fun findNearestStation(x: Double): String {
        var nearest = stations.firstOrNull()?.key ?: "No.0"
        var minDist = Double.MAX_VALUE

        for ((name, coord) in stations) {
            val dist = abs(coord.first - x)
            if (dist < minDist) {
                minDist = dist
                nearest = name
            }
        }

        // 測点から離れている場合は +offset 形式で表記
        val nearestCoord = index.getStationCoord(nearest)
        if (nearestCoord != null) {
            val offset = ((x - nearestCoord.first) / 1000).roundToInt()
            if (offset != 0 && abs(offset) < 20) {
                // No.3 + 11m → No.3+11
                val baseParts = nearest.split("+")
                val baseNo = baseParts[0]
                val existingOffset = baseParts.getOrNull(1)?.toIntOrNull() ?: 0
                val newOffset = existingOffset + offset
                return if (newOffset > 0) "$baseNo+$newOffset" else baseNo
            }
        }

        return nearest
    }
}

/**
 * シート同期用のJSON出力
 */
fun List<ExtractedMarking>.toSheetData(): List<List<String>> {
    val header = listOf("No", "種類", "起点", "終点", "延長(m)", "本数", "備考")
    val rows = mapIndexed { idx, m ->
        listOf(
            (idx + 1).toString(),
            m.type,
            m.startStation,
            m.endStation,
            m.length.toString(),
            m.count.toString(),
            m.remarks
        )
    }
    return listOf(header) + rows
}

/**
 * メイン: DXFから区画線を抽出してコンソール出力
 */
fun main(args: Array<String>) {
    val path = if (args.isNotEmpty()) args[0] else
        "H:/マイドライブ/〇市道 南千反畑町第１号線舗装補修工事/１０測量と設計照査/面積展開図_南千反畑町第１号線.dxf"

    val file = File(path)
    if (!file.exists()) {
        println("File not found: $path")
        return
    }

    println("=== DXF区画線抽出 ===")
    println("File: ${file.name}")

    val content = file.readText()
    val parser = DxfParser()
    val result = parser.parse(content)
    val index = DxfIndex(result)

    val extractor = DxfMarkingExtractor(result, index)
    val markings = extractor.extractAll()

    println("\n=== 抽出結果 ===")
    if (markings.isEmpty()) {
        println("区画線が見つかりませんでした")
        return
    }

    // テーブル形式で出力
    println("No\t種類\t\t起点\t\t終点\t\t延長\t本数\t備考")
    println("-".repeat(80))
    markings.forEachIndexed { idx, m ->
        println("${idx + 1}\t${m.type}\t${m.startStation}\t\t${m.endStation}\t\t${m.length}m\t${m.count}\t${m.remarks}")
    }

    // シート用データも出力
    println("\n=== シート用データ (CSV) ===")
    markings.toSheetData().forEach { row ->
        println(row.joinToString(","))
    }
}
