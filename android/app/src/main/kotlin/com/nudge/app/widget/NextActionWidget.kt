package com.nudge.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.nudge.app.R
import com.nudge.app.ServiceLocator
import com.nudge.app.capture.CaptureActivity
import com.nudge.app.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.first

val TaskIdKey = ActionParameters.Key<String>("task_id")

/**
 * The primary product surface (PRD §6: "the widget IS the product"). Resting state shows
 * exactly one task with two actions — never a list, never a remaining-count (M3). Glance
 * re-renders via explicit updateAll() calls (see WidgetActionCallbacks, UploadWorker,
 * TaskActionSyncWorker), not continuous observation — provideGlance takes one snapshot per
 * update.
 */
class NextActionWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lowBatteryMode = ServiceLocator.preferences.lowBatteryMode.first()
        val task = ServiceLocator.taskRepository.observeNextAction(lowBatteryMode).first()
        val hasAnyOpenTask = ServiceLocator.taskRepository.observeHasAnyOpenTask().first()

        provideContent {
            GlanceTheme {
                NextActionContent(task = task, hasAnyOpenTask = hasAnyOpenTask, lowBatteryMode = lowBatteryMode)
            }
        }
    }
}

@Composable
private fun NextActionContent(task: TaskEntity?, hasAnyOpenTask: Boolean, lowBatteryMode: Boolean) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        when {
            task != null -> TaskState(task)
            hasAnyOpenTask -> LowBatteryEmptyState()
            else -> EmptyState()
        }
        Spacer(modifier = GlanceModifier.size(8.dp))
        LowBatteryToggleRow(lowBatteryMode)
    }
}

@Composable
private fun TaskState(task: TaskEntity) {
    val context = LocalContext.current
    Text(
        text = task.text,
        style = TextStyle(fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onBackground),
    )
    Spacer(modifier = GlanceModifier.size(12.dp))
    Row {
        Text(
            text = context.getString(R.string.widget_action_done),
            modifier = GlanceModifier
                .clickable(actionRunCallback<DoneActionCallback>(actionParametersOf(TaskIdKey to task.id)))
                .padding(8.dp),
            style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = GlanceModifier.size(16.dp))
        Text(
            text = context.getString(R.string.widget_action_not_today),
            modifier = GlanceModifier
                .clickable(actionRunCallback<SkipActionCallback>(actionParametersOf(TaskIdKey to task.id)))
                .padding(8.dp),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

@Composable
private fun EmptyState() {
    val context = LocalContext.current
    Text(text = context.getString(R.string.widget_empty_state_title), style = TextStyle(fontWeight = FontWeight.Medium))
    Text(
        text = context.getString(R.string.widget_empty_state_body),
        modifier = GlanceModifier
            .clickable(actionStartActivity<CaptureActivity>())
            .padding(top = 8.dp),
    )
}

@Composable
private fun LowBatteryEmptyState() {
    // Never force-surfaces a heavy task during Low Battery Mode (M3 edge case) — this state
    // means "there IS work, just not light work right now."
    val context = LocalContext.current
    Text(text = context.getString(R.string.widget_low_battery_empty_title), style = TextStyle(fontWeight = FontWeight.Medium))
    Text(text = context.getString(R.string.widget_low_battery_empty_body))
}

@Composable
private fun LowBatteryToggleRow(lowBatteryMode: Boolean) {
    val context = LocalContext.current
    Text(
        text = context.getString(R.string.widget_low_battery_toggle) + if (lowBatteryMode) " • on" else "",
        modifier = GlanceModifier
            .clickable(actionRunCallback<ToggleLowBatteryActionCallback>())
            .padding(4.dp),
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
    )
}
