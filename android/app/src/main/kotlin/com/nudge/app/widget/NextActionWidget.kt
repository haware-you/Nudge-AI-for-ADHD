package com.nudge.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.size
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
    // Card layout, top to bottom: [low battery toggle | capture], the one task, [Done | Not today].
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(Sizing.widgetCornerRadius)
            .padding(Spacing.md),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = GlanceModifier.defaultWeight()) { LowBatteryToggle(lowBatteryMode) }
            CaptureButton()
        }
        // The state area takes whatever height is left between the two fixed 48dp rows.
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(horizontal = Spacing.xs),
            contentAlignment = Alignment.CenterStart,
        ) {
            when {
                task != null -> WidgetText(task.text, size = WidgetType.task, weight = FontWeight.Medium, maxLines = 3)
                hasAnyOpenTask -> LowBatteryEmptyState()
                else -> EmptyState()
            }
        }
        if (task != null) TaskActions(task)
    }
}

@Composable
private fun TaskActions(task: TaskEntity) {
    val context = LocalContext.current
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Done: filled pill with a check, the one visually primary action on the widget.
        Row(
            modifier = GlanceModifier
                .height(Sizing.minTouchTarget)
                .background(GlanceTheme.colors.primary)
                .cornerRadius(Sizing.widgetCornerRadius)
                .clickable(actionRunCallback<DoneActionCallback>(actionParametersOf(TaskIdKey to task.id)))
                .padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_check),
                contentDescription = null,
                modifier = GlanceModifier.size(Sizing.widgetIcon),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
            )
            Spacer(modifier = GlanceModifier.width(Spacing.sm))
            WidgetText(
                context.getString(R.string.widget_action_done),
                color = GlanceTheme.colors.onPrimary,
                weight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.width(Spacing.sm))
        WidgetAction(
            label = context.getString(R.string.widget_action_not_today),
            color = GlanceTheme.colors.onSurfaceVariant,
            action = actionRunCallback<SkipActionCallback>(actionParametersOf(TaskIdKey to task.id)),
        )
    }
}

/** Always present, so capture is one tap away whatever the widget is showing (M1). */
@Composable
private fun CaptureButton() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .size(Sizing.minTouchTarget)
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(Sizing.widgetCornerRadius)
            .clickable(actionStartActivity<CaptureActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_mic),
            contentDescription = context.getString(R.string.capture_cta),
            modifier = GlanceModifier.size(Sizing.widgetIcon),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
        )
    }
}

@Composable
private fun EmptyState() {
    val context = LocalContext.current
    // The whole state is a tap target for capture, as well as the mic button.
    Column(modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<CaptureActivity>())) {
        WidgetText(context.getString(R.string.widget_empty_state_title), size = WidgetType.task, weight = FontWeight.Medium)
        WidgetText(context.getString(R.string.widget_empty_state_body), color = GlanceTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun LowBatteryEmptyState() {
    // Never force-surfaces a heavy task during Low Battery Mode (M3 edge case) — this state
    // means "there IS work, just not light work right now."
    val context = LocalContext.current
    Column {
        WidgetText(context.getString(R.string.widget_low_battery_empty_title), size = WidgetType.task, weight = FontWeight.Medium)
        WidgetText(context.getString(R.string.widget_low_battery_empty_body), color = GlanceTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun LowBatteryToggle(lowBatteryMode: Boolean) {
    val context = LocalContext.current
    WidgetAction(
        label = context.getString(
            if (lowBatteryMode) R.string.widget_low_battery_on else R.string.settings_low_battery_mode,
        ),
        color = GlanceTheme.colors.onSurfaceVariant,
        action = actionRunCallback<ToggleLowBatteryActionCallback>(),
    )
}

/** A text action with a touch target of at least 48dp in height. */
@Composable
private fun WidgetAction(label: String, color: ColorProvider, action: Action) {
    Box(
        modifier = GlanceModifier
            .height(Sizing.minTouchTarget)
            .clickable(action)
            .padding(horizontal = Spacing.xs),
        contentAlignment = Alignment.CenterStart,
    ) {
        WidgetText(label, color = color, maxLines = 1)
    }
}

/** The only Text wrapper the widget should use: always an explicit themed color and size. */
@Composable
private fun WidgetText(
    text: String,
    color: ColorProvider = GlanceTheme.colors.onSurface,
    size: TextUnit = WidgetType.body,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(text = text, style = TextStyle(color = color, fontSize = size, fontWeight = weight), maxLines = maxLines)
}
