package com.jpaver.trianglelist.label

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.EditList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 番号サークルの退避 (2026-08-27 user 指示):
 *   「番号サークルを、サイドの辺の方向にスライドさせれば寸法値を置くスペースが出来ることが多い」
 *   「それでも寸法値が収まらない場合は、いっそ番号を外に出す」
 *   「簡単なのはそのままスライド方向に外に出してしまう事」
 *
 * → 内側スライドと外への旗揚げを**同じ 1 本の梯子**にする。方向を選び、距離を伸ばしていき、
 *   図形から出た時点でそれが旗揚げ (引出線は既存の描画が自動で出す ── 番号が図形外なら
 *   重心から矢印を引く、WebPrimitiveRenderer / DrawingFileWriter.pointNumberPrims)。
 *
 * 退避は「衝突している時だけ」動く。既存の autoAlign (面積・辺長の閾値で動く) とは
 * 発火条件が別 ── 可読サイズの文字は、閾値を通る普通の三角形でも番号にぶつかる。
 */
class NumberCircleEscapeTest {

    /** JIS 3.5mm を 1/150 で model に直した大きさ (TextSizePolicy.dimensionModelSize(150f))。
     *  可読サイズでないと番号サークルの干渉が再現しない ── 現行サイズは基準の 2.1 倍小さい。 */
    private val JIS_TEXT_SIZE = 0.525f

    private val NL = 10.toChar().toString()

    private fun buildList(csv: String): EditList<CycleShape> {
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        return CsvCodec.buildMixed(doc, trilist, 1f)
    }

    private fun circleCollisions(list: EditList<CycleShape>, textSize: Float): List<OverlapPair> =
        ModelOverlapAnalyzer.analyze(list, textSize = textSize).pairs
            .filter { it.otherKind == ObstacleKind.CIRCLE }

    @Test
    fun `衝突が無ければ 1 つも動かさない`() {
        val list = buildList("1,100.0,100.0,100.0,-1,-1\n")

        val moves = NumberCircleEscape.solve(list, textSize = 0.25f)

        assertTrue(moves.isEmpty(), "余裕のある図形で番号を動かすのはおかしい: $moves")
    }

    @Test
    fun `同じ入力なら同じ結果 (決定的)`() {
        // golden test は「同じ CSV → 同じ図面」を前提にしている。候補の走査順は
        // 辺の並び + 距離の昇順で固定されていて、乱択は入れない
        val list1 = buildList("1,1.0,0.9,0.8,-1,-1" + NL)
        val list2 = buildList("1,1.0,0.9,0.8,-1,-1" + NL)

        val a = NumberCircleEscape.solve(list1, textSize = JIS_TEXT_SIZE)
        val b = NumberCircleEscape.solve(list2, textSize = JIS_TEXT_SIZE)

        assertEquals(a.toString(), b.toString(), "同じ入力で結果が変わるのは決定的でない")
    }

    @Test
    fun `旗揚げを許さない設定では図形の外へ出さない`() {
        // 実データの干渉はスライドで足りる (user 2026-08-27)。外に出すと引出線が要り、
        // 番号の帰属が引出線頼みになるので既定では出さない
        val list = buildList("1,1.0,0.9,0.8,-1,-1" + NL)

        val moves = NumberCircleEscape.solve(list, textSize = JIS_TEXT_SIZE, allowFlagOut = false)

        assertTrue(moves.none { it.isFlagOut }, "allowFlagOut=false なのに外へ出た: $moves")
    }

    @Test
    fun `退避しても寸法値どうしの衝突を増やさない`() {
        // 番号を動かした先で別の寸法値を押し出す、のような二次被害が無いこと
        val list = buildList("1,1.0,0.9,0.8,-1,-1" + NL)
        val before = ModelOverlapAnalyzer.analyze(list, textSize = JIS_TEXT_SIZE).pairs
            .count { it.otherKind == ObstacleKind.LABEL }

        NumberCircleEscape.apply(list, NumberCircleEscape.solve(list, textSize = JIS_TEXT_SIZE))
        val after = ModelOverlapAnalyzer.analyze(list, textSize = JIS_TEXT_SIZE).pairs
            .count { it.otherKind == ObstacleKind.LABEL }

        assertTrue(after <= before, "退避で文字どうしの衝突が増えた: before=$before after=$after")
    }
}
