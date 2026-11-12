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

// Warm paper neutrals with a dark olive primary and sage accents (PRD §6: low-saturation,
// calm). Every Material 3 role is set explicitly so no component falls back to the
// purple-tinted M3 baseline. The `error` roles are deliberately a warm grey, not red: semantic
// color is never used for guilt, and "overdue" is not a state in this product.
//
// Cards sit on `surfaceContainerLowest` (lighter than the page in light mode). Shared with the
// Glance widget (widget/NextActionWidget.kt) so app and widget use one palette. `primary`
// light is mirrored in res/values/colors.xml (brand_primary) for the launcher icon.
val CalmLight = lightColorScheme(
    primary = Color(0xFF2F3B2C),
    onPrimary = Color(0xFFF6F5EF),
    primaryContainer = Color(0xFFDDE3D3),
    onPrimaryContainer = Color(0xFF1F281C),
    inversePrimary = Color(0xFFC9D4BC),
    secondary = Color(0xFF566349),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4E8DA),
    onSecondaryContainer = Color(0xFF232B1E),
    tertiary = Color(0xFF6A6455),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECE7D8),
    onTertiaryContainer = Color(0xFF2A261C),
    error = Color(0xFF625C50),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFEAE5D8),
    onErrorContainer = Color(0xFF2B2820),
    background = Color(0xFFF1EFE8),
    onBackground = Color(0xFF1F211D),
    surface = Color(0xFFF1EFE8),
    onSurface = Color(0xFF1F211D),
    surfaceVariant = Color(0xFFE4E2D8),
    onSurfaceVariant = Color(0xFF565B50),
    surfaceTint = Color(0xFF2F3B2C),
    inverseSurface = Color(0xFF31332E),
    inverseOnSurface = Color(0xFFF1EFE8),
    outline = Color(0xFF7C8075),
    outlineVariant = Color(0xFFDCDAD0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFBFAF6),
    surfaceDim = Color(0xFFDAD8CF),
    surfaceContainerLowest = Color(0xFFFBFAF6),
    surfaceContainerLow = Color(0xFFF5F3ED),
    surfaceContainer = Color(0xFFEDEBE3),
    surfaceContainerHigh = Color(0xFFE7E5DC),
    surfaceContainerHighest = Color(0xFFE1DFD6),
)

val CalmDark = darkColorScheme(
    primary = Color(0xFFC9D4BC),
    onPrimary = Color(0xFF222C1F),
    primaryContainer = Color(0xFF38442F),
    onPrimaryContainer = Color(0xFFDDE3D3),
    inversePrimary = Color(0xFF2F3B2C),
    secondary = Color(0xFFB9C4AB),
    onSecondary = Color(0xFF263021),
    secondaryContainer = Color(0xFF3A4433),
    onSecondaryContainer = Color(0xFFE4E8DA),
    tertiary = Color(0xFFCFC8B5),
    onTertiary = Color(0xFF353122),
    tertiaryContainer = Color(0xFF4B4737),
    onTertiaryContainer = Color(0xFFECE7D8),
    error = Color(0xFFCFC8BA),
    onError = Color(0xFF35312A),
    errorContainer = Color(0xFF4C473E),
    onErrorContainer = Color(0xFFEAE5D8),
    background = Color(0xFF171A16),
    onBackground = Color(0xFFE4E3DB),
    surface = Color(0xFF171A16),
    onSurface = Color(0xFFE4E3DB),
    surfaceVariant = Color(0xFF42463D),
    onSurfaceVariant = Color(0xFFC3C7BA),
    surfaceTint = Color(0xFFC9D4BC),
    inverseSurface = Color(0xFFE4E3DB),
    inverseOnSurface = Color(0xFF31332E),
    outline = Color(0xFF8D9184),
    outlineVariant = Color(0xFF3A3E36),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3A3E37),
    surfaceDim = Color(0xFF171A16),
    surfaceContainerLowest = Color(0xFF20241E),
    surfaceContainerLow = Color(0xFF1C1F1A),
    surfaceContainer = Color(0xFF232721),
    surfaceContainerHigh = Color(0xFF2D312B),
    surfaceContainerHighest = Color(0xFF383C35),
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
