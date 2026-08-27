package com.jpaver.trianglelist.label

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.calcPoints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 自動退避させた番号位置が、図形の再計算・クローンを跨いで残ること。
 *
 * 2026-08-27 に踏んだ実バグ: 退避は掛かっていた (moves=14、モデル上の pointnumber も
 * 動いていた) のに DXF が 1 バイトも変わらなかった。原因は calcPoints() の
 * 「isMovedByUser でなければ pointnumber = pointcenter」── 書き出し時のクローン経路で
 * 再計算が走り、退避結果が既定位置へ戻されていた。
 *
 * isMovedByUser を立てて誤魔化さない (動かしたのは自動処理であって user ではない、
 * CSV の永続化やアプリの「ユーザーが動かした」判定と意味が混ざる)。退避専用の
 * フラグを持たせて、そこだけ再計算の対象外にする。
 */
class NumberEscapePersistTest {

    private fun triangle(): Triangle {
        val csv = "1,1.0,0.9,0.8,-1,-1" + 10.toChar()
        val doc = CsvCodec.parse(csv)
        return CsvCodec.build(doc).getBy(1)
    }

    @Test
    fun `退避した番号は再計算で既定位置に戻らない`() {
        val tri = triangle()
        val escaped = tri.pointcenter.plus(0.3, 0.2)
        tri.pointnumber = escaped
        tri.pointNumber.flag.isEscaped = true

        tri.calcPoints()

        assertEquals(escaped.x, tri.pointnumber.x, 1e-9, "退避位置が再計算で戻された")
        assertEquals(escaped.y, tri.pointnumber.y, 1e-9, "退避位置が再計算で戻された")
    }

    @Test
    fun `退避していない番号は従来どおり重心に戻る`() {
        val tri = triangle()
        tri.pointnumber = tri.pointcenter.plus(0.3, 0.2)

        tri.calcPoints()

        assertEquals(tri.pointcenter.x, tri.pointnumber.x, 1e-9)
        assertEquals(tri.pointcenter.y, tri.pointnumber.y, 1e-9)
    }

    @Test
    fun `退避フラグはクローンで引き継がれる`() {
        val tri = triangle()
        tri.pointNumber.flag.isEscaped = true

        val copy = tri.clone()

        assertTrue(copy.pointNumber.flag.isEscaped, "クローンで退避フラグが落ちると書き出しで戻される")
    }
}
