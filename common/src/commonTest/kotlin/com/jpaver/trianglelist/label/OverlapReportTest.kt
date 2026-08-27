package com.jpaver.trianglelist.label

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * OverlapReport.collisionKindByText: 「どの寸法テキストが、何と衝突しているか」を
 * box 単位に畳んだもの。ビューワー (desktop CAD Viewer の box overlay / web の枠) が
 * **枠を色分けする唯一の根拠**。
 *
 * 2026-08-27 user 指示「衝突してる寸法テキストを正しく色分けした枠付けて表示」。
 * ここで固定するのは 3 点:
 *  1. **辺 (EDGE) との接触は色分けしない**。寸法値の判定 box は縦アライメントの
 *     パディング分だけ実際のグリフより大きいので、辺に沿って置かれる寸法値は
 *     ほぼ必ず辺に触れる ── それを衝突として塗ると図面が埋まり、本当に読めない箇所が
 *     隠れる (user「縦アライメントのパディングの都合で線接触判定になってる、本来は
 *     文字同士の接触だけをみてくれればいい」「円との接触も問題になる」)。
 *     見るのは文字どうし (LABEL) と番号サークル (CIRCLE) の 2 種。
 *  2. 衝突は 2 つの box の間に起きる事実なので、**ペアの両側**に色が付く。
 *     OverlapPair は A-B / B-A を 1 件に正規化して textId 側しか持たないため、
 *     素朴に textId だけ見ると団子の片側だけ色が付く (= 色分けとして誤り)。
 *  3. 1 つの box が複数と衝突していたら「一番読めなくなる相手」を代表色にする
 *     (LABEL > CIRCLE)。1 box = 1 色でないと画面が読めない。
 */
class OverlapReportTest {

    private fun report(vararg pairs: OverlapPair) =
        OverlapReport(totalTexts = 4, overlappingTexts = pairs.size, pairs = pairs.toList())

    @Test
    fun `ラベル同士の衝突はペアの両側に色が付く`() {
        val r = report(OverlapPair("text:1:5.3", "text:2:3.9", ObstacleKind.LABEL, 0.0))

        val kinds = r.collisionKindByText

        assertEquals(ObstacleKind.LABEL, kinds["text:1:5.3"], "textId 側が衝突扱いでない: $kinds")
        assertEquals(ObstacleKind.LABEL, kinds["text:2:3.9"], "otherId 側 (相手のテキスト) に色が付いていない: $kinds")
    }

    @Test
    fun `番号サークルとの衝突は色分けするが 円の側は対象にしない`() {
        val r = report(OverlapPair("text:2:3.9", "circle:3", ObstacleKind.CIRCLE, 0.4))

        val kinds = r.collisionKindByText

        assertEquals(ObstacleKind.CIRCLE, kinds["text:2:3.9"])
        assertTrue("circle:3" !in kinds, "テキストでない相手が色分け対象に混ざった: $kinds")
    }

    @Test
    fun `辺との衝突は深くても色が付かない`() {
        // 判定 box は縦アライメントのパディング分グリフより大きい ── 辺に沿う寸法値は
        // ほぼ必ず辺に当たるので、深さがあっても「読めない」の証拠にならない
        val r = report(
            OverlapPair("text:1:5.3", "line:7", ObstacleKind.EDGE, 2.0),
            OverlapPair("text:2:3.9", "line:8", ObstacleKind.EDGE, 0.0),
        )

        assertTrue(r.collisionKindByText.isEmpty(), "辺との接触に色が付いた: ${r.collisionKindByText}")
    }

    @Test
    fun `文字どうしと番号サークルの両方に当たっている box は文字どうしが代表色になる`() {
        val r = report(
            OverlapPair("text:1:5.3", "line:7", ObstacleKind.EDGE, 2.0),
            OverlapPair("text:1:5.3", "circle:3", ObstacleKind.CIRCLE, 0.3),
            OverlapPair("text:1:5.3", "text:2:3.9", ObstacleKind.LABEL, 0.0),
        )

        assertEquals(
            ObstacleKind.LABEL,
            r.collisionKindByText["text:1:5.3"],
            "深さではなく種別の重さで代表色を決めるはず: ${r.collisionKindByText}",
        )
    }

    @Test
    fun `衝突が無ければ空`() {
        assertTrue(OverlapReport(totalTexts = 3, overlappingTexts = 0, pairs = emptyList()).collisionKindByText.isEmpty())
    }
}
