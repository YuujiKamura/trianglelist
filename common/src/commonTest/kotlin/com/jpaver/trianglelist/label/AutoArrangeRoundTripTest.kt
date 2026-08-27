package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.datamanager.HeaderValues
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 自動配置の結果は pointnumber / dimHorizontal という**CSV に載るフィールド**に入る。
 * 保存 → 読み込み → 再配置で同じ図面になること (往復で配置が動かないこと) を固定する。
 *
 * ここが崩れると「保存して開き直すたびに寸法の位置が変わる」になる ── リリース前に
 * 一番踏みたくない類のバグなので、エッジケース網羅の一部として置く。
 */
class AutoArrangeRoundTripTest {

    private val NL = 10.toChar().toString()

    private fun chain(n: Int): String = buildString {
        append("1,4.0,3.0,3.5,-1,-1").append(NL)
        for (i in 2..n) append("$i,3.0,2.8,3.2,${i - 1},${if (i % 2 == 0) 1 else 2}").append(NL)
    }

    private fun load(csv: String): Pair<TriangleList, CsvCodec.CsvDoc> {
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc, applyRecoverState = false)
        CsvCodec.applyListParams(doc, trilist) { }
        trilist.recoverState(PointXY(0f, 0f))
        trilist.arrangePointNumbers()
        return trilist to doc
    }

    private fun save(trilist: TriangleList, original: CsvCodec.CsvDoc): String =
        CsvCodec.serialize(
            CsvCodec.bake(
                trilist,
                DeductionList(),
                HeaderValues("工事", "路線", "業者", "1/1"),
                original,
                1f,
            ),
        )

    private fun placement(list: TriangleList): String = buildString {
        list.forEachItem { t ->
            t as Triangle
            append(t.mynumber).append(':')
            append("%.4f,%.4f".format(t.pointnumber.x, t.pointnumber.y)).append(':')
            append(t.dimHorizontal.a).append(t.dimHorizontal.b).append(t.dimHorizontal.c).append('|')
        }
    }

    @Test
    fun `保存して開き直しても配置が変わらない`() {
        for (ts in kotlin.collections.listOf(0.25f, 0.525f)) {
            val (first, doc) = load(chain(8))
            first.arrangeLabelsWithoutCollision(ts)
            val before = placement(first)

            val csv = save(first, doc)
            val (second, _) = load(csv)
            second.arrangeLabelsWithoutCollision(ts)

            assertEquals(before, placement(second), "ts=$ts で往復後に配置が変わった")
        }
    }

    @Test
    fun `往復を 2 回繰り返しても変わらない`() {
        val ts = 0.525f
        var (list, doc) = load(chain(6))
        list.arrangeLabelsWithoutCollision(ts)
        val first = placement(list)

        repeat(2) {
            val csv = save(list, doc)
            val loaded = load(csv)
            list = loaded.first
            doc = loaded.second
            list.arrangeLabelsWithoutCollision(ts)
        }

        assertEquals(first, placement(list), "往復を重ねると配置がずれていく")
    }
}
