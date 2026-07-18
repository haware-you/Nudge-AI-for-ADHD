package com.nudge.app.data

/**
 * Decides whether a returning user sees the Welcome Back screen (Flow E) instead of the normal
 * app/widget state. Pure and Android-free like [PriorityScore], for the same reason: it's the
 * one piece of this feature actually worth unit-testing without a Gradle sync.
 *
 * PRD story: "As Maya, I want to come back after two weeks and see a calm 'welcome back' screen,
 * not a backlog" — Open Decision 1. The design doc's Flow E fires this at >= 7 days away.
 */
object AbsenceGate {
    const val ABSENCE_THRESHOLD_MILLIS: Long = 7L * 24 * 3_600_000

    /**
     * [lastOpenedAtMillis] is 0L for "never recorded" (e.g. a fresh install before onboarding
     * has run once) — that case is never an absence, it's a first run, and first runs go
     * through Onboarding, not Welcome Back.
     */
    fun shouldShowWelcomeBack(lastOpenedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (lastOpenedAtMillis <= 0L) return false
        return (nowMillis - lastOpenedAtMillis) >= ABSENCE_THRESHOLD_MILLIS
    }
}
