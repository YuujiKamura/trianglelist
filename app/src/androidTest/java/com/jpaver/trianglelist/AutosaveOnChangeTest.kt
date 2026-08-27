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
 * 自動保存が「操作の配線」ではなく「モデルの変更」で駆動されることの gate (2026-08-27)。
 *
 * 以前は setCommonFabListener の isSaveCSV 引数で操作ごとに保存要否を人間が判断していて、
 * その配線を通らない MyView のピンチ回転 (fabRotate 直呼び) が丸ごと漏れていた。
 * 今は requestAutosave() を多めに投げ、実際に書くかは焼いた本文の比較が決める。
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AutosaveOnChangeTest {

    private fun privateCsv(a: MainActivity) = File(a.filesDir, a.PrivateCSVFileName)

    /** デバウンス (700ms) より十分長く待つ */
    private fun waitDebounce() = Thread.sleep(1500)

    @Test
    fun 配線を通らない回転でも自動保存される() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())
            waitDebounce()

            var before = ""
            scenario.onActivity { before = privateCsv(it).readText() }
            assertTrue("前提: private CSV が書かれていない", before.contains("koujiname"))

            // setCommonFabListener を通らない経路 (= ピンチ回転と同じ)
            scenario.onActivity { it.fabRotate(37f, false, false) }
            waitDebounce()

            var after = ""
            scenario.onActivity { after = privateCsv(it).readText() }
            assertNotEquals("回転が private CSV に反映されていない", before, after)
            assertTrue(after.contains("ListAngle"))
        }
    }

    @Test
    fun 変更が無ければ書き直さない() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())
            waitDebounce()

            var stamp = 0L
            scenario.onActivity { stamp = privateCsv(it).lastModified() }

            // 何も変えずに要求だけ投げる
            scenario.onActivity { it.requestAutosave() }
            waitDebounce()
            scenario.onActivity { it.requestAutosave() }
            waitDebounce()

            scenario.onActivity {
                assertEquals("内容が同じなのに書き直している", stamp, privateCsv(it).lastModified())
            }
        }
    }

    @Test
    fun 連続操作はデバウンスで束ねられ最後の状態が残る() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())
            waitDebounce()

            // ピンチ相当の連続回転 (毎回 requestAutosave が飛ぶ)
            var expected = 0f
            scenario.onActivity {
                repeat(10) { _ -> it.fabRotate(3f, false, false) }
                expected = it.currentListAngleForTest()
            }
            waitDebounce()

            scenario.onActivity {
                val text = privateCsv(it).readText()
                val angle = Regex("""ListAngle,\s*([-\d.]+)""").find(text)
                    ?.groupValues?.get(1)?.toFloat()
                assertEquals("最後の回転状態が保存されていない", expected, angle!!, 1e-2f)
            }
        }
    }

    @Test
    // DEX 040 未満はメソッド名に空白を置けないため、区切りは _ を使う
    fun onStop時は待ちを打ち切って書く() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())
            waitDebounce()

            var before = ""
            scenario.onActivity { before = privateCsv(it).readText() }

            // 回転直後、デバウンスを待たずに背面へ
            scenario.onActivity { it.fabRotate(22f, false, false) }
            scenario.moveToState(Lifecycle.State.CREATED)

            scenario.onActivity {
                assertNotEquals("onStop で flush されていない", before, privateCsv(it).readText())
            }
        }
    }
}
