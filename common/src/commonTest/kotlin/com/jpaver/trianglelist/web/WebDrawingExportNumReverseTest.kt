package com.jpaver.trianglelist.web

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 番号逆順 (numreverse) 付き書き出しの pin。
 *
 * アプリの現物 (= 正): 保存ダイアログの NumReverse ボタン (MainActivity.kt:2293) →
 * writer.isReverse_ = true (:2578/:2597) → DxfFileWriter.writeEntities:319-323 で
 * trilistNumbered.resetNumReverse() (= 三角形の幾何はそのまま、mynumber だけ逆順に振り直す、
 * TriangleList.kt:927-936) + 控除リスト reverse。つまり「同じ位置の三角形に逆順の番号が付く」。
 *
 * pin: 2 三角形 fixture で番号テキスト "1"/"2" の座標を DXF から拾い、
 * normal の "1" の位置 == reverse の "2" の位置 (とその逆) を assert する。
 */
class WebDrawingExportNumReverseTest {

    private val fixtureCsv = """
        市道○○号線 舗装打換工事
        市道○○号線
        ○○建設株式会社
        1/1
        1,10,2,10,-1,-1
        2,10,1,10,1,2
        ListAngle, 180
    """.trimIndent() + "\n"

    /**
     * DXF は group code 行と値行のペアの連なり。TEXT エンティティの文字列は code 1、
     * 直前に出る code 10/20 が挿入点 (DxfEntity.writeTextHV:51-60 の出力順)。
     * ENTITIES 内で値がちょうど "1"/"2" の TEXT = 三角形番号 (writePointNumber:80。
     * 寸法値は " 10.0" 形式、図枠は "1/1" 等なので単独数字と衝突しない)。
     * 面積計算書にも番号は出るが、最初の出現は writeTriangle 経由の番号サークル側
     * (writeEntities:342-345 が図枠・計算書より先)。
     */
    private fun firstNumberTextPositions(dxf: String): Map<String, Pair<Double, Double>> {
        val lines = dxf.lines()
        val found = mutableMapOf<String, Pair<Double, Double>>()
        var x = 0.0
        var y = 0.0
        var i = 0
        while (i + 1 < lines.size) {
            val code = lines[i].trim()
            val value = lines[i + 1].trim()
            when (code) {
                "10" -> x = value.toDoubleOrNull() ?: x
                "20" -> y = value.toDoubleOrNull() ?: y
                "1" -> if ((value == "1" || value == "2") && value !in found) found[value] = x to y
            }
            if (found.size == 2) break
            i += 2
        }
        return found
    }

    @Test
    fun dxf_numreverse_swaps_number_texts_in_place() {
        val normal = WebDrawingExport.buildDxfText(fixtureCsv, "", false)
        val reversed = WebDrawingExport.buildDxfText(fixtureCsv, "", true)
        val n = firstNumberTextPositions(normal)
        val r = firstNumberTextPositions(reversed)
        assertEquals(setOf("1", "2"), n.keys, "normal: number texts 1/2 not found")
        assertEquals(setOf("1", "2"), r.keys, "reversed: number texts 1/2 not found")
        // 幾何 (挿入点) は据え置きのまま、番号だけが入れ替わる
        fun near(a: Pair<Double, Double>, b: Pair<Double, Double>) =
            abs(a.first - b.first) < 1e-3 && abs(a.second - b.second) < 1e-3
        assertTrue(near(n["1"]!!, r["2"]!!), "normal#1 ${n["1"]} != reversed#2 ${r["2"]}")
        assertTrue(near(n["2"]!!, r["1"]!!), "normal#2 ${n["2"]} != reversed#1 ${r["1"]}")
        // 同位置で番号が違う = 全体としても別物
        assertTrue(normal != reversed)
    }

    @Test
    fun numreverse_false_keeps_legacy_output() {
        // 既定 (false) は従来 2 引数版と完全同一 — golden 同値テストの前提を崩さない
        assertEquals(
            WebDrawingExport.buildDxfText(fixtureCsv, ""),
            WebDrawingExport.buildDxfText(fixtureCsv, "", false),
        )
        assertEquals(
            WebDrawingExport.buildSfcText(fixtureCsv, "test.sfc", ""),
            WebDrawingExport.buildSfcText(fixtureCsv, "test.sfc", "", false),
        )
    }

    @Test
    fun sfc_numreverse_changes_output() {
        // SFC も同じ resetNumReverse 経路 (SfcWriter.kt:49-53)。反転で出力が変わることを pin
        val normal = WebDrawingExport.buildSfcText(fixtureCsv, "test.sfc", "", false)
        val reversed = WebDrawingExport.buildSfcText(fixtureCsv, "test.sfc", "", true)
        assertTrue(normal != reversed)
    }
}
