package com.nudge.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// PRD §6: "minimum 16 sp body text" — the default M3 type scale's bodyLarge/bodyMedium already
// clear this, this override exists mainly to make the floor explicit rather than incidental.
val NudgeTypography = Typography(
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
)
