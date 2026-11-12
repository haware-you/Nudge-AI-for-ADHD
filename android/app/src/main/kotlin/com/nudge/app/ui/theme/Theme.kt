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

// Quiet grouped neutrals with one deep-green tint (PRD §6: low-saturation, calm). Pages are a
// soft grey, content sits on plain cards (`surfaceContainerLowest`), and the tint is spent only
// on the primary action of a screen. Every Material 3 role is set explicitly so no component
// falls back to the purple-tinted M3 baseline. The `error` roles are a neutral grey, not red:
// semantic color is never used for guilt, and "overdue" is not a state in this product.
//
// Shared with the Glance widget (widget/NextActionWidget.kt). `primary` light is mirrored in
// res/values/colors.xml (brand_primary) for the launcher icon.
val CalmLight = lightColorScheme(
    primary = Color(0xFF2E5A46),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E8DE),
    onPrimaryContainer = Color(0xFF0F2A1E),
    inversePrimary = Color(0xFF8FD0AE),
    secondary = Color(0xFF4F5B54),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3E8E4),
    onSecondaryContainer = Color(0xFF1B211D),
    tertiary = Color(0xFF5A5F6B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE2E4EC),
    onTertiaryContainer = Color(0xFF1D2028),
    error = Color(0xFF5E5F5C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFE6E7E4),
    onErrorContainer = Color(0xFF1F201E),
    background = Color(0xFFF2F3F1),
    onBackground = Color(0xFF1A1C1A),
    surface = Color(0xFFF2F3F1),
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFE4E6E2),
    onSurfaceVariant = Color(0xFF5A5F59),
    surfaceTint = Color(0xFF2E5A46),
    inverseSurface = Color(0xFF2E302E),
    inverseOnSurface = Color(0xFFF2F3F1),
    outline = Color(0xFF7A7F78),
    outlineVariant = Color(0xFFDDE0DB),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFD9DBD6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8F6),
    surfaceContainer = Color(0xFFECEEEA),
    surfaceContainerHigh = Color(0xFFE6E8E4),
    surfaceContainerHighest = Color(0xFFE0E2DE),
)

val CalmDark = darkColorScheme(
    primary = Color(0xFF8FD0AE),
    onPrimary = Color(0xFF0B3323),
    primaryContainer = Color(0xFF1F4534),
    onPrimaryContainer = Color(0xFFD7E8DE),
    inversePrimary = Color(0xFF2E5A46),
    secondary = Color(0xFFB8C4BC),
    onSecondary = Color(0xFF242D27),
    secondaryContainer = Color(0xFF363F39),
    onSecondaryContainer = Color(0xFFE3E8E4),
    tertiary = Color(0xFFC4C7D1),
    onTertiary = Color(0xFF2D3038),
    tertiaryContainer = Color(0xFF44474F),
    onTertiaryContainer = Color(0xFFE2E4EC),
    error = Color(0xFFC9CAC6),
    onError = Color(0xFF2F302E),
    errorContainer = Color(0xFF474846),
    onErrorContainer = Color(0xFFE6E7E4),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E8E4),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE6E8E4),
    surfaceVariant = Color(0xFF3A3D3A),
    onSurfaceVariant = Color(0xFFB4B9B2),
    surfaceTint = Color(0xFF8FD0AE),
    inverseSurface = Color(0xFFE6E8E4),
    inverseOnSurface = Color(0xFF2E302E),
    outline = Color(0xFF868B84),
    outlineVariant = Color(0xFF2E312E),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF363836),
    surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF1C1D1C),
    surfaceContainerLow = Color(0xFF121312),
    surfaceContainer = Color(0xFF1C1D1C),
    surfaceContainerHigh = Color(0xFF262826),
    surfaceContainerHighest = Color(0xFF303230),
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
        shapes = NudgeShapes,
        content = content,
    )
}
