package com.jpaver.trianglelist.label

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 自動配置 (TriangleList.arrangeLabelsWithoutCollision) のエッジケース。
 *
 * これは**プロダクト経路**に入っている: attachToTheView が呼ぶので、アプリの
 * モデル更新のたびに走る。回転操作では毎秒以下の間隔で呼ばれる (MyView.kt:434 の
 * コメント) ため、下記が壊れると図面が操作のたびにジワジワ動く/落ちる。
 *
 * 軸: 図形数 (0/1/大量) × 文字サイズ (0/極小/通常/極大) × 形状 (通常/極小/潰れ)
 *     × 繰り返し回数 (1/2/3) × user 配置の有無。
 */
class AutoArrangeEdgeCaseTest {

    private val NL = 10.toChar().toString()

    private fun listOf(csv: String): TriangleList = CsvCodec.build(CsvCodec.parse(csv))

    private fun snapshot(list: TriangleList): String = buildString {
        list.forEachItem { t ->
            t as Triangle
            append(t.mynumber).append(':')
            append("%.6f,%.6f".format(t.pointnumber.x, t.pointnumber.y)).append(':')
            append(t.dimHorizontal.a).append(t.dimHorizontal.b).append(t.dimHorizontal.c)
            append(t.dimHorizontal.s).append('|')
        }
    }

    private fun chain(n: Int): String = buildString {
        append("1,4.0,3.0,3.5,-1,-1").append(NL)
        for (i in 2..n) {
            val conn = if (i % 2 == 0) 1 else 2
            append("$i,3.0,2.8,3.2,${i - 1},$conn").append(NL)
        }
    }

    @Test
    fun `繰り返し呼んでも配置が動き続けない (冪等)`() {
        // attachToTheView は回転操作で連続的に呼ばれる。2 回目以降で配置が変わると
        // 図面が操作のたびに動いて見える
        for (ts in kotlin.collections.listOf(0.25f, 0.525f, 1.0f)) {
            val list = listOf(chain(8))
            list.arrangeLabelsWithoutCollision(ts)
            val once = snapshot(list)
            list.arrangeLabelsWithoutCollision(ts)
            val twice = snapshot(list)
            list.arrangeLabelsWithoutCollision(ts)
            val thrice = snapshot(list)

            assertEquals(once, twice, "ts=$ts で 2 回目に配置が動いた")
            assertEquals(twice, thrice, "ts=$ts で 3 回目に配置が動いた")
        }
    }

    @Test
    fun `図形が 0 件 1 件でも落ちない`() {
        TriangleList().arrangeLabelsWithoutCollision(0.525f)
        listOf("1,3.0,3.0,3.0,-1,-1" + NL).arrangeLabelsWithoutCollision(0.525f)
    }

    @Test
    fun `文字サイズが 0 や負なら何もしない`() {
        val list = listOf(chain(4))
        val before = snapshot(list)
        list.arrangeLabelsWithoutCollision(0f)
        list.arrangeLabelsWithoutCollision(-1f)
        assertEquals(before, snapshot(list), "サイズ未確定なのに配置を動かした")
    }

    @Test
    fun `極小や潰れた三角形でも例外を出さず NaN を作らない`() {
        val csv = "1,0.05,0.05,0.05,-1,-1" + NL +
            "2,0.05,0.001,0.05,1,1" + NL +
            "3,0.05,3.0,3.0,1,2" + NL
        val list = listOf(csv)

        list.arrangeLabelsWithoutCollision(0.525f)

        list.forEachItem { t ->
            t as Triangle
            assertTrue(t.pointnumber.x.isFinite() && t.pointnumber.y.isFinite(), "番号が NaN: #${t.mynumber}")
            for (h in kotlin.collections.listOf(t.dimHorizontal.a, t.dimHorizontal.b, t.dimHorizontal.c)) {
                assertTrue(h in 0..4, "horizontal が範囲外: #${t.mynumber} $h")
            }
        }
    }

    @Test
    fun `極端な文字サイズでも範囲外の値を作らない`() {
        for (ts in kotlin.collections.listOf(0.0001f, 0.01f, 5.0f, 50f)) {
            val list = listOf(chain(5))
            list.arrangeLabelsWithoutCollision(ts)
            list.forEachItem { t ->
                t as Triangle
                assertTrue(t.pointnumber.x.isFinite(), "ts=$ts で番号が NaN")
                assertTrue(t.dimHorizontal.a in 0..4 && t.dimHorizontal.b in 0..4 && t.dimHorizontal.c in 0..4,
                    "ts=$ts で horizontal が範囲外")
            }
        }
    }

    @Test
    fun `user が動かした配置は繰り返し呼んでも保たれる`() {
        val list = listOf(chain(6))
        // 印を付ける対象と読み出す対象は同じ引き方で取る (mynumber と並び順は別物)
        val target = list.getBy(3)
        target.pointnumber = target.pointcenter.plus(0.4, 0.3)
        target.pointNumber.flag.isMovedByUser = true
        target.dimHorizontal.b = 4
        target.dim.flag[1].isMovedByUser = true
        val kept = target.pointnumber.clone()

        repeat(3) { list.arrangeLabelsWithoutCollision(0.525f) }

        val after = list.getBy(3)
        assertEquals(kept.x, after.pointnumber.x, 1e-9, "user 配置の番号が動かされた")
        assertEquals(kept.y, after.pointnumber.y, 1e-9, "user 配置の番号が動かされた")
        assertEquals(4, after.dimHorizontal.b, "user 操作の旗揚げが変えられた")
    }
    @Test
    fun `大規模データ(100件)でも正常に動作する`() {
        val list = listOf(chain(100))
        list.arrangeLabelsWithoutCollision(0.525f)
        list.forEachItem { t ->
            t as Triangle
            assertTrue(t.pointnumber.x.isFinite(), "番号が NaN")
        }
    }

    @Test
    fun `台形混在(conn=0)でも処理可能`() {
        val csv = "1,3.0,3.0,3.0,-1,-1" + NL +
            "2,3.0,2.8,3.2,1,0" + NL + // conn=0 (台形)
            "3,3.0,2.8,3.2,2,0" + NL
        val list = listOf(csv)
        list.arrangeLabelsWithoutCollision(0.525f)
        list.forEachItem { t ->
            t as Triangle
            assertTrue(t.pointnumber.x.isFinite(), "番号が NaN")
        }
    }
}
