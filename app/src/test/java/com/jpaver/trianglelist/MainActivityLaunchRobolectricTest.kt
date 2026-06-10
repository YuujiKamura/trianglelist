package com.jpaver.trianglelist

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * エミュレータなしの起動 smoke (Robolectric 版 E2E)。
 *
 * androidTest/MainActivityTest (Espresso, 要エミュレータ) と同じ assert を
 * JVM 上の Robolectric で実走する。起動経路は onCreate (binding inflate /
 * initFabs / fabController) → onAttachedToWindow (resumeCSV / loadEditTable /
 * setEditorTableTextWatcher → isViewAttached=true) まで通るので、
 * PointXY Double 化・editmodel/writer 降ろし・MyView placement 切替の
 * 「起動時に走るコード」がエミュレータなしで踏める。
 */
@RunWith(AndroidJUnit4::class)
class MainActivityLaunchRobolectricTest {

    @Test
    fun launch_reachesResumed_andViewIsAttached() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
            scenario.onActivity { activity ->
                assertTrue(activity.isViewAttached)
            }
        }
    }

    @Test
    fun recreate_doesNotCrash_andViewReattaches() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.recreate()
            scenario.onActivity { activity ->
                assertTrue(activity.isViewAttached)
            }
        }
    }
}
