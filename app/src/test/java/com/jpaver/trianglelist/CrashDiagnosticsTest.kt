package com.jpaver.trianglelist

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrashDiagnosticsTest {

    @Test
    fun testLogAndBuildDump() {
        CrashDiagnostics.setStateProvider {
            "State: triangles=5"
        }
        CrashDiagnostics.log("action_step_1")
        CrashDiagnostics.log("action_step_2")

        val dump = CrashDiagnostics.buildDump()
        assertTrue(dump.contains("State: triangles=5"))
        assertTrue(dump.contains("action_step_1"))
        assertTrue(dump.contains("action_step_2"))
    }

    @Test
    fun testDumpWithoutStateProvider() {
        CrashDiagnostics.setStateProvider { throw RuntimeException("Simulated error") }
        CrashDiagnostics.log("something")
        val dump = CrashDiagnostics.buildDump()
        assertTrue(dump.contains("Failed to get state"))
        assertTrue(dump.contains("something"))
    }
}
