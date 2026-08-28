package com.jpaver.trianglelist.editmodel

import com.jpaver.trianglelist.datamanager.CsvCodec
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * 番号サークルの既定位置を「三角形の中で一番余裕のある点」= 内心 (incenter) に置く
 * (2026-08-28 user「最初にすべきは三角形の重心比重と面積に沿って番号サークルを寄せる処理」)。
 *
 * 従来は [PointNumberManager.weightedMidpoint] が「頂点の角度 + 定数 35」を重みにしていた。
 * 向きの勘所 (広い角の方へ寄る) は合っているが、35 は magic number で効きを鈍らせるうえ、
 * 「番号サークルという**円**が入るか」とは無関係の量で位置を決めている。
 *
 * 内心を正にする理由は定義そのもの: 内心は 3 辺からの最短距離が最大になる点であり、
 * = その三角形に描ける最大の円の中心。番号サークルを置くのに最も余裕がある点が欲しい、
 * という要求と一対一で対応する。しかも
 *   - 位置は辺長比重 (a·A + b·B + c·C) / (a+b+c) で出る
 *   - 半径は r = 面積 / 半周長 で出る = 「そもそも番号サークルが図形内に入るか」の判定
 * となり、位置と可否が同じ 1 つの幾何から出る (面積と辺長比重の両方を使うのはこのため)。
 *
 * 実害の出どころは歩道の巻き込み (samples/makikomi_r3.csv): 半径 3m の扇を細い三角形で
 * 割った形では、番号が内寄りに留まって寸法値と密集し、寸法値の側が旗揚げに追い込まれる。
 * 旗揚げは辺の延長線上へ出るので、細長い三角形では引出線が図形の数倍に伸びて図面が壊れる
 * (2026-08-28 実測: 10 分割版で intrusion 1 → 3 と悪化)。番号が先に一番広い所へ退けば、
 * 寸法値は図形内に収まる。
 */
class PointNumberIncenterTest {

    private val NL = 10.toChar().toString()

    private fun triangleOf(csv: String): Triangle {
        val list = CsvCodec.build(CsvCodec.parse(csv))
        list.arrangePointNumbers()
        return list.getBy(1)
    }

    /** 内心 = (a·A + b·B + c·C) / (a+b+c)。a は頂点 A の対辺の長さ。 */
    private fun incenterOf(t: Triangle): Pair<Double, Double> {
        // 頂点 pointAB は辺 C の対、pointBC は辺 A の対、pointCA は辺 B の対
        val wApex = t.lengthC.toDouble()
        val wBC = t.lengthA.toDouble()
        val wCA = t.lengthB.toDouble()
        val total = wApex + wBC + wCA
        val x = (t.pointAB.x * wApex + t.pointBC.x * wBC + t.pointCA.x * wCA) / total
        val y = (t.pointAB.y * wApex + t.pointBC.y * wBC + t.pointCA.y * wCA) / total
        return x to y
    }

    /** 点から線分 (p→q) までの距離。 */
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
        return (0..2).minOf { i ->
            val p = v[i]
            val q = v[(i + 1) % 3]
            distToSegment(p.x, p.y, q.x, q.y, x, y)
        }
    }

    @Test
    fun `正三角形では内心と重心が一致するので番号位置は動かない`() {
        // 既定位置の変更が「普通の三角形」を動かさないことの担保。
        // 巻き込みのために全図面の番号がずれるのでは代償が釣り合わない
        val t = triangleOf("1,4.0,4.0,4.0,-1,-1" + NL)
        val c = t.pointcenter
        assertEquals(c.x, t.pointnumber.x, 1e-3, "正三角形で番号が重心から外れた")
        assertEquals(c.y, t.pointnumber.y, 1e-3, "正三角形で番号が重心から外れた")
    }

    @Test
    fun `歩道巻き込みの細い三角形では番号が内心に来る`() {
        // samples/makikomi_r3.csv の 1 枚分 (半径 3m の扇を 6 分割、弧長 0.78、頂角 約 15 度)
        val t = triangleOf("1,3.0,3.0,0.78,-1,-1" + NL)
        val (ix, iy) = incenterOf(t)
        assertEquals(ix, t.pointnumber.x.toDouble(), 1e-3, "番号が内心に置かれていない (x)")
        assertEquals(iy, t.pointnumber.y.toDouble(), 1e-3, "番号が内心に置かれていない (y)")
    }

    @Test
    fun `細い三角形では番号のクリアランスが内接円半径になる`() {
        // 内心の定義そのもの: 3 辺からの最短距離 = 内接円半径 r = 面積 / 半周長。
        // 位置を「円が入る余裕」で決めている、を式ではなく実測で固定する
        val t = triangleOf("1,3.0,3.0,0.78,-1,-1" + NL)
        val s = (t.lengthA + t.lengthB + t.lengthC) / 2.0
        val r = t.getArea() / s
        val actual = clearance(t, t.pointnumber.x, t.pointnumber.y)
        assertEquals(r, actual, 1e-3, "番号位置の余裕が内接円半径と違う (r=$r actual=$actual)")
    }

    @Test
    fun `細い三角形では従来の角度重み付けより外周側へ寄る`() {
        // 従来 (角度 + 35) でも広い角の方へは寄るが足りない。巻き込みで詰まる原因なので、
        // 「内心の方が頂点から遠い」ことを不等式で固定する (数値そのものは形状依存)
        val t = triangleOf("1,3.0,3.0,0.78,-1,-1" + NL)
        val apex = t.pointAB
        val old = PointNumberManager().weightedMidpoint(t, 35f)
        val oldDist = sqrt(
            ((old.x - apex.x) * (old.x - apex.x) + (old.y - apex.y) * (old.y - apex.y)).toDouble()
        )
        val newDist = sqrt(
            ((t.pointnumber.x - apex.x) * (t.pointnumber.x - apex.x) +
                (t.pointnumber.y - apex.y) * (t.pointnumber.y - apex.y)).toDouble()
        )
        assertTrue(
            newDist > oldDist + 0.1,
            "内心が従来位置より外周側に来ていない (旧=$oldDist 新=$newDist)",
        )
    }

    @Test
    fun `内接円半径で番号サークルが図形内に入るかを判定できる`() {
        // 位置と「そもそも入るか」を同じ幾何から出す。入らない図形は自動退避が
        // 図形内をいくら探しても解けないので、探索前に分かる必要がある
        val wide = triangleOf("1,3.0,3.0,0.78,-1,-1" + NL)
        val sWide = (wide.lengthA + wide.lengthB + wide.lengthC) / 2.0
        val rWide = wide.getArea() / sWide

        val sliver = triangleOf("1,3.0,3.0,0.10,-1,-1" + NL)
        val sSliver = (sliver.lengthA + sliver.lengthB + sliver.lengthC) / 2.0
        val rSliver = sliver.getArea() / sSliver

        // 1:50 図面の JIS 3.5mm 文字 → 番号サークル半径 0.85 * 0.175 = 0.149 (model m)
        val circleRadius = 0.85 * 0.175
        assertTrue(rWide > circleRadius, "巻き込み R=3 / 弧長 0.78 は番号が入るはず (r=$rWide)")
        assertTrue(rSliver < circleRadius, "弧長 0.10 の潰れた三角形は番号が入らないはず (r=$rSliver)")
        assertTrue(abs(rWide - 0.342) < 0.01, "内接円半径の実測値が想定とずれた: $rWide")
    }
}
