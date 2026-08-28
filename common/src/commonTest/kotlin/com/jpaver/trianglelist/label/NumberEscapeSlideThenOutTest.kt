package com.jpaver.trianglelist.label

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 番号サークルを動かす段取り (2026-08-28 user 確定):
 *
 *   1. 余裕があるなら**動かさない** ── 「今ビューワに乗ってるケースだと、ぜんぜん
 *      余裕があるので寄せる必要がない」。衝突していない番号に手を出さない
 *   2. 衝突したら図形内をスライドして解く ── 「もちろんスライドで賄える時はそれでいい」
 *   3. 図形内で解けない (面積がきつい) なら**外に出す** ── 「いっそ外に出すっていうので
 *      統一したほうが良い」「面積がきつくなってから外に出すのを考える」
 *
 * 一度は「番号を無条件に外周側へ寄り切らせる」実装を入れたが撤回した (2026-08-28)。
 * 余裕のある図面まで動かしてしまい、寄せる理由が無い所で位置が変わるため。
 * 判断の分かれ目は面積であって、形の細さそのものではない。
 *
 * 内外を 1 つの探索に混ぜてはいけない: 距離の小さい順に見ると「図形のすぐ外」が
 * 「図形内の少し遠い所」より先に当たり、まだ中に置けるのに引出線が出る。
 * 段を分けて、内側を全距離試してから外に出る。
 */
class NumberEscapeSlideThenOutTest {

    private val NL = 10.toChar().toString()

    private fun listOf(csv: String): TriangleList = CsvCodec.build(CsvCodec.parse(csv))

    /** 余裕のある普通の図形。1:50 の JIS 3.5mm 相当。 */
    private val roomy = "1,6.0,5.0,4.0,-1,-1" + NL +
        "2,5.0,4.0,3.5,1,1" + NL +
        "3,4.0,3.5,3.0,1,2" + NL

    /** 面積がきつい図形 (歩道巻き込みの細いスライス)。 */
    private val tight = "1,3.0,3.0,0.78,-1,-1" + NL +
        "2,3.0,3.0,0.78,1,1" + NL +
        "3,3.0,3.0,0.78,2,1" + NL

    @Test
    fun `余裕がある図形の番号は動かさない`() {
        val list = listOf(roomy)
        val before = mutableListOf<Pair<Double, Double>>()
        list.forEachItem { before.add((it as Triangle).pointnumber.x to it.pointnumber.y) }

        val moves = NumberCircleEscape.solve(list, textSize = 0.175f)

        assertTrue(moves.isEmpty(), "衝突していないのに番号を動かした: $moves")
        val after = mutableListOf<Pair<Double, Double>>()
        list.forEachItem { after.add((it as Triangle).pointnumber.x to it.pointnumber.y) }
        assertTrue(before == after, "solve が呼ぶだけでモデルを動かした")
    }

    @Test
    fun `面積がきつくて図形内に解が無ければ外に出す`() {
        // 文字を上げていくとスライスの中に番号サークルの置き場が無くなる。
        // そこで諦めるのではなく引出線付きで外へ出す = 「外に出すで統一」
        val list = listOf(tight)
        val moves = NumberCircleEscape.solve(list, textSize = 0.5f)

        assertTrue(moves.isNotEmpty(), "面積がきついのに 1 つも退避していない")
        assertTrue(moves.all { it.isFlagOut }, "図形内に解が無いのに外へ出していない: $moves")
    }

    @Test
    fun `図形内で解けるうちは外に出さない`() {
        // 内外を混ぜて探索すると「図形のすぐ外」が先に当たって引出線が出てしまう。
        // 内側を全距離試し切ってから外、の順序が守られていることを見る
        val list = listOf(tight)
        val moves = NumberCircleEscape.solve(list, textSize = 0.5f, allowFlagOut = false)

        // 外禁止なら 1 件も出ない (= 上のテストで出た解は全部「図形外」だった)
        assertTrue(moves.isEmpty(), "図形内に解が無いはずなのに内側で解けた: $moves")
    }

    @Test
    fun `外に出した番号も他のラベルに当たらない`() {
        val list = listOf(tight)
        val moves = NumberCircleEscape.solve(list, textSize = 0.5f)
        NumberCircleEscape.apply(list, moves)

        val judge = 0.5f * (1f + NumberCircleEscape.DEFAULT_CLEARANCE)
        val boxes = ModelOverlapAnalyzer.boxes(list, judge)
        val radius = NumberCircleEscape.circleRadius(judge)
        list.forEachItem { shape ->
            val at = shape.pointNumberAnchor()
            val hit = boxes.any { it.second.penetrationDepthCircle(at, radius) != null }
            assertTrue(!hit, "退避後も番号が寸法値に当たっている (#${(shape as Triangle).mynumber})")
        }
    }
}
