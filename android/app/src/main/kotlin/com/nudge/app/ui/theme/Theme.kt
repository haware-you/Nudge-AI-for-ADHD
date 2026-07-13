package com.nudge.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Low-saturation, calm neutrals per PRD §6 — semantic color is never used for guilt (no red
// for "overdue"; that state does not exist in this product). These are placeholder swatches
// until a real brand palette exists; the important constraint is the *absence* of an alarm/red
// role, not these specific values.
private val CalmLight = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2E5F5A),
    secondary = androidx.compose.ui.graphics.Color(0xFF5B6B6A),
    background = androidx.compose.ui.graphics.Color(0xFFF4F3EF),
    surface = androidx.compose.ui.graphics.Color(0xFFF4F3EF),
)

private val CalmDark = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF8FCFC7),
    secondary = androidx.compose.ui.graphics.Color(0xFFB7C4C2),
    background = androidx.compose.ui.graphics.Color(0xFF1B1F1E),
    surface = androidx.compose.ui.graphics.Color(0xFF1B1F1E),
)

@Composable
fun NudgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> CalmDark
        else -> CalmLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NudgeTypography,
        content = content,
    )
}
