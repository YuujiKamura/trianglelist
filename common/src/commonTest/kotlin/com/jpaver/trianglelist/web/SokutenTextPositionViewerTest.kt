package com.jpaver.trianglelist.web

import com.jpaver.trianglelist.dxf.DxfParser
import com.jpaver.trianglelist.dxf.DxfText
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 測点名テキストの位置を「ビューワーの目」で見る出力テスト。
 *
 * 経路: CSV -> WebDrawingExport.buildDxfText (アプリ保存と同じ DxfFileWriter)
 *       -> DxfParser.parse (CAD ビューワーが実際に読む parser)
 *       -> TEXT の挿入点を三角形本体の bbox と突き合わせる。
 *
 * 症状 (2026-08-27 user 報告「測点テキストの描画位置が原点になってる」) の実体は
 * 挿入点が NaN。DXF の group 10/20/11/21 に "NaN" という文字列が出るので、
 * CAD 側は数値化に失敗して 0 とみなし、結果として原点に描かれる。
 *
 * 発生源: DimensionLayout.layout の SIDE_SOKUTEN 分岐。
 *   lineLength = textWidth + dimheight*0.4  (text が非空のとき)
 * が dimHeight==0 で 0 になり、pointA==pointB (長さ 0 の測点線) になる。すると
 *   dimpoint = pointA.calcMidPoint(pointB).offset(pointB, offsetH)
 * が零ベクトルを normalize() して NaN を返す (PointXY.kt:234-237)。
 * 書き出し経路の Triangle.dimHeight は既定 0f のまま (CycleShape.kt:30) 誰も
 * 設定しないため、常にこの穴を踏む。
 *
 * 回帰した commit: 552070ca (2026-06-22)「[Gemini CLI] Fix PDF text collapse...」
 * 同 commit 以前は測点線長が定数 -3.0*scale で dimHeight に依存せず、NaN は出なかった。
 */
class SokutenTextPositionViewerTest {

    // ---- 軸の列挙 (この指摘が開く path の cartesian) ----
    // 接続種別: 独立 / 親の B 辺 / 親の C 辺 (SIDE_SOKUTEN の基準辺 = A 辺の由来が変わる)
    private val connTypes = listOf(-1, 1, 2)
    // 測点名の左右 (dim.horizontal.s): 0=既定, 1=反対側 (layout 内で lp/rp が入れ替わる)
    private val sokutenSides = listOf(0, 1)
    // 名前の文字種: 半角のみ / 全角混じり (estimateTextWidth の 0..127 分岐)
    private val names = listOf("No.1", "測点イ")

    private fun csvFor(connType: Int, name: String): String = buildString {
        appendLine("市道○○号線 舗装打換工事")
        appendLine("市道○○号線")
        appendLine("○○建設株式会社")
        appendLine("1/1")
        appendLine("1,10,8,9,-1,-1,$name")
        if (connType != -1) appendLine("2,${if (connType == 1) 8 else 9},7,6,1,$connType,$name")
        appendLine("ListAngle, 180")
    }

    private fun overridesFor(triCount: Int, sokutenSide: Int): String {
        if (sokutenSide == 0) return ""
        val dims = (1..triCount).joinToString(",") { """{"tri":$it,"side":4,"h":1}""" }
        return """{"dims":[$dims]}"""
    }

    private data class Case(val connType: Int, val sokutenSide: Int, val name: String) {
        override fun toString() = "conn=$connType s=$sokutenSide name='$name'"
    }

    private fun allCases(): List<Case> =
        connTypes.flatMap { c -> sokutenSides.flatMap { s -> names.map { n -> Case(c, s, n) } } }

    @Test
    fun 測点名テキストは原点ではなく図形の脇に置かれる() {
        val failures = mutableListOf<String>()

        for (case in allCases()) {
            val triCount = if (case.connType == -1) 1 else 2
            val dxf = WebDrawingExport.buildDxfText(
                csvFor(case.connType, case.name),
                overridesFor(triCount, case.sokutenSide),
                false
            )

            // (1) DXF テキストとして NaN 座標が出ていないこと (CAD は数値化に失敗して原点へ描く)
            val nanLines = dxf.lines().count { it.trim() == "NaN" }
            if (nanLines > 0) failures += "$case: DXF に NaN 座標が $nanLines 行"

            // (2) ビューワー (DxfParser) が読んだ結果、測点名が図形の近傍にあること
            val parsed = DxfParser().parse(dxf)
            val body = parsed.lines.filter { it.layer == "0" }
            if (body.isEmpty()) { failures += "$case: 三角形の線が 0 本 (parse 失敗)"; continue }
            val minX = body.minOf { minOf(it.x1, it.x2) }
            val maxX = body.maxOf { maxOf(it.x1, it.x2) }
            val minY = body.minOf { minOf(it.y1, it.y2) }
            val maxY = body.maxOf { maxOf(it.y1, it.y2) }
            val margin = maxOf(maxX - minX, maxY - minY)

            val hits = parsed.texts.filter { it.text == case.name && it.layer == "0" }
            if (hits.size != triCount) {
                failures += "$case: 測点名 TEXT が ${hits.size} 個 (期待 $triCount)"
                continue
            }
            for (t in hits) failures += checkText(case, t, minX, maxX, minY, maxY, margin)
        }

        if (failures.isNotEmpty()) {
            fail("測点名の配置が壊れているケース ${failures.size} 件:\n" + failures.joinToString("\n"))
        }
    }

    private fun checkText(
        case: Case, t: DxfText,
        minX: Double, maxX: Double, minY: Double, maxY: Double, margin: Double
    ): List<String> = buildList {
        if (t.x.isNaN() || t.y.isNaN()) {
            add("$case: 挿入点が NaN -> CAD は原点に描く: $t")
            return@buildList
        }
        if (abs(t.x) < 1e-6 && abs(t.y) < 1e-6) {
            add("$case: 挿入点が原点 (0,0): $t")
            return@buildList
        }
        val inX = t.x in (minX - margin)..(maxX + margin)
        val inY = t.y in (minY - margin)..(maxY + margin)
        if (!inX || !inY) {
            add("$case: 図形から外れた位置: (${t.x}, ${t.y}) bbox=[$minX,$minY]-[$maxX,$maxY]")
        }
    }

    @Test
    fun 測点線は長さゼロにならない() {
        // 測点名の旗線 (pointA-pointB) が長さ 0 になるのが NaN の直接原因。
        // ビューワーが読んだ LINE の中に、測点名テキストの近くで長さ 0 のものが無いこと。
        val failures = mutableListOf<String>()
        for (case in allCases()) {
            val triCount = if (case.connType == -1) 1 else 2
            val dxf = WebDrawingExport.buildDxfText(
                csvFor(case.connType, case.name),
                overridesFor(triCount, case.sokutenSide),
                false
            )
            val parsed = DxfParser().parse(dxf)
            val degenerate = parsed.lines.filter {
                it.layer == "0" && abs(it.x1 - it.x2) < 1e-9 && abs(it.y1 - it.y2) < 1e-9
            }
            if (degenerate.isNotEmpty()) {
                failures += "$case: 長さ 0 の線が ${degenerate.size} 本 (測点線が潰れている): ${degenerate.first()}"
            }
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }
}
