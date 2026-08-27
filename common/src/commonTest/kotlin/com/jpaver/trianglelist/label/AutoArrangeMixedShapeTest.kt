package com.jpaver.trianglelist.label

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.EditList
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 台形 (Rectangle) が混ざったリストでの自動配置。
 *
 * リリース前の網羅として、図形種別の軸を押さえる: 三角形だけ / 台形だけ /
 * 台形+子三角形 / 三角形+台形の連結。台形は
 *   - 番号位置が重心固定 (pointNumberAnchor() = centroid) で動かせない
 *   - 辺が 4 本あり、D 辺 (side 3) は横方向コードを持たない
 * ので、退避が「動かせないものを動かそうとして壊す/落ちる」ことがないかを見る。
 */
class AutoArrangeMixedShapeTest {

    private val NL = 10.toChar().toString()

    private fun mixed(csv: String): EditList<CycleShape> {
        val doc = CsvCodec.parse(csv)
        return CsvCodec.buildMixed(doc, CsvCodec.build(doc), 1f)
    }

    private val cases = mapOf(
        "台形だけ" to ("Rectangle,1,5,4,3,-1,0" + NL),
        "三角形+台形" to ("1,6.0,5.0,4.0,-1,-1" + NL + "Rectangle,1,5,4,3,1,1" + NL),
        "台形+子三角形" to ("Rectangle,1,5,10,7,-1,0" + NL + "2,7,4,4,1,2" + NL),
        "小さい台形+三角形" to ("1,1.2,1.0,0.9,-1,-1" + NL + "Rectangle,1,1,0.8,0.6,1,1" + NL),
    )

    @Test
    fun `台形が混ざっても落ちず 値が壊れない`() {
        for ((name, csv) in cases) {
            for (ts in kotlin.collections.listOf(0.25f, 0.525f, 1.0f)) {
                val list = mixed(csv)
                NumberCircleEscape.apply(list, NumberCircleEscape.solve(list, ts))
                DimensionTextEscape.apply(list, DimensionTextEscape.solve(list, ts))

                list.forEachItem { shape ->
                    val anchor = shape.pointNumberAnchor()
                    assertTrue(anchor.x.isFinite() && anchor.y.isFinite(), "$name ts=$ts で番号が NaN")
                    val h = shape.dimHorizontal
                    assertTrue(
                        h.a in 0..4 && h.b in 0..4 && h.c in 0..4 && h.s in 0..4,
                        "$name ts=$ts で horizontal が範囲外: $h",
                    )
                }
            }
        }
    }

    @Test
    fun `台形の番号は動かさない (重心固定なので動かせない)`() {
        val list = mixed(cases.getValue("小さい台形+三角形"))
        val before = mutableMapOf<Int, Pair<Double, Double>>()
        list.forEachItemIndexed { num, shape ->
            if (shape is Rectangle) before[num] = shape.pointNumberAnchor().x to shape.pointNumberAnchor().y
        }
        assertTrue(before.isNotEmpty(), "前提: 台形が含まれるケースのはず")

        val moves = NumberCircleEscape.solve(list, 0.525f)
        NumberCircleEscape.apply(list, moves)

        list.forEachItemIndexed { num, shape ->
            if (shape is Rectangle) {
                val (x, y) = before.getValue(num)
                assertEquals(x, shape.pointNumberAnchor().x, 1e-9, "台形 #$num の番号が動いた")
                assertEquals(y, shape.pointNumberAnchor().y, 1e-9, "台形 #$num の番号が動いた")
            }
        }
    }

    @Test
    fun `台形混在でも繰り返し適用で配置が動き続けない`() {
        for ((name, csv) in cases) {
            val list = mixed(csv)
            fun snapshot(): String = buildString {
                list.forEachItemIndexed { num, s ->
                    val a = s.pointNumberAnchor()
                    append(num).append(':')
                    append("%.6f,%.6f".format(a.x, a.y)).append(':')
                    append(s.dimHorizontal.a).append(s.dimHorizontal.b)
                    append(s.dimHorizontal.c).append(s.dimHorizontal.s).append('|')
                }
            }
            fun arrange() {
                NumberCircleEscape.apply(list, NumberCircleEscape.solve(list, 0.525f))
                DimensionTextEscape.apply(list, DimensionTextEscape.solve(list, 0.525f))
            }
            arrange()
            val once = snapshot()
            arrange()
            assertEquals(once, snapshot(), "$name で 2 回目に配置が動いた")
        }
    }

    @Test
    fun `三角形と台形が混ざっていても三角形側は従来どおり退避できる`() {
        val list = mixed(cases.getValue("三角形+台形"))
        val moves = NumberCircleEscape.solve(list, 2.0f) // 大きい文字で必ず衝突させる

        assertTrue(
            moves.all { move ->
                var isTriangle = false
                list.forEachItemIndexed { num, s -> if (num == move.shapeNumber) isTriangle = s is Triangle }
                isTriangle
            },
            "台形の番号を動かす提案が出た: $moves",
        )
    }
}
