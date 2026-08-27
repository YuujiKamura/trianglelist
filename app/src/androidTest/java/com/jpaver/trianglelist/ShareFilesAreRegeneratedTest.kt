package com.jpaver.trianglelist

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 共有ファイルが毎回作り直されることの gate (2026-08-27)。
 *
 * 共有ファイル名は日付 + 路線名なので、同じ日の 2 回目以降は同じ名前になる。
 * 以前は makeShareUris が `if(!newFile.exists())` で「無いときだけ」生成し、
 * さらに saveCSVtoPrivate 側にも「同じ名前で連続保存ならスキップ」があったため、
 * 図面を編集してから共有し直しても **1 回目の中身が相手に届いていた**。
 * 共有後の削除は RESULT_OK のときだけで、共有アプリの多くは成功しても
 * CANCELED を返すのでファイルは実際に残る。
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ShareFilesAreRegeneratedTest {

    private val sentinel = "STALE_CONTENT_MUST_BE_OVERWRITTEN"

    @Test
    fun 既存の共有ファイルは古い中身のまま渡されない() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())

            val name = "sharetest_regenerate.csv"

            // 1 回目の共有で出来たファイルを模して、古い中身を置いておく
            scenario.onActivity { File(it.filesDir, name).writeText(sentinel) }
            scenario.onActivity {
                assertTrue("前提: 古いファイルが置けていない",
                    File(it.filesDir, name).readText().contains(sentinel))
            }

            // 2 回目の共有
            scenario.onActivity { it.makeShareUrisForTest(listOf(name)) }

            scenario.onActivity {
                val text = File(it.filesDir, name).readText()
                assertFalse("共有ファイルが古い中身のまま渡されている", text.contains(sentinel))
                assertTrue("共有ファイルが図面 CSV として書き直されていない: ${text.take(80)}",
                    text.contains("koujiname"))
            }
        }
    }

    @Test
    fun 編集を挟んだ再共有で中身が更新される() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(isRoot())

            val name = "sharetest_afteredit.csv"

            scenario.onActivity { it.makeShareUrisForTest(listOf(name)) }
            var first = ""
            scenario.onActivity { first = File(it.filesDir, name).readText() }

            // 保存を伴わない編集 (ピンチ回転が通る経路)
            scenario.onActivity { it.fabRotate(45f, false, false) }

            scenario.onActivity { it.makeShareUrisForTest(listOf(name)) }
            scenario.onActivity {
                val second = File(it.filesDir, name).readText()
                assertFalse("編集後の再共有で中身が更新されていない", first == second)
                assertTrue(second.contains("ListAngle"))
            }
        }
    }
}
