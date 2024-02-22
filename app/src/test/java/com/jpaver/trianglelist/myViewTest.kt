package com.jpaver.trianglelist

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// MyViewTest.kt
@RunWith(RobolectricTestRunner::class)
class MyViewTest {
    @Test
    fun testPointerCount() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val myView = MyView(context, null)
        // ポインタの数を設定
        myView.setPointerCountForTest(2)
        // ポインタの数が正しく設定されたことを検証
        assertEquals(2, myView.getPointerCountForTest())
    }
}