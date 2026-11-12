package com.nudge.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 4dp-grid spacing scale. Use these instead of inline dp values, in the app and the widget. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object Sizing {
    /** PRD §6: touch targets >= 48 dp. */
    val minTouchTarget = 48.dp
    val widgetCornerRadius = 24.dp
    val bottomBarItemHeight = 56.dp
    val stopButton = 72.dp
    val widgetIcon = 20.dp
    val border = 1.dp
    val borderStrong = 1.5.dp
}

/**
 * Glance text does not read MaterialTheme.typography, so the widget's sizes live here.
 * PRD §6: minimum 16 sp body text.
 */
object WidgetType {
    val task = 18.sp
    val body = 16.sp
}
