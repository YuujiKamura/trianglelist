package com.jpaver.trianglelist

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Activity が作り直されてもモデルが生き残ることの gate (2026-08-27)。
 *
 * モデルの所有を DrawingSessionViewModel へ移した理由がここにある。ViewModel は
 * 構成変更 (端末回転など) では死なずプロセスと一緒に死ぬので、
 * 「CSV から復元すべき唯一の瞬間 = メモリにモデルが無いとき」と寿命が一致する。
 *
 * 以前は Activity の lateinit フィールドがモデルを持ち、復元は
 * onAttachedToWindow / onResume という View の都合のコールバックに紐付いていた。
 * その構造では Activity が作り直されるたびにディスクから読み直され、
 * まだ保存されていない操作が巻き戻る。
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SessionSurvivesRecreateTest {

    private fun privateCsv(a: MainActivity) = File(a.filesDir, a.PrivateCSVFileName)
    private fun waitDebounce() = Thread.sleep(1500)

    @Test
    fun 端末回転で作り直されても回転状態が残る() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())
            waitDebounce()

            var expected = 0f
            scenario.onActivity {
                it.fabRotate(41f, false, false)
                expected = it.currentListAngleForTest()
            }

            // 端末回転相当。ViewModel は生き残り、Activity と View は作り直される
            scenario.recreate()
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())

            scenario.onActivity {
                assertEquals("再生成でモデルが作り直されている", expected, it.currentListAngleForTest(), 1e-3f)
            }
        }
    }

    @Test
    fun 保存前に作り直されても巻き戻らない() {
        // デバウンス待ちの最中に作り直す = private CSV にはまだ書かれていない状態
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())
            waitDebounce()

            var expected = 0f
            scenario.onActivity {
                it.fabRotate(28f, false, false)
                expected = it.currentListAngleForTest()
            }
            // デバウンスを待たずに即座に再生成
            scenario.recreate()
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())

            scenario.onActivity {
                assertEquals("保存前の変更が再生成で失われた", expected, it.currentListAngleForTest(), 1e-3f)
            }
        }
    }

    @Test
    fun 再生成を繰り返しても積み上がる() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())
            waitDebounce()

            var expected = 0f
            scenario.onActivity { expected = it.currentListAngleForTest() }

            for (deg in listOf(11f, -23f, 60f)) {
                scenario.onActivity { it.fabRotate(deg, false, false) }
                expected += deg
                scenario.recreate()
                scenario.moveToState(Lifecycle.State.RESUMED)
                onView(isRoot())
                scenario.onActivity {
                    assertEquals("deg=" + deg + " の再生成後", expected, it.currentListAngleForTest(), 1e-3f)
                }
            }
        }
    }

    @Test
    fun 背面往復と再生成を混ぜても壊れない() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())
            waitDebounce()

            var expected = 0f
            scenario.onActivity { expected = it.currentListAngleForTest() }

            scenario.onActivity { it.fabRotate(17f, false, false) }
            expected += 17f
            scenario.moveToState(Lifecycle.State.CREATED)   // 背面へ (onStop で flush)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.recreate()                              // さらに作り直し
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())

            scenario.onActivity {
                assertEquals(expected, it.currentListAngleForTest(), 1e-3f)
                // 背面へ回った時点で private CSV にも落ちているはず
                assertTrue(privateCsv(it).readText().contains("ListAngle"))
            }
        }
    }

    @Test
    fun プロセス相当の再起動ではCSVから復元される() {
        // ViewModel を含めて完全に畳んでから launch し直す = 新プロセス相当。
        // ここでは逆に「CSV から復元されること」が正しい (モデルがメモリに無いため)。
        var saved = 0f
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())
            waitDebounce()
            scenario.onActivity {
                it.fabRotate(33f, false, false)
                saved = it.currentListAngleForTest()
            }
            scenario.moveToState(Lifecycle.State.CREATED) // onStop で確実に書く
        }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())
            scenario.onActivity {
                assertEquals("再起動で回転が復元されていない", saved, it.currentListAngleForTest(), 1e-3f)
                assertNotEquals(0f, it.currentListAngleForTest())
            }
        }
    }
}
