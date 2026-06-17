package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.EditObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 2026-06-16 WebHitTest mixed 化 (Codex 引き継ぎ tier 1):
 *
 * CLAUDE.md Rule 6 cartesian: 軸 (図形構成 × クリック位置) を generator で全件 pin、件数は
 * 軸の積で決まる (固定数字を書かない)。Triangle のみ CSV は既存 WebHitTestTest (desktopTest)
 * で押さえている — ここは Rectangle が挟まる経路を中心に網羅する。
 *
 * 軸:
 *   - 図形構成: 5 種 (Tri only-1 / Tri only-3 / Rect only-1 / Tri-Rect-Tri / Tri-Tri-Rect)
 *   - クリック位置: 各構成について「全図形の重心 (= mixed 番号期待)」 + 「遠方外側 1 点 (= 0 期待)」
 *
 * 重心は mixed.get(i).vertices() の平均で計算 (= EditObject.centroid と同型)。CSV の座標式に依存せず
 * buildMixed の構築結果が source of truth として動く。これにより CSV を増やすときに座標を手計算する
 * 必要がない (軸を増やしたら generator が自動的にケースを増やす)。
 */
class WebHitTestMixedTest {

    private data class Construct(val name: String, val csv: String)

    /** CSV 形式:
     *  Triangle 行: `num, lengthA, lengthB, lengthC, parent, conn`
     *  Rectangle 行: `Rectangle, num, length, widthA, widthB, parent, side, align`
     *  - parent=-1 / conn=-1 で独立。parent は figureRows 内の混在通し番号 (1-based)
     *  - Triangle.conn: 1=親のB辺, 2=親のC辺 (CLAUDE.md 三角形接続の仕様)
     *  - Rectangle.side: 1=B(左脚), 2=C(上辺), 3=D(右脚)
     */
    private val constructs = listOf(
        Construct(
            "tri-only-1",
            "1,6.0,5.0,4.0,-1,-1\n"
        ),
        Construct(
            "tri-only-3",
            """
                1,6.0,5.0,4.0,-1,-1
                2,5.0,4.0,3.0,1,1
                3,4.0,3.5,3.0,1,2
            """.trimIndent() + "\n"
        ),
        Construct(
            "rect-only-1",
            "Rectangle,1,2.0,3.0,3.0,-1,0,0\n"
        ),
        Construct(
            "tri-rect-tri",
            """
                1,6.0,5.0,4.0,-1,-1
                Rectangle,2,2.0,5.0,5.0,1,1,0
                3,4.0,3.5,3.0,1,2
            """.trimIndent() + "\n"
        ),
        Construct(
            "tri-tri-rect",
            """
                1,6.0,5.0,4.0,-1,-1
                2,5.0,4.0,3.0,1,1
                Rectangle,3,2.0,4.0,4.0,1,2,0
            """.trimIndent() + "\n"
        ),
    )

    @Test
    fun `every figure centroid hits its own mixed number`() {
        for (c in constructs) {
            val doc = CsvCodec.parse(c.csv)
            val trilist = CsvCodec.build(doc)
            val mixed = CsvCodec.buildMixed(doc, trilist, 1f)
            assertTrue(mixed.size() > 0, "construct '${c.name}' built no figures")
            for (i in 1..mixed.size()) {
                val center = centroidOf(mixed.get(i))
                val cx = center.x.toFloat()
                val cy = center.y.toFloat()
                val got = WebHitTest.hitTriangle(c.csv, cx, cy)
                assertEquals(
                    i,
                    got,
                    "construct '${c.name}' figure #$i centroid ($cx, $cy) expected $i, got $got",
                )
            }
        }
    }

    @Test
    fun `far outside hits 0 for every construct`() {
        for (c in constructs) {
            val got = WebHitTest.hitTriangle(c.csv, 10_000f, 10_000f)
            assertEquals(0, got, "construct '${c.name}' far-outside expected 0, got $got")
        }
    }

    private fun centroidOf(obj: EditObject): PointXY {
        val vs = obj.vertices()
        assertTrue(vs.isNotEmpty(), "vertices() returned empty for $obj")
        var sx = 0.0
        var sy = 0.0
        for (v in vs) {
            sx += v.x.toDouble()
            sy += v.y.toDouble()
        }
        return PointXY((sx / vs.size).toFloat(), (sy / vs.size).toFloat())
    }
}
