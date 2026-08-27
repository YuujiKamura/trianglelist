package com.jpaver.trianglelist.label

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.EditList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 寸法値の退避 (2026-08-27 user)。番号サークルを逃がしても残る「文字どうしの重なり」を、
 * 寸法値そのものの配置を変えて解く。
 *
 * 順番は「まず少しスライド → だめなら旗揚げ」(user「１４と１５の辺に関しては少し
 * スライドさせるだけで解決できる」)。候補は新規に発明せず、既にある horizontal コード
 * (0=中央 / 1,2=辺に沿って寄せる / 3,4=旗揚げ左右) をそのまま使う ── CSV に永続化済みの
 * 語彙なので、退避結果がそのまま既存の描画・書き出し・手動サイクル (W キー) と地続きになる。
 *
 * 既存の Dims.autoDimHorizontalByAngle は同じ狙いの書きかけ (enableAutoHorizontal=false で
 * 封印されている)。「面積 5 以下 かつ 内角 20 度以下の辺を旗揚げ」── 面積も角度も
 * 「文字が入らない」の代理指標でしかない。当たり判定ができた今は、代理をやめて
 * 実際に衝突しているかで発火できる (user「今のように当たり判定が付与されていれば
 * より分かりやすい」)。鋭角側を選ぶという幾何の勘所は候補の優先順として残す。
 */
class DimensionTextEscapeTest {

    private val NL = 10.toChar().toString()

    private fun buildList(csv: String): EditList<CycleShape> {
        val doc = CsvCodec.parse(csv)
        return CsvCodec.buildMixed(doc, CsvCodec.build(doc), 1f)
    }

    @Test
    fun `梯子はスライドを先に 旗揚げを後に試す`() {
        // 1,2 = 辺に沿って寄せる (少しスライド)、3,4 = 旗揚げ。
        // 旗揚げは引出線が増えて図が賑やかになるので、済むならスライドで済ませる
        val ladder = DimensionTextEscape.candidateLadder(current = 0, preferRight = true)

        assertEquals(listOf(1, 2, 3, 4), ladder)
        assertTrue(ladder.indexOf(1) < ladder.indexOf(3), "旗揚げがスライドより先に来ている")
    }

    @Test
    fun `現在値は候補から除く`() {
        assertTrue(2 !in DimensionTextEscape.candidateLadder(current = 2, preferRight = false))
    }

    @Test
    fun `preferRight で左右の優先順が入れ替わる`() {
        assertEquals(listOf(2, 1, 4, 3), DimensionTextEscape.candidateLadder(current = 0, preferRight = false))
    }

    @Test
    fun `衝突が無ければ 1 つも動かさない`() {
        val list = buildList("1,100.0,100.0,100.0,-1,-1" + NL)

        val moves = DimensionTextEscape.solve(list, textSize = 0.25f)

        assertTrue(moves.isEmpty(), "余裕のある図形で寸法値を動かすのはおかしい: $moves")
    }

    @Test
    fun `同じ入力なら同じ結果 (決定的)`() {
        val a = DimensionTextEscape.solve(buildList("1,1.0,0.9,0.8,-1,-1" + NL), textSize = 0.525f)
        val b = DimensionTextEscape.solve(buildList("1,1.0,0.9,0.8,-1,-1" + NL), textSize = 0.525f)

        assertEquals(a.toString(), b.toString(), "同じ入力で結果が変わるのは決定的でない")
    }

    @Test
    fun `退避で衝突を増やさない`() {
        val list = buildList("1,1.0,0.9,0.8,-1,-1" + NL)
        val before = ModelOverlapAnalyzer.analyze(list, textSize = 0.525f).collisionKindByText.size

        DimensionTextEscape.apply(list, DimensionTextEscape.solve(list, textSize = 0.525f))
        val after = ModelOverlapAnalyzer.analyze(list, textSize = 0.525f).collisionKindByText.size

        assertTrue(after <= before, "退避で衝突が増えた: before=$before after=$after")
    }
}
