package com.jpaver.trianglelist.label

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.PointNumberManager
import com.jpaver.trianglelist.editmodel.Triangle
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 番号サークルを「外周側へ目一杯」寄せる (2026-08-28 user「番号をもっと外周側に目一杯
 * スライドさせれば 3m を旗揚げしなくても中に置くスペースがあったりする」
 * 「ビューワに出てるサンプルだと寄せてるように見えない」)。
 *
 * 内心 (= その三角形に描ける最大の円の中心) は「一番余裕のある点」ではあるが、
 * **サークルが行ける限界ではない**。3,3,1 の三角形で内接円半径は 0.42m あるのに、
 * 番号サークルの半径は 1:50 の JIS 3.5mm で 0.149m しかない ── その差だけ、まだ
 * 短辺側へ寄れる (頂点から 85.7% → 94.9%)。寄り切って初めて、細長い三角形の
 * 内側に寸法値を置く余地が空く = 旗揚げ (辺の延長線上に飛ぶ、細長い figure では
 * 引出線が図形の数倍になる) を避けられる。
 *
 * 置き場所の定義: **サークルが収まる領域の中で、一番鋭い頂点から最も遠い点**。
 *   - 3,3,1 のような細い三角形 → 鋭い頂点は 3 と 3 が合う所、遠い点は短辺 (1) 側
 *   - 正三角形 → どの頂点も同じ鋭さなので退化し、内心 (= 重心) に戻る
 * magic number を持たない (寄せる量は文字サイズと形から一意に決まる)。
 */
class NumberFarthestSlideTest {

    private val NL = 10.toChar().toString()

    /** 1:50 図面・JIS 3.5mm の寸法値 → model 単位 0.175、番号サークル半径はその 0.85 倍。 */
    private val ts = 0.175f

    private fun triangleOf(csv: String): Triangle {
        val list = CsvCodec.build(CsvCodec.parse(csv))
        list.arrangePointNumbers()
        return list.getBy(1)
    }

    private fun distToSegment(px: Double, py: Double, qx: Double, qy: Double, x: Double, y: Double): Double {
        val dx = qx - px
        val dy = qy - py
        val len2 = dx * dx + dy * dy
        if (len2 == 0.0) return sqrt((x - px) * (x - px) + (y - py) * (y - py))
        var t = ((x - px) * dx + (y - py) * dy) / len2
        if (t < 0.0) t = 0.0
        if (t > 1.0) t = 1.0
        val cx = px + t * dx
        val cy = py + t * dy
        return sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy))
    }

    private fun clearance(t: Triangle, x: Double, y: Double): Double {
        val v = listOf(t.pointAB, t.pointBC, t.pointCA)
        return (0..2).minOf { i -> distToSegment(v[i].x, v[i].y, v[(i + 1) % 3].x, v[(i + 1) % 3].y, x, y) }
    }

    /** 頂点 (3 と 3 が合う所) から底辺中点までを 1.0 とした位置。 */
    private fun ratioFromApex(t: Triangle, x: Double, y: Double): Double {
        val apex = t.pointAB
        val baseMid = t.pointBC.calcMidPoint(t.pointCA)
        val h = sqrt(
            ((baseMid.x - apex.x) * (baseMid.x - apex.x) + (baseMid.y - apex.y) * (baseMid.y - apex.y)).toDouble()
        )
        return sqrt((x - apex.x) * (x - apex.x) + (y - apex.y) * (y - apex.y)) / h
    }

    @Test
    fun `3-3-1 では番号が短辺側へ寄り切る`() {
        val t = triangleOf("1,3.0,3.0,1.0,-1,-1" + NL)
        val p = NumberCircleEscape.farthestFromSharpestVertex(t, ts)
        val ratio = ratioFromApex(t, p.x, p.y)
        assertTrue(ratio > 0.93, "短辺側へ寄り切っていない (頂点から ${ratio * 100}%)")
        assertTrue(ratio < 1.0, "底辺を越えて図形の外へ出た (頂点から ${ratio * 100}%)")
    }

    @Test
    fun `寄り切った先でもサークルは図形内に収まる`() {
        val t = triangleOf("1,3.0,3.0,1.0,-1,-1" + NL)
        val p = NumberCircleEscape.farthestFromSharpestVertex(t, ts)
        val radius = NumberCircleEscape.circleRadius(ts)
        assertTrue(
            clearance(t, p.x, p.y) >= radius - 1e-6,
            "サークルが辺からはみ出した (余裕=${clearance(t, p.x, p.y)} 半径=$radius)",
        )
    }

    @Test
    fun `正三角形では退化して内心のまま動かない`() {
        // 「寄せる」が普通の三角形まで動かすと、巻き込みのために全図面が壊れる
        val t = triangleOf("1,4.0,4.0,4.0,-1,-1" + NL)
        val p = NumberCircleEscape.farthestFromSharpestVertex(t, ts)
        val inc = PointNumberManager().incenter(t)
        assertEquals(inc.x, p.x, 1e-3, "正三角形で番号が内心から動いた")
        assertEquals(inc.y, p.y, 1e-3, "正三角形で番号が内心から動いた")
    }

    @Test
    fun `文字が大きいほど寄れる量が減る`() {
        // 寄せ量は magic number ではなく「サークルが収まるか」で決まる。
        // 文字を大きくすれば収まる領域が狭まり、寄り切った位置は内心へ戻っていく
        val t = triangleOf("1,3.0,3.0,1.0,-1,-1" + NL)
        val small = ratioFromApex(t, NumberCircleEscape.farthestFromSharpestVertex(t, 0.125f).let { it.x }.toDouble(),
            NumberCircleEscape.farthestFromSharpestVertex(t, 0.125f).y.toDouble())
        val large = ratioFromApex(t, NumberCircleEscape.farthestFromSharpestVertex(t, 0.5f).let { it.x }.toDouble(),
            NumberCircleEscape.farthestFromSharpestVertex(t, 0.5f).y.toDouble())
        assertTrue(small > large, "文字を大きくしても寄せ量が変わらない (小=$small 大=$large)")
    }

    @Test
    fun `サークルが入らない三角形では内心へ落ちる`() {
        // 内接円半径 < サークル半径 = どこにも収まらない。無理に寄せず内心 (一番マシ) に置く。
        // 「図形外へ出す」判断は別 (退避の担当)
        val t = triangleOf("1,3.0,3.0,0.10,-1,-1" + NL)
        val p = NumberCircleEscape.farthestFromSharpestVertex(t, ts)
        val inc = PointNumberManager().incenter(t)
        assertEquals(inc.x, p.x, 1e-3, "収まらない三角形で内心に落ちていない")
        assertEquals(inc.y, p.y, 1e-3, "収まらない三角形で内心に落ちていない")
    }
}
