package com.jpaver.trianglelist.editmodel

import com.jpaver.trianglelist.datamanager.CsvCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

/**
 * クローンした三角形は**自分の寸法配置を持つ**こと。
 *
 * 2026-08-27 に踏んだ実バグ: Dims は horizontal/vertical/height を
 * `triangle.dimHorizontal` 等への proxy として持つが、Dims.clone() が
 * `Dims(triangle)` = **元の triangle** を掴んだまま複製していた。結果:
 *   - クローンの dim.horizontal を読むと元の三角形の値が返る
 *   - クローンの dimHorizontal (CycleShape 側の実体) は誰もコピーしないので既定値のまま
 *   - clone() 自体が元の triangle.dimHorizontal を別インスタンスに差し替える
 *
 * アプリ画面はモデルを clone してから描くため、自動配置がクローン側の dimHorizontal を
 * 書き換えても、描画が読む dim.horizontal は元の値 ── 「補正が画面に出ない」の正体。
 */
class TriangleDimsCloneTest {

    private fun triangle(): Triangle =
        CsvCodec.build(CsvCodec.parse("1,3.0,2.8,3.2,-1,-1" + 10.toChar())).getBy(1)

    @Test
    fun `クローンは値を引き継ぎつつ 自分の寸法配置を持つ`() {
        val original = triangle()
        original.dimHorizontal.c = 3
        original.dimVertical.b = 1
        original.dimHeight = 7f

        val copy = original.clone()

        assertEquals(3, copy.dim.horizontal.c, "クローンに値が引き継がれていない")
        assertEquals(3, copy.dimHorizontal.c, "クローンの実体に値が入っていない")
        assertEquals(1, copy.dim.vertical.b)
        assertEquals(7f, copy.dim.height)
        assertNotSame(original.dimHorizontal, copy.dimHorizontal, "同じインスタンスを共有している")
    }

    @Test
    fun `クローンを書き換えても元に影響しない`() {
        val original = triangle()
        original.dimHorizontal.c = 3
        val copy = original.clone()

        copy.dimHorizontal.c = 4

        assertEquals(4, copy.dim.horizontal.c, "クローンの読みが自分の値になっていない")
        assertEquals(3, original.dimHorizontal.c, "元の三角形が書き換わった")
        assertEquals(3, original.dim.horizontal.c, "元の三角形が書き換わった")
    }

    @Test
    fun `元を書き換えてもクローンに影響しない`() {
        val original = triangle()
        val copy = original.clone()

        original.dimHorizontal.b = 4
        original.dim.flag[1].isMovedByUser = true

        assertEquals(0, copy.dimHorizontal.b, "元の変更がクローンに漏れた")
        assertEquals(false, copy.dim.flag[1].isMovedByUser, "フラグが共有されている")
    }
}
