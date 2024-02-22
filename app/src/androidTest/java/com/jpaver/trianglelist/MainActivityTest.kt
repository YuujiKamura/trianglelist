package com.jpaver.trianglelist

import androidx.test.ext.junit.rules.ActivityScenarioRule
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    @get:Rule
    var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testViewAttachment() {
        mActivityScenarioRule.scenario.onActivity { activity ->
            // ここでisViewAttachedの値をテストする
            assertTrue(activity.isViewAttached)
        }
    }
}