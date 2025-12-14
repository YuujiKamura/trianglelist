import com.jpaver.trianglelist.dxf.DxfAnalyzer
import com.jpaver.trianglelist.dxf.DxfParser
import com.jpaver.trianglelist.dxf.DxfIndex
import java.io.File

fun main(args: Array<String>) {
    val path = if (args.isNotEmpty()) {
        args[0]
    } else {
        "H:/マイドライブ/〇市道 南千反畑町第１号線舗装補修工事/１０測量と設計照査/面積展開図_南千反畑町第１号線.dxf"
    }

    val file = File(path)
    if (!file.exists()) {
        println("File not found: $path")
        return
    }

    println("Analyzing: ${file.name}")
    println("File size: ${file.length() / 1024} KB")
    println()

    // DXFをパース
    val content = file.readText()
    val parser = DxfParser()
    val result = parser.parse(content)
    val index = DxfIndex(result)

    println("=== 測点一覧 ===")
    index.getAllStations().entries.sortedBy { it.value.first }.forEach { (name, coord) ->
        println("  $name: X=${coord.first.toInt()}, Y=${coord.second.toInt()}")
    }

    println("\n=== レイヤー別LINE数 ===")
    val linesByLayer = result.lines.groupBy { it.layer }
    linesByLayer.entries.sortedByDescending { it.value.size }.forEach { (layer, lines) ->
        println("  $layer: ${lines.size} lines")
    }

    // No.3+11とNo.3+15を探す
    val no3_11 = index.getStationCoord("No.3+11")
    val no3_15 = index.getStationCoord("No.3+15")

    if (no3_11 != null) {
        println("\n=== No.3+11 情報 ===")
        println("  X=${no3_11.first.toInt()}, Y=${no3_11.second.toInt()}")

        if (no3_15 != null) {
            println("  No.3+15: X=${no3_15.first.toInt()}")
            println("  距離: ${(no3_15.first - no3_11.first).toInt()}mm")
        }

        // No.3+11周辺（前後5m）の線を検索
        println("\n=== No.3+11周辺 (±5000mm) のLINE ===")
        val nearbyLines = result.lines.filter { line ->
            val lineMinX = minOf(line.x1, line.x2)
            val lineMaxX = maxOf(line.x1, line.x2)
            // 線がNo.3+11の±5000mm範囲と重なるか
            lineMaxX >= no3_11.first - 5000 && lineMinX <= no3_11.first + 5000
        }

        nearbyLines.groupBy { it.layer }.entries.sortedByDescending { it.value.size }.forEach { (layer, lines) ->
            println("\n  [$layer] ${lines.size} lines")
            if (lines.size <= 30) {
                lines.sortedBy { minOf(it.x1, it.x2) }.forEach { line ->
                    val minX = minOf(line.x1, line.x2).toInt()
                    val maxX = maxOf(line.x1, line.x2).toInt()
                    val minY = minOf(line.y1, line.y2).toInt()
                    val maxY = maxOf(line.y1, line.y2).toInt()
                    println("    X: $minX ~ $maxX, Y: $minY ~ $maxY")
                }
            }
        }
    }
}
