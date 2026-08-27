package com.jpaver.trianglelist

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 三角形の辺を編集してから FAB を一通り叩く煙テスト。
 *
 * 2026-08-27 に matcher を書き直した。元は Espresso レコーダの出力そのままで
 *   (a) withContentDescription("rewrite shape") — 文字列リソースが
 *       "Add or Rewrite" に変わった時点で一致しなくなっていた (strings.xml:113)
 *   (b) childAtPosition(CoordinatorLayout, 18) 等の位置指定 — レイアウトの並びを
 *       変えるたびに壊れる
 * の 2 つに依存していて、実装が正しくても落ちる状態で放置されていた。
 * id は安定していて一意なので withId + isDisplayed だけで十分。操作の順序は元のまま。
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest2 {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.INTERNET",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

    private fun tapFab(id: Int) = onView(allOf(withId(id), isDisplayed())).perform(click())

    @Test
    fun mainActivityTest2() {
        // 辺 B / C を編集して確定する
        onView(allOf(withId(R.id.editLengthB1), isDisplayed()))
            .perform(replaceText("6"), closeSoftKeyboard())
        onView(allOf(withId(R.id.editLengthB1), isDisplayed()))
            .perform(pressImeActionButton())
        onView(allOf(withId(R.id.editLengthC1), isDisplayed()))
            .perform(replaceText("6"), closeSoftKeyboard())

        // 図形操作 FAB を順に叩く (元のテストと同じ順序)
        tapFab(R.id.fab_replace)
        tapFab(R.id.fab_dimsideh)
        tapFab(R.id.fab_dimsidew)
        tapFab(R.id.fab_flag)
        tapFab(R.id.fab_up)
        tapFab(R.id.fab_resetView)
    }
}
