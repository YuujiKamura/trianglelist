package com.jpaver.trianglelist.regression

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 過去に発生したサイレントバグ・クラッシュ操作パスの回帰テスト集。
 *
 * 各テストは「不具合が報告された操作シーケンス」をそのまま再現し、
 * RegressionHarness の不変条件チェックによって再発を防止する。
 */
class RegressionPlaybackTest {

    @Test
    fun `基本パス_追加と削除の連続操作で不変条件が保たれる`() {
        val harness = RegressionHarness()
        harness
            .addTriangle(5f, 4f, 3f)
            .addTriangle(4f, 3.5f, 3f)
            .addTriangle(3.5f, 3f, 2.5f)
            .removeLast() // 4番を削除
            .addTriangle(3.5f, 3f, 2.5f) // 再度追加
            .rotate(45f)
            .serializeAndReload()

        assertEquals(4, harness.triangleList.size())
        assertTrue(harness.calculateTotalArea() > 0f)
    }

    @Test
    fun `サイレント巻き戻り防止_CSV書き出しと再読み込みでデータが1ミリも狂わない`() {
        val harness = RegressionHarness()
        harness
            .addTriangle(5f, 4f, 3f)
            .addTriangle(4f, 3.5f, 3f)

        val areaBefore = harness.calculateTotalArea()

        // 複数回連続でセーブ＆リロード（アプリのバックグラウンド/フォアグラウンド往復相当）
        harness
            .serializeAndReload()
            .serializeAndReload()
            .serializeAndReload()

        assertEquals(3, harness.triangleList.size())
        val areaAfter = harness.calculateTotalArea()
        assertTrue(
            abs(areaBefore - areaAfter) < 0.01f,
            "Area must not drift across multiple serializations. Before=$areaBefore, After=$areaAfter"
        )
    }

    @Test
    fun `分岐接続パス_親を指定した枝分かれ接続の整合性`() {
        val harness = RegressionHarness()
        // 1番 (6,5,4)
        // 2番 -> 1番のB辺に接続
        harness.addConnectedTriangle(5f, 4f, 3f, parentNumber = 1, connectionSide = 1)
        // 3番 -> 1番のC辺に接続 (枝分かれ)
        harness.addConnectedTriangle(4f, 3.5f, 3f, parentNumber = 1, connectionSide = 2)

        harness.serializeAndReload()

        assertEquals(3, harness.triangleList.size())
        assertEquals(1, harness.triangleList.getBy(2).parentnumber)
        assertEquals(1, harness.triangleList.getBy(3).parentnumber)
    }
}
