package com.nudge.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * Flow A's "prompt to add the widget (system widget picker)" step, fired once the onboarding
 * sample capture has gone through. Best-effort: not every launcher supports the pin-widget
 * request, and even where it's supported there's no reliable callback for whether the user
 * actually completed the placement — this fires the system prompt and lets the OS/launcher
 * take it from there (API 26+ only, which matches this app's minSdk).
 */
fun requestPinNextActionWidget(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, NextActionWidgetReceiver::class.java)
    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        appWidgetManager.requestPinAppWidget(provider, null, null)
    }
}
