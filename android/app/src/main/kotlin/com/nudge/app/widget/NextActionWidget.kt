package com.nudge.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nudge.app.R
import com.nudge.app.ServiceLocator
import com.nudge.app.capture.CaptureActivity
import com.nudge.app.data.local.entities.TaskEntity
import com.nudge.app.ui.theme.CalmDark
import com.nudge.app.ui.theme.CalmLight
import com.nudge.app.ui.theme.Sizing
import com.nudge.app.ui.theme.Spacing
import com.nudge.app.ui.theme.WidgetType
import kotlinx.coroutines.flow.first

val TaskIdKey = ActionParameters.Key<String>("task_id")

// Same palette as the in-app theme (ui/theme/Theme.kt), so widget and app match.
private val CalmWidgetColors = ColorProviders(light = CalmLight, dark = CalmDark)

/**
 * The primary product surface (PRD §6: "the widget IS the product"). Resting state shows
 * exactly one task with two actions — never a list, never a remaining-count (M3). Glance
 * re-renders via explicit updateAll() calls (see WidgetActionCallbacks, UploadWorker,
 * TaskActionSyncWorker), not continuous observation — provideGlance takes one snapshot per
 * update.
 *
 * Styling rules for this file: every Text sets an explicit color and a size from WidgetType
 * (Glance's defaults are not theme-aware), and every tappable element is at least
 * Sizing.minTouchTarget tall.
 */
class NextActionWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lowBatteryMode = ServiceLocator.preferences.lowBatteryMode.first()
        val dynamicColor = ServiceLocator.preferences.dynamicColor.first()
        val task = ServiceLocator.taskRepository.observeNextAction(lowBatteryMode).first()
        val hasAnyOpenTask = ServiceLocator.taskRepository.observeHasAnyOpenTask().first()

        provideContent {
            // GlanceTheme's own default is the wallpaper-derived scheme on Android 12+, so the
            // calm palette has to be passed explicitly unless the user opted in to dynamic color.
            val content: @Composable () -> Unit = {
                NextActionContent(task = task, hasAnyOpenTask = hasAnyOpenTask, lowBatteryMode = lowBatteryMode)
            }
            if (dynamicColor) GlanceTheme(content = content) else GlanceTheme(colors = CalmWidgetColors, content = content)
        }
    }
}

@Composable
private fun NextActionContent(task: TaskEntity?, hasAnyOpenTask: Boolean, lowBatteryMode: Boolean) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(Sizing.widgetCornerRadius)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        // The state area takes whatever height is left after the fixed 48dp rows below it.
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            when {
                task != null -> WidgetText(task.text, size = WidgetType.task, weight = FontWeight.Medium, maxLines = 3)
                hasAnyOpenTask -> LowBatteryEmptyState()
                else -> EmptyState()
            }
        }
        if (task != null) TaskActions(task)
        LowBatteryToggleRow(lowBatteryMode)
    }
}

@Composable
private fun TaskActions(task: TaskEntity) {
    val context = LocalContext.current
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        WidgetAction(
            label = context.getString(R.string.widget_action_done),
            color = GlanceTheme.colors.primary,
            weight = FontWeight.Bold,
            action = actionRunCallback<DoneActionCallback>(actionParametersOf(TaskIdKey to task.id)),
        )
        Spacer(modifier = GlanceModifier.width(Spacing.lg))
        WidgetAction(
            label = context.getString(R.string.widget_action_not_today),
            color = GlanceTheme.colors.onSurfaceVariant,
            weight = FontWeight.Normal,
            action = actionRunCallback<SkipActionCallback>(actionParametersOf(TaskIdKey to task.id)),
        )
    }
}

@Composable
private fun EmptyState() {
    val context = LocalContext.current
    // The whole state is the tap target, not just the body line.
    Column(modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<CaptureActivity>())) {
        WidgetText(context.getString(R.string.widget_empty_state_title), weight = FontWeight.Medium)
        WidgetText(context.getString(R.string.widget_empty_state_body), color = GlanceTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun LowBatteryEmptyState() {
    // Never force-surfaces a heavy task during Low Battery Mode (M3 edge case) — this state
    // means "there IS work, just not light work right now."
    val context = LocalContext.current
    Column {
        WidgetText(context.getString(R.string.widget_low_battery_empty_title), weight = FontWeight.Medium)
        WidgetText(context.getString(R.string.widget_low_battery_empty_body), color = GlanceTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun LowBatteryToggleRow(lowBatteryMode: Boolean) {
    val context = LocalContext.current
    WidgetAction(
        label = context.getString(
            if (lowBatteryMode) R.string.widget_low_battery_on else R.string.settings_low_battery_mode,
        ),
        color = GlanceTheme.colors.onSurfaceVariant,
        weight = FontWeight.Normal,
        action = actionRunCallback<ToggleLowBatteryActionCallback>(),
    )
}

/** A text action with a touch target of at least 48dp in height. */
@Composable
private fun WidgetAction(label: String, color: ColorProvider, weight: FontWeight, action: Action) {
    Box(
        modifier = GlanceModifier
            .height(Sizing.minTouchTarget)
            .clickable(action)
            .padding(horizontal = Spacing.sm),
        contentAlignment = Alignment.CenterStart,
    ) {
        WidgetText(label, color = color, weight = weight, maxLines = 1)
    }
}

/** The only Text wrapper the widget should use: always an explicit themed color and size. */
@Composable
private fun WidgetText(
    text: String,
    color: ColorProvider = GlanceTheme.colors.onBackground,
    size: TextUnit = WidgetType.body,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(text = text, style = TextStyle(color = color, fontSize = size, fontWeight = weight), maxLines = maxLines)
}
