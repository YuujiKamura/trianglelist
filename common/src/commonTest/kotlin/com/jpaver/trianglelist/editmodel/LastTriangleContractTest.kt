package com.jpaver.trianglelist.editmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 「最後にタップされた三角形」問い合わせの契約。
 *
 * 呼び出し元は MyView の「画面の中心をどこに据えるか」だけ (MyView.kt:472,495)。
 * 空リストや陳腐化した lastTapNumber は異常ではなく「中心に据える三角形が無い」という
 * 正当な状態なので null で返す。例外にすると View 初期化順のズレがそのまま落ちる原因になり、
 * 実際 onResume に復元処理をコピーする回避策 (7f204c15, 2025-04-22) の引き金になっていた。
 */
class LastTriangleContractTest {

    private fun listOf3(): TriangleList {
        val t = TriangleList(Triangle(6f, 5f, 4f))
        t.add(Triangle(5f, 4f, 3f), true)
        t.add(Triangle(4f, 3.5f, 3f), true)
        return t
    }

    @Test
    fun `空リストは null を返す`() {
        assertNull(TriangleList().lastTriangleOrNull())
    }

    @Test
    fun `未タップ (0 以下) はリスト末尾を返す`() {
        val t = listOf3()
        t.lastTapNumber = 0
        assertEquals(t.getBy(t.size()), t.lastTriangleOrNull())
        t.lastTapNumber = -1
        assertEquals(t.getBy(t.size()), t.lastTriangleOrNull())
    }

    @Test
    fun `有効な番号はその三角形を返す`() {
        val t = listOf3()
        for (n in 1..t.size()) {
            t.lastTapNumber = n
            assertEquals(t.getBy(n), t.lastTriangleOrNull(), "lastTapNumber=$n")
        }
    }

    @Test
    fun `範囲外の番号は例外ではなく null`() {
        val t = listOf3()
        // 削除などで lastTapNumber が陳腐化した状態
        t.lastTapNumber = t.size() + 1
        assertNull(t.lastTriangleOrNull())
        t.lastTapNumber = 999
        assertNull(t.lastTriangleOrNull())
    }
}
