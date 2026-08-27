package com.jpaver.trianglelist

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 「図面をシェアして保存した後に、図形の回転状態がリセットされる」(2026-08-27 実機報告) の gate。
 *
 * 症状の実体は共有ではなく **共有チューザーから戻ったときの再読み込み**だった。
 * onResume は別 Activity (チューザー / ファイルピッカー) から戻るたびに走るのに、
 * そこに CSV からの復元がコピーされていた (7f204c15, 2025-04-22)。
 * その結果、まだ private CSV に書かれていない操作 — 二本指ピンチ回転は
 * MyView.kt:227 から fabRotate を直接呼び、setCommonFabListener の autosave を通らない —
 * が resume のたびにディスクの内容で巻き戻されていた。
 *
 * ここでは ActivityScenario で CREATED -> RESUMED を往復させ、チューザーから戻る状況を作る。
 * fabRotate の直接呼び出しは、保存を伴わないピンチ回転の経路と同じ。
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class RotationSurvivesResumeTest {

    private fun ActivityScenario<MainActivity>.angle(): Float {
        var v = 0f
        onActivity { v = it.currentListAngleForTest() }
        return v
    }

    @Test
    fun 未保存の回転がチューザー往復で巻き戻らない() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot()) // 初期描画の完了待ち

            val before = scenario.angle()

            // 保存を伴わない回転 (= ピンチ回転が通る経路そのもの)
            scenario.onActivity { it.fabRotate(30f, false, false) }
            val rotated = scenario.angle()
            assertEquals("回転がモデルに入っていない", before + 30f, rotated, 1e-3f)

            // 共有チューザーへ行って戻る = onStop -> onResume
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())

            assertEquals("resume で回転が巻き戻った", rotated, scenario.angle(), 1e-3f)
        }
    }

    @Test
    fun 回転は往復を繰り返しても積み上がったまま() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())

            var expected = scenario.angle()
            for (deg in listOf(15f, -40f, 90f)) {
                scenario.onActivity { it.fabRotate(deg, false, false) }
                expected += deg
                scenario.moveToState(Lifecycle.State.CREATED)
                scenario.moveToState(Lifecycle.State.RESUMED)
                onView(isRoot())
                assertEquals("deg=$deg の往復後", expected, scenario.angle(), 1e-3f)
            }
        }
    }
}
