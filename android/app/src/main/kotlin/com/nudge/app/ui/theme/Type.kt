package com.nudge.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Type rules (from the apple-design skill, §15, adapted to Compose):
//  - System font. It already ships tuned metrics; no custom face.
//  - Tracking depends on size: large text is tightened, body stays at 0. Never one value for all.
//  - Leading runs the other way: tight on large text, looser on body.
//  - Hierarchy comes from weight + size together, not from uppercase or colour.
// PRD §6: everything a person reads as content is >= 16sp. labelMedium/labelSmall are for short
// tags and tab labels only, never sentences or task text.
val NudgeTypography = Typography(
    // Large numerals (the capture timer).
    displayMedium = TextStyle(fontSize = 48.sp, lineHeight = 52.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em),
    // Screen titles.
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em),
    // Row titles and task text.
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
)

val NudgeShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
