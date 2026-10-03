package com.jpaver.trianglelist.regression

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * モデル層に対するランダムファジングテスト (Property-Based Testing)。
 *
 * 【目的】
 * 人間が思いつかないデタラメな操作順序（何十回の追加・削除・回転・保存・復元の連続）を
 * 浴びせ倒し、サイレントな幾何破綻・親ノード破損・CSV往復のデータ欠落を先回りで検出する。
 *
 * 再現性を保つため固定シード (42) を使用する。
 * 失敗した場合は harness.getBreadcrumbs() がそのままバグの「完全な再現手順」となる。
 */
class ModelFuzzingTest {

    @Test
    fun `ファジング_ランダムな100回のアクションシーケンスで不変条件が維持される`() {
        val random = Random(42) // 再現可能な固定シード
        val harness = RegressionHarness()

        for (step in 1..100) {
            val actionType = random.nextInt(5)

            try {
                when (actionType) {
                    0, 1 -> {
                        // 三角形追加 (チェーン接続)
                        // 直前の三角形の自由辺に合わせた辺長を生成
                        val lastTri = harness.triangleList.getBy(harness.triangleList.size())
                        val parentSide = if (random.nextBoolean()) 1 else 2
                        val a = if (parentSide == 1) lastTri.lengthB_ else lastTri.lengthC_

                        // 有効な三角形（a, b, c）を生成 (三角形不等式を満たす: a, 0.8a, 0.7a)
                        val b = (a * 0.8f).coerceAtLeast(1.0f)
                        val c = (a * 0.7f).coerceAtLeast(1.0f)

                        harness.addConnectedTriangle(
                            a = a,
                            b = b,
                            c = c,
                            parentNumber = lastTri.mynumber,
                            connectionSide = parentSide
                        )
                    }

                    2 -> {
                        // 末尾削除 (サイズが2以上の場合のみ)
                        if (harness.triangleList.size() > 1) {
                            harness.removeLast()
                        }
                    }

                    3 -> {
                        // 回転
                        val angle = random.nextFloat() * 360f - 180f
                        harness.rotate(angle)
                    }

                    4 -> {
                        // CSVシリアライズ ＆ 再ロード (セーブ/復元)
                        harness.serializeAndReload()
                    }
                }
            } catch (t: Throwable) {
                // 失敗時、そこに至る全操作履歴を出力して即座にテスト化できるようにする
                val log = harness.getBreadcrumbs().joinToString("\n")
                throw AssertionError(
                    "Fuzzing failed at step $step (actionType=$actionType)!\n--- ACTION SEQUENCE ---\n$log",
                    t
                )
            }
        }

        assertTrue(harness.triangleList.size() >= 1)
        assertTrue(harness.calculateTotalArea() > 0.0f)
    }
}
