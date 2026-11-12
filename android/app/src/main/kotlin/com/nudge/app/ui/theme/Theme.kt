package com.nudge.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Low-saturation, calm neutrals per PRD §6. Every Material 3 role is set explicitly so no
// component falls back to the purple-tinted M3 baseline. The `error` roles are deliberately a
// warm grey, not red: semantic color is never used for guilt, and "overdue" is not a state in
// this product. These are placeholder swatches until a real brand palette exists.
//
// Shared with the Glance widget (widget/NextActionWidget.kt) so app and widget use one palette.
// `primary` light is mirrored in res/values/colors.xml (brand_primary) for the launcher icon.
val CalmLight = lightColorScheme(
    primary = Color(0xFF2E5F5A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE5E1),
    onPrimaryContainer = Color(0xFF12302D),
    inversePrimary = Color(0xFF8FCFC7),
    secondary = Color(0xFF5B6B6A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE4E2),
    onSecondaryContainer = Color(0xFF1F2A29),
    tertiary = Color(0xFF5F6477),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE1E3EC),
    onTertiaryContainer = Color(0xFF1F2230),
    error = Color(0xFF5F5A52),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFE7E2D9),
    onErrorContainer = Color(0xFF2A2722),
    background = Color(0xFFF4F3EF),
    onBackground = Color(0xFF1C1E1D),
    surface = Color(0xFFF4F3EF),
    onSurface = Color(0xFF1C1E1D),
    surfaceVariant = Color(0xFFE1E3DF),
    onSurfaceVariant = Color(0xFF464B49),
    surfaceTint = Color(0xFF2E5F5A),
    inverseSurface = Color(0xFF303332),
    inverseOnSurface = Color(0xFFEEF0ED),
    outline = Color(0xFF767B79),
    outlineVariant = Color(0xFFC6C9C5),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF4F3EF),
    surfaceDim = Color(0xFFD8D7D2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFEEEA),
    surfaceContainer = Color(0xFFEAE9E4),
    surfaceContainerHigh = Color(0xFFE4E3DE),
    surfaceContainerHighest = Color(0xFFDEDDD8),
)

val CalmDark = darkColorScheme(
    primary = Color(0xFF8FCFC7),
    onPrimary = Color(0xFF0B3531),
    primaryContainer = Color(0xFF1E4A45),
    onPrimaryContainer = Color(0xFFCFE5E1),
    inversePrimary = Color(0xFF2E5F5A),
    secondary = Color(0xFFB7C4C2),
    onSecondary = Color(0xFF24302F),
    secondaryContainer = Color(0xFF3A4645),
    onSecondaryContainer = Color(0xFFDDE4E2),
    tertiary = Color(0xFFC3C6D6),
    onTertiary = Color(0xFF2C2F3D),
    tertiaryContainer = Color(0xFF434655),
    onTertiaryContainer = Color(0xFFE1E3EC),
    error = Color(0xFFCFC8BC),
    onError = Color(0xFF35312A),
    errorContainer = Color(0xFF4C473F),
    onErrorContainer = Color(0xFFE7E2D9),
    background = Color(0xFF1B1F1E),
    onBackground = Color(0xFFE2E4E1),
    surface = Color(0xFF1B1F1E),
    onSurface = Color(0xFFE2E4E1),
    surfaceVariant = Color(0xFF414846),
    onSurfaceVariant = Color(0xFFC1C8C5),
    surfaceTint = Color(0xFF8FCFC7),
    inverseSurface = Color(0xFFE2E4E1),
    inverseOnSurface = Color(0xFF303332),
    outline = Color(0xFF8B9290),
    outlineVariant = Color(0xFF414846),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3A3F3E),
    surfaceDim = Color(0xFF1B1F1E),
    surfaceContainerLowest = Color(0xFF151918),
    surfaceContainerLow = Color(0xFF1F2322),
    surfaceContainer = Color(0xFF232726),
    surfaceContainerHigh = Color(0xFF2D3231),
    surfaceContainerHighest = Color(0xFF383D3C),
)

/**
 * The calm palette is the default look. Wallpaper-derived dynamic color (PRD §6 asks for
 * "dynamic color support") is opt-in from Settings, because a saturated wallpaper would
 * otherwise override the PRD's low-saturation palette requirement on every Android 12+ device.
 */
@Composable
fun NudgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
