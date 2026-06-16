package com.jpaver.trianglelist.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * 2026-06-17 Codex 引き継ぎ tier 5: Rectangle 子 Triangle の number override が DXF export
 * に乗ることを pin。 WebDrawingExport.suffixWithBase の `if (!t.pointNumber.flag.isMovedByUser)
 * t.pointnumber = t.pointcenter` ガードが正しく Rectangle 子 Triangle の手動配置を保護することの
 * 回帰テスト ── ガードが外れると DXF/SFC export 時に override 値が pointcenter に上書きされ
 * 「render では動くが export で消える」 タイプの silent regression を起こす。
 *
 * 構造的検証 (WebDrawingExportNumReverseTest と同型): DXF の TEXT エンティティから番号テキスト
 * の挿入点 (group code 10/20) を抜き、 baseline (override なし) と target (override あり)
 * で 該当 Triangle の番号テキスト位置が **異なる** (= override 反映) と、 他図形 (Rectangle
 * mixed #1) の位置が **同じ** (= 誤適用なし) を assert。
 */
class WebDxfRectChildNumberOverrideTest {

    /**
     * mixed #1 = Rectangle (独立、 length=5/widthA=10/widthB=7/align=0)
     * mixed #2 = Triangle (parent=1 = Rectangle、 conn=2 = Rectangle.C 辺 上辺 接続)
     */
    private val csv = "Rectangle,1,5,10,7,-1,0\n2,7,4,4,1,2\n"

    private data class TextPos(val x: Double, val y: Double)

    /**
     * DXF の TEXT エンティティから 値 (group code 1) = "1" / "2" の最初の出現を拾う。
     * code 10/20 は挿入点 X/Y、 直前ペアが TEXT の挿入点として有効。
     * 寸法値は " 10.0" のような形式、 図枠タイトルは "1/1" 等で単独数字 "1"/"2" とは衝突しない
     * (WebDrawingExportNumReverseTest の parser と同型・同前提)。
     */
    private fun numberTextPositions(dxf: String): Map<String, TextPos> {
        val lines = dxf.lines()
        val found = mutableMapOf<String, TextPos>()
        var x = 0.0
        var y = 0.0
        var i = 0
        while (i + 1 < lines.size) {
            val code = lines[i].trim()
            val value = lines[i + 1].trim()
            when (code) {
                "10" -> x = value.toDoubleOrNull() ?: x
                "20" -> y = value.toDoubleOrNull() ?: y
                "1" -> if ((value == "1" || value == "2") && value !in found) {
                    found[value] = TextPos(x, y)
                }
            }
            if (found.size == 2) break
            i += 2
        }
        return found
    }

    @Test
    fun `rectangle child triangle number override moves only its own number text in DXF`() {
        val baseline = WebDrawingExport.buildDxfText(csv, "", false)
        val withOverride = WebDrawingExport.buildDxfText(
            csv,
            """{"numbers":[{"tri":2,"x":1.5,"y":2.5}]}""",
            false,
        )

        val b = numberTextPositions(baseline)
        val t = numberTextPositions(withOverride)
        assertEquals(setOf("1", "2"), b.keys, "baseline DXF must include number texts 1 and 2")
        assertEquals(setOf("1", "2"), t.keys, "target DXF must include number texts 1 and 2")

        // Rectangle mixed #1 の番号テキスト位置は不変 (誤適用なし)
        assertEquals(b["1"], t["1"], "Rectangle #1 number text moved unexpectedly")

        // Rectangle 子 Triangle #2 の番号テキスト位置は override で動く
        // suffixWithBase の isMovedByUser ガードが effective でないとここが等しくなる
        assertNotEquals(
            b["2"],
            t["2"],
            "Triangle #2 number text did not move — number override ignored by numberTrapTris/suffixWithBase path?",
        )
    }
}
