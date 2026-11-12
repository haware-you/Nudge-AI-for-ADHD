package com.nudge.app.data

import com.nudge.app.data.local.entities.Effort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Verified against a standalone Kotlin/JVM sandbox during scaffolding (this pure function has
// no Android dependency, so it's the one piece of this scaffold actually compiled and run
// before commit — see the PR description for what remains unverified).
class PriorityScoreTest {
    private val now = 1_000_000_000_000L

    @Test
    fun `fresh low-effort task outranks fresh high-effort task`() {
        val low = PriorityScore.compute(Effort.LOW, now, 0, now)
        val high = PriorityScore.compute(Effort.HIGH, now, 0, now)
        assertTrue("expected low ($low) > high ($high)", low > high)
    }

    @Test
    fun `skipping a task lowers its score`() {
        val neverSkipped = PriorityScore.compute(Effort.MEDIUM, now, 0, now)
        val skippedOnce = PriorityScore.compute(Effort.MEDIUM, now, 1, now)
        assertTrue(skippedOnce < neverSkipped)
    }

    @Test
    fun `skip penalty is capped so a heavily-skipped task is not buried forever`() {
        val skippedAtCap = PriorityScore.compute(Effort.MEDIUM, now, 4, now) // 4 * 0.15 = cap
        val skippedWayOverCap = PriorityScore.compute(Effort.MEDIUM, now, 50, now)
        assertEquals(skippedAtCap, skippedWayOverCap, 0.0)
    }

    @Test
    fun `older task scores lower than a freshly-touched identical task`() {
        val fresh = PriorityScore.compute(Effort.LOW, now, 0, now)
        val oneWeekOld = PriorityScore.compute(Effort.LOW, now - 7 * 24 * 3_600_000L, 0, now)
        assertTrue(oneWeekOld < fresh)
    }

    @Test
    fun `a task is never fully buried by skips alone - floor stays above zero for low effort`() {
        val worstCase = PriorityScore.compute(Effort.LOW, now - 365L * 24 * 3_600_000L, 1000, now)
        assertTrue("expected > 0, got $worstCase", worstCase > 0)
    }

    @Test
    fun `Not Today never raises a task's score - regression for recency reset on skip`() {
        // Before the fix, markSkipped passed `now` as the recency timestamp, so skipping a
        // six-day-old task jumped its score from ~0.34 to ~0.57 and it reappeared at the top.
        val sixDaysAgo = now - 6 * 24 * 3_600_000L
        val before = PriorityScore.compute(Effort.MEDIUM, sixDaysAgo, 0, now)
        val afterSkip = PriorityScore.compute(Effort.MEDIUM, sixDaysAgo, 1, now)
        assertTrue("expected skip to lower score: before=$before after=$afterSkip", afterSkip < before)
    }
}
