package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.ConnCode
import com.jpaver.trianglelist.editmodel.ConnParam
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList

/**
 * Web 段階1 用の最小 CSV リーダー (insight #61)。
 *
 * app/datamanager/CsvLoader.parseCSV は BufferedReader (java.io) に結合しているため
 * commonMain に置けない。三角形生成の中核 (buildTriangle の最小形式 + 接続形式の分岐) だけを
 * CsvLoader.kt:191-238 と同じロジックで再実装する。完全形式 (28カラム) のレイアウト復元・
 * Deduction・ListAngle/ListScale/TextSize 行は段階1 のスコープ外 (スキップ)。
 *
 * 対応形式 (desktop/sample/sample_triangles.csv と同形):
 *   番号, 辺A, 辺B, 辺C [, 親番号, 接続タイプ]
 *   接続タイプ: -1=独立, 1=親のB辺, 2=親のC辺 (A辺接続は設計上存在しない)
 * 先頭カラムが整数でない行 (ヘッダー・Deduction 等) は読み飛ばす。
 */
object WebCsvReader {

    fun read(csv: String): TriangleList {
        val trilist = TriangleList()
        for (line in csv.lineSequence()) {
            val chunks = line.split(",").map { it.trim() }
            if (chunks.size < 4) continue

            // 数値でない行 (ヘッダー, ListAngle, Deduction 等) はスキップ (CsvLoader.buildTriangle と同じ)
            val number = chunks[0].toIntOrNull() ?: continue
            if (number < 0) continue

            val lengthA = chunks[1].toFloatOrNull() ?: continue
            val lengthB = chunks[2].toFloatOrNull() ?: continue
            val lengthC = chunks[3].toFloatOrNull() ?: continue
            val parent = chunks.getOrNull(4)?.toIntOrNull() ?: -1
            val connectionType = chunks.getOrNull(5)?.toIntOrNull() ?: -1

            // 非接続（独立三角形）— CsvLoader と同じ初期位置 (0,0)・角度 180
            if (connectionType < 1) {
                trilist.add(
                    Triangle(lengthA, lengthB, lengthC, PointXY(0f, 0f), 180f),
                    true
                )
            }
            // 接続 — コード 1/2 = 辺共有、3-8 = 二重断面 (右/左/中央)、9/10 = フロート。
            // 完全形式 (列17-19 = cp.side/type/lcr) があればそれを優先 (CsvLoader.readCParamSafe と同形)
            else {
                if (parent < 1 || parent > trilist.size()) continue
                val ptri = trilist.getBy(parent)
                val cpSide = chunks.getOrNull(17)?.toIntOrNull()
                val cpType = chunks.getOrNull(18)?.toIntOrNull()
                val cpLcr = chunks.getOrNull(19)?.toIntOrNull()
                val cp = if (cpSide != null && cpType != null && cpLcr != null) {
                    ConnParam(cpSide, cpType, cpLcr, lengthA)
                } else {
                    ConnCode.toConnParam(connectionType, lengthA) ?: continue
                }
                trilist.add(Triangle(ptri, cp, lengthB, lengthC), true)
            }

            // 接続タイプの記録 (CsvLoader.finalizeBuildTriangle と同じ)
            trilist.getBy(trilist.size()).connectionSide = connectionType
        }
        return trilist
    }
}
