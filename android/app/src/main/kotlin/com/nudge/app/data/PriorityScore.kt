package com.nudge.app.data

import com.nudge.app.data.local.entities.Effort
import kotlin.math.exp
import kotlin.math.min

/**
 * Mirrors supabase/functions/process-memo/score.ts exactly. The server recomputes
 * authoritatively whenever a memo is processed; this client-side copy exists only so a
 * Done/Skip action can re-rank the local queue instantly (PRD §8: widget update < 5s) without
 * waiting on a network round-trip. Keep the constants identical to score.ts — if you tune one,
 * tune both.
 */
object PriorityScore {
    private val effortWeight = mapOf(Effort.LOW to 1.0, Effort.MEDIUM to 0.6, Effort.HIGH to 0.3)
    private const val RECENCY_HALF_LIFE_HOURS = 72.0
    private const val SKIP_PENALTY_PER_SKIP = 0.15
    private const val SKIP_PENALTY_CAP = 0.6
    private const val EFFORT_COMPONENT_WEIGHT = 0.5
    private const val RECENCY_COMPONENT_WEIGHT = 0.3
    private const val SKIP_COMPONENT_WEIGHT = 0.2

    fun compute(effort: Effort, updatedAtMillis: Long, skipCount: Int, nowMillis: Long = System.currentTimeMillis()): Double {
        val ageHours = (nowMillis - updatedAtMillis).coerceAtLeast(0).toDouble() / 3_600_000.0
        val recencyWeight = exp(-ageHours / RECENCY_HALF_LIFE_HOURS)
        val skipPenalty = min(skipCount * SKIP_PENALTY_PER_SKIP, SKIP_PENALTY_CAP)

        return EFFORT_COMPONENT_WEIGHT * (effortWeight[effort] ?: 0.6) +
            RECENCY_COMPONENT_WEIGHT * recencyWeight -
            SKIP_COMPONENT_WEIGHT * skipPenalty
    }
}
