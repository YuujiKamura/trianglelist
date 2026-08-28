package com.jpaver.trianglelist

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.label.DimensionTextEscape
import com.jpaver.trianglelist.label.LabelArrangeReset
import com.jpaver.trianglelist.label.ModelOverlapAnalyzer
import com.jpaver.trianglelist.label.NumberCircleEscape
import com.jpaver.trianglelist.scale.TextSizePolicy
import org.junit.Test
import java.io.File
import kotlin.math.sqrt
import kotlin.test.assertTrue

/**
 * 放射図形の旗揚げは**収束点から遠い側**へ出す (2026-08-28 user「１３，１４のような
 * 放射図形だと、旗揚げを収束点側にするとどの辺に対応してるのかみづらくなったりする」)。
 *
 * 扇状に並ぶと多数の辺が 1 つの頂点 (収束点) に集まる。旗揚げは辺の延長線上へ出すので、
 * 収束点側の端へ出すと全部の辺の寸法値が要に集まり、どれがどの辺の寸法か引出線を辿らないと
 * 分からなくなる。反対の端へ出せば辺ごとに散る。
 *
 * 衝突数では測れない (収束点の外側は図形が無いので box は空いており、機械的には
 * 「衝突ゼロの良い解」に見える)。悪いのは帰属の読みやすさなので、候補の**順序**で守る。
 *
 * 合成の扇 (等分・二等辺) では従来の順序でもたまたま遠い側に出るため再現しない。
 * 実際の路面の区割り (samples/8.25_bad.csv、#12-#16 が 1 点に集まる) で見る
 * (2026-08-28 user「実際の路面の区割りをもとに考えていかないと空想にしかならない」)。
 */
class DimensionFlagAwayFromHubTest {

    private fun repoRoot(): File {
        var d: File? = File(System.getProperty("user.dir")).absoluteFile
        while (d != null) { if (File(d, "settings.gradle.kts").exists()) return d; d = d.parentFile }
        error("repo root not found")
    }

    private fun load(): Pair<TriangleList, Float> {
        val csv = File(repoRoot(), "samples/8.25_bad.csv")
            .readText(java.nio.charset.Charset.forName("MS932"))
        val list = CsvCodec.build(CsvCodec.parse(csv))
        val ts = TextSizePolicy.paperMmToModelSize(3.5f, list.getPrintScale(1f) * 100f)
        return list to ts
    }

    private fun dist(ax: Double, ay: Double, bx: Double, by: Double) =
        sqrt((ax - bx) * (ax - bx) + (ay - by) * (ay - by))

    /** 3 つ以上の辺が集まっている頂点をすべて返す (= 収束点)。 */
    private fun hubs(list: TriangleList): List<Pair<Double, Double>> {
        val counts = mutableMapOf<Pair<Long, Long>, Int>()
        list.forEachItem { s ->
            for (e in s.edges()) {
                for (p in listOf(e.left, e.right)) {
                    val k = (p.x * 1e3).toLong() to (p.y * 1e3).toLong()
                    counts[k] = (counts[k] ?: 0) + 1
                }
            }
        }
        return counts.filter { it.value >= 8 }.map { it.key.first / 1e3 to it.key.second / 1e3 }
    }

    private fun centerOf(list: TriangleList, num: Int, side: Int, h: Int, ts: Float): Pair<Double, Double> {
        val tri = list.getBy(num)
        val keep = when (side) { 0 -> tri.dimHorizontal.a; 1 -> tri.dimHorizontal.b; else -> tri.dimHorizontal.c }
        when (side) { 0 -> tri.dimHorizontal.a = h; 1 -> tri.dimHorizontal.b = h; else -> tri.dimHorizontal.c = h }
        val box = ModelOverlapAnalyzer.boxesOf(tri, num, ts).first { it.first == "dim:$num:$side" }.second
        when (side) { 0 -> tri.dimHorizontal.a = keep; 1 -> tri.dimHorizontal.b = keep; else -> tri.dimHorizontal.c = keep }
        return box.center.x to box.center.y
    }

    @Test
    fun `実データの旗揚げは収束点から遠い側の端へ出る`() {
        val (list, ts) = load()
        LabelArrangeReset.reset(list)
        NumberCircleEscape.apply(list, NumberCircleEscape.solve(list, ts))
        val hubs = hubs(list)
        assertTrue(hubs.isNotEmpty(), "収束点が 1 つも見つからない (サンプルが違う)")

        val moves = DimensionTextEscape.solve(list, ts)
        val flagged = moves.filter { it.to == 3 || it.to == 4 }
        assertTrue(flagged.isNotEmpty(), "旗揚げが 1 件も出ていない: $moves")

        val bad = mutableListOf<String>()
        for (m in flagged) {
            if (m.side !in 0..2) continue
            val other = if (m.to == 3) 4 else 3
            val (cx, cy) = centerOf(list, m.shapeNumber, m.side, m.to, ts)
            val (ox, oy) = centerOf(list, m.shapeNumber, m.side, other, ts)
            // その辺の端点が収束点である場合だけを見る (無関係な頂点を拾わない)
            val tri = list.getBy(m.shapeNumber)
            val edge = tri.edges()[m.side]
            val hub = hubs.firstOrNull { h ->
                listOf(edge.left, edge.right).any {
                    kotlin.math.abs(it.x - h.first) < 1e-2 && kotlin.math.abs(it.y - h.second) < 1e-2
                }
            } ?: continue
            val chosen = dist(cx, cy, hub.first, hub.second)
            val alt = dist(ox, oy, hub.first, hub.second)
            // 収束点の近くに出た側だけ問題にする (両方遠いなら帰属は読める)
            if (chosen < alt) bad.add("#${m.shapeNumber} side=${m.side} 選=%.2f 他=%.2f".format(chosen, alt))
        }
        assertTrue(bad.isEmpty(), "収束点側へ旗揚げした: $bad")
    }
}
