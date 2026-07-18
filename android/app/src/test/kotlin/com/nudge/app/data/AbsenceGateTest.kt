package com.nudge.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AbsenceGateTest {
    private val now = 1_000_000_000_000L

    @Test
    fun `never-opened before (fresh install) is never an absence`() {
        assertFalse(AbsenceGate.shouldShowWelcomeBack(lastOpenedAtMillis = 0L, nowMillis = now))
    }

    @Test
    fun `opened yesterday is not an absence`() {
        val yesterday = now - 24 * 3_600_000L
        assertFalse(AbsenceGate.shouldShowWelcomeBack(lastOpenedAtMillis = yesterday, nowMillis = now))
    }

    @Test
    fun `six days away is not yet an absence`() {
        val sixDaysAgo = now - 6L * 24 * 3_600_000
        assertFalse(AbsenceGate.shouldShowWelcomeBack(lastOpenedAtMillis = sixDaysAgo, nowMillis = now))
    }

    @Test
    fun `exactly seven days away triggers welcome back`() {
        val sevenDaysAgo = now - AbsenceGate.ABSENCE_THRESHOLD_MILLIS
        assertTrue(AbsenceGate.shouldShowWelcomeBack(lastOpenedAtMillis = sevenDaysAgo, nowMillis = now))
    }

    @Test
    fun `two weeks away triggers welcome back`() {
        val twoWeeksAgo = now - 14L * 24 * 3_600_000
        assertTrue(AbsenceGate.shouldShowWelcomeBack(lastOpenedAtMillis = twoWeeksAgo, nowMillis = now))
    }
}
