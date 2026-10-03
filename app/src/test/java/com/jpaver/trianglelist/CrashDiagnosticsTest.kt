package com.jpaver.trianglelist

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun testInvariantCheckValidTriangleList() {
        val list = TriangleList(Triangle(6f, 5f, 4f))
        val violations = CrashDiagnostics.checkInvariants(list)
        assertTrue("Valid triangle list should have 0 violations, had: $violations", violations.isEmpty())
    }

    @Test
    fun testInvariantCheckNullList() {
        val violations = CrashDiagnostics.checkInvariants(null)
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.contains("null") })
    }

    @Test
    fun testRecordInvariantViolation() {
        CrashDiagnostics.recordInvariantViolation("Geometry", "Degenerate triangle detected")
        val violations = CrashDiagnostics.getRecordedViolations()
        assertTrue(violations.any { it.contains("Degenerate triangle detected") })

        val dump = CrashDiagnostics.buildDump()
        assertTrue(dump.contains("INVARIANT VIOLATIONS"))
        assertTrue(dump.contains("Degenerate triangle detected"))
    }

    @Test
    fun testBuildReport() {
        CrashDiagnostics.log("report_test_action")
        val report = CrashDiagnostics.buildReport(null)
        assertTrue(report.contains("--- APPLICATION & DEVICE INFO ---"))
        assertTrue(report.contains("OS: Android"))
        assertTrue(report.contains("report_test_action"))
    }
}
