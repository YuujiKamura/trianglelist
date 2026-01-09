import java.io.File

/**
 * DXF内の横断歩道を修正するツール
 * No.3+11を中央→起点に変更（+2000mmシフト）
 */
fun main() {
    val path = "H:/マイドライブ/〇市道 南千反畑町第１号線舗装補修工事/１０測量と設計照査/面積展開図_南千反畑町第１号線.dxf"
    val file = File(path)

    if (!file.exists()) {
        println("File not found: $path")
        return
    }

    // バックアップ作成
    val backupPath = path.replace(".dxf", "_backup.dxf")
    file.copyTo(File(backupPath), overwrite = true)
    println("Backup created: $backupPath")

    val content = file.readText()
    val lines = content.lines().toMutableList()

    // 横断歩道の線を検索してシフト
    // X座標が118721～122721の範囲で、Y座標が114641～120491の範囲の線を対象
    val shiftX = 2000.0  // +2000mm シフト

    var modified = 0
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()

        // グループコード10（X座標）を探す
        if (line == "10" && i + 1 < lines.size) {
            val xValue = lines[i + 1].trim().toDoubleOrNull()
            if (xValue != null && xValue >= 118721.0 - 1 && xValue <= 122721.0 + 1) {
                // この付近のエンティティが横断歩道かチェック（Y座標範囲）
                // 前後を見てレイヤーを確認
                var isInCrosswalk = false
                for (j in maxOf(0, i - 50)..minOf(lines.size - 1, i + 10)) {
                    if (lines[j].trim() == "8" && j + 1 < lines.size) {
                        val layerName = lines[j + 1].trim()
                        if (layerName.contains("路面標示") || layerName.contains("横断")) {
                            isInCrosswalk = true
                            break
                        }
                    }
                }

                if (isInCrosswalk) {
                    val newX = xValue + shiftX
                    lines[i + 1] = newX.toString()
                    modified++
                    println("Shifted X: $xValue -> $newX")
                }
            }
        }

        // グループコード11（X2座標）も同様に処理
        if (line == "11" && i + 1 < lines.size) {
            val xValue = lines[i + 1].trim().toDoubleOrNull()
            if (xValue != null && xValue >= 118721.0 - 1 && xValue <= 122721.0 + 1) {
                var isInCrosswalk = false
                for (j in maxOf(0, i - 50)..minOf(lines.size - 1, i + 10)) {
                    if (lines[j].trim() == "8" && j + 1 < lines.size) {
                        val layerName = lines[j + 1].trim()
                        if (layerName.contains("路面標示") || layerName.contains("横断")) {
                            isInCrosswalk = true
                            break
                        }
                    }
                }

                if (isInCrosswalk) {
                    val newX = xValue + shiftX
                    lines[i + 1] = newX.toString()
                    modified++
                    println("Shifted X2: $xValue -> $newX")
                }
            }
        }

        i++
    }

    if (modified > 0) {
        file.writeText(lines.joinToString("\n"))
        println("\n=== 完了 ===")
        println("$modified 箇所の座標を修正しました")
        println("横断歩道: X=118721~122721 → X=120721~124721")
        println("(No.3+11を起点に4m)")
    } else {
        println("修正対象が見つかりませんでした")
    }
}
