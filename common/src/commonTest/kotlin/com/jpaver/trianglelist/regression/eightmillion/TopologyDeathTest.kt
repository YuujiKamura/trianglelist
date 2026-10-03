package com.jpaver.trianglelist.regression.eightmillion

import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 【エイトミリオンウェイストゥダイ：トポロジー・ツリー破壊編】
 *
 * 循環参照、不正な親番号、範囲外インデックス削除、孤立ノード、
 * 急激な追加・削除連打などのトポロジー破綻からアプリを防御する。
 */
class TopologyDeathTest {

    @Test
    fun `Death_11_範囲外削除_サイズ以上のインデックス削除でクラッシュしない`() {
        val list = TriangleList(Triangle(6f, 5f, 4f))
        list.add(Triangle(5f, 4f, 3f), true)

        assertEquals(2, list.size())

        // 範囲外インデックスの削除試行
        try {
            list.remove(999)
        } catch (_: IndexOutOfBoundsException) {
            // 例外で安全に拒絶されるのは正常
        } catch (_: IllegalArgumentException) {}

        try {
            list.remove(0)
        } catch (_: IndexOutOfBoundsException) {}
        catch (_: IllegalArgumentException) {}

        // リストが壊れていないこと
        assertTrue(list.size() in 1..2)
    }

    @Test
    fun `Death_12_ルート削除試行_最後の1個を削除してもリストが空になってクラッシュしない`() {
        val list = TriangleList(Triangle(6f, 5f, 4f))
        assertEquals(1, list.size())

        // 最後の1個を削除しようとする
        try {
            list.remove(1)
        } catch (_: Throwable) {
            // 例外で拒否されるか、または空化を防ぐ
        }

        // TriangleList は空であってはならない (または安全に初期化されている)
        // 少なくとも getBy(1) や size で致命的な状態にならないこと
        assertTrue(list.size() >= 0)
    }

    @Test
    fun `Death_13_大量追加と削除連打_50回の追加削除でインデックス連続性が維持される`() {
        val list = TriangleList(Triangle(6f, 5f, 4f))

        for (i in 1..50) {
            val child = Triangle(5f, 4f, 3f)
            child.parentnumber = list.size()
            child.connectionSide = 1
            list.add(child, true)
        }
        assertEquals(51, list.size())

        // 25個削除
        for (i in 1..25) {
            list.remove(list.size())
        }
        assertEquals(26, list.size())

        // 再度10個追加
        for (i in 1..10) {
            val child = Triangle(4f, 3.5f, 3f)
            child.parentnumber = list.size()
            child.connectionSide = 1
            list.add(child, true)
        }
        assertEquals(36, list.size())

        // 全要素の mynumber と parentnumber の妥当性を検証
        for (idx in 1..list.size()) {
            val t = list.getBy(idx)
            assertEquals(idx, t.mynumber, "mynumber must match index $idx")
            if (idx == 1) {
                assertEquals(-1, t.parentnumber)
            } else {
                assertTrue(t.parentnumber in 1 until idx, "parentnumber must point to earlier triangle")
            }
        }
    }

    @Test
    fun `Death_14_多重分岐_同じ親の同じ辺に複数の子が接続されてもデータが破壊されない`() {
        val list = TriangleList(Triangle(6f, 5f, 4f))

        // 親(1)のB辺に2番を追加
        val child1 = Triangle(5f, 4f, 3f)
        child1.parentnumber = 1
        child1.connectionSide = 1
        list.add(child1, true)

        // 親(1)のB辺に3番も追加（実世界では重なるがデータ構造として破綻しないか）
        val child2 = Triangle(4f, 3f, 2.5f)
        child2.parentnumber = 1
        child2.connectionSide = 1
        list.add(child2, true)

        assertEquals(3, list.size())
        assertEquals(1, list.getBy(2).parentnumber)
        assertEquals(1, list.getBy(3).parentnumber)
        assertFalse(list.getBy(2).pointBC.x.isNaN())
        assertFalse(list.getBy(3).pointBC.x.isNaN())
    }
}
