package com.example.trilib

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        // applicationId はフレーバーで変わる (dev は com.jpaver.myapplication)。
        // テンプレート由来のハードコード文字列 "com.example.trilib.test" を検証していて
        // 実装と無関係に落ち続けていたので、実際の applicationId と突き合わせる形にした (2026-08-27)。
        assertEquals(com.jpaver.trianglelist.BuildConfig.APPLICATION_ID, appContext.packageName)
    }
}