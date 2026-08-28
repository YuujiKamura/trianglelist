package com.jpaver.trianglelist.label

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 一番短い辺の寸法値は動かさない (2026-08-28 user「三辺の中のもっとも狭い辺って、
 * 単純に横スライドしても見づらくなるケースが結構ある。５，６とか、８とか、むしろ
 * 動かさないほうが良い」)。
 *
 * 短い辺には動かす余地が無い。スライド量は辺長の 10% (DimensionLayout の habayose) なので
 * 辺が短いほど効かず、旗揚げは辺の延長線上へ出るので短辺では文字が辺から離れすぎて
 * 「どの辺の寸法か」が読めなくなる ── 衝突数は減っても図面としては悪化する。
 *
 * ここは探索の目的関数 (衝突が減れば採用) では守れない: 機械の数字は減っているので、
 * 動かしてはいけない対象を**先に外す**しかない。実データ 8.25 では #5 が 3 辺とも、
 * #6 / #8 も動かされていた。
 */
class DimensionShortestSideTest {

    private val NL = 10.toChar().toString()

    private fun listOf(csv: String): TriangleList = CsvCodec.build(CsvCodec.parse(csv))

    /** 細い三角形が連続して寸法値が混み合う形 (8.25 の #5-#8 付近と同じ性質)。 */
    private val crowded = "1,4.0,1.15,3.9,-1,-1" + NL +
        "2,1.15,0.9,1.0,1,1" + NL +
        "3,3.9,2.04,4.45,1,2" + NL +
        "4,2.04,1.0,2.5,3,1" + NL

    @Test
    fun `一番短い辺の寸法値は退避対象にしない`() {
        val list = listOf(crowded)
        val moves = DimensionTextEscape.solve(list, textSize = 0.525f)

        for (move in moves) {
            val tri = list.getBy(move.shapeNumber)
            if (move.side !in 0..2) continue
            val lengths = kotlin.collections.listOf(tri.lengthA, tri.lengthB, tri.lengthC)
            assertTrue(
                lengths[move.side] > lengths.min() + 1e-6f,
                "一番短い辺 (#${move.shapeNumber} side=${move.side}, 長さ ${lengths[move.side]}) を動かした",
            )
        }
    }

    @Test
    fun `短辺を除いても他の辺は従来どおり退避できる`() {
        // 「動かさない」を入れた結果、退避そのものが死んでいないことの担保
        val list = listOf(crowded)
        val moves = DimensionTextEscape.solve(list, textSize = 0.525f)
        assertTrue(moves.isNotEmpty(), "短辺を外したら 1 件も退避しなくなった")
    }

    @Test
    fun `正三角形は 3 辺とも同じ長さなので 1 本も動かさない`() {
        // 最短が一意に決まらない形では全辺が「最短」扱い = 触らない。
        // 動かす余地の無さは同じで、恣意的にどれか 1 本だけ動かす理由が無い
        val list = listOf("1,3.0,3.0,3.0,-1,-1" + NL + "2,3.0,3.0,3.0,1,1" + NL)
        val moves = DimensionTextEscape.solve(list, textSize = 2.0f)
        assertEquals(0, moves.count { it.side in 0..2 }, "正三角形の辺を動かした: $moves")
    }
}
