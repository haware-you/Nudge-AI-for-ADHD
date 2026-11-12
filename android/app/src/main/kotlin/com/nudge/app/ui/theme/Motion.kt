package com.nudge.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Motion rules (from the apple-design skill, §1, §4, §7, adapted to Compose):
 *  - Springs, not fixed-duration tweens, so anything can be interrupted and re-targeted.
 *  - Critically damped by default: nothing overshoots unless a gesture threw it.
 *  - Feedback starts on press-down, not on release.
 *  - Things leave along the path they arrived on.
 * Compose scales these by the system animation setting, so "remove animations" is honoured
 * without extra code (needs verification on a device).
 */
object NudgeMotion {
    /** Default for anything that moves or appears. No bounce. */
    fun <T> settle(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Press feedback: fast enough to read as instant. */
    fun <T> press(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

    const val PRESSED_SCALE = 0.97f
    const val SCRIM_ALPHA = 0.32f
    const val CHROME_ALPHA = 0.9f
}
