package com.jpaver.trianglelist.label

import com.jpaver.trianglelist.web.WebDrawingExport
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 自動配置の入口が 1 つであることの担保 (2026-08-28)。
 *
 * アプリ経路 (TriangleList.arrangeLabelsWithoutCollision) と書き出し経路
 * (WebDrawingExport.buildDxfText) に同じ順番が別々に書かれていた。片方だけ直すと
 * 「画面と図面で配置が違う」が起きる ── user が 2026-08-27 に「基本アプリと図面が
 * 食い違うのはバグだと思っていい。期待値と違うわけだから」と言った類の事故が、
 * 手順の写し間違いという形でいつでも入り得る状態だった。
 *
 * さらに書き出し側は設定 (LabelArrangePolicy.enabled) を見ていなかったので、
 * アプリで「寸法の自動配置」を OFF にしても書き出しには効いていなかった。
 * 判定を LabelArrange に集約したので、ここでその 2 点を固定する。
 */
class LabelArrangeEntryPointTest {

    private val NL = 10.toChar().toString()
    private val csv = "1,6.0,5.0,4.0,-1,-1" + NL +
        "2,5.0,4.0,3.5,1,1" + NL +
        "3,4.0,3.5,3.0,1,2" + NL +
        "4,3.5,3.0,2.5,2,1" + NL

    @AfterTest
    fun restore() {
        LabelArrangePolicy.enabled = true
    }

    @Test
    fun `設定を OFF にすると書き出しにも効く`() {
        val raw = WebDrawingExport.buildDxfText(csv, "", false, 3.5f, false)

        LabelArrangePolicy.enabled = false
        val offed = WebDrawingExport.buildDxfText(csv, "", false, 3.5f, true)
        assertEquals(raw, offed, "自動配置 OFF なのに書き出しが自動配置後の図面になっている")

        LabelArrangePolicy.enabled = true
        val oned = WebDrawingExport.buildDxfText(csv, "", false, 3.5f, true)
        assertTrue(oned != raw, "自動配置 ON なのに書き出しが変わらない (このサンプルは衝突する前提)")
    }
}
