package com.nudge.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.work.WorkManager
import com.nudge.app.ServiceLocator
import com.nudge.app.sync.TaskActionSyncScheduler
import kotlinx.coroutines.flow.first

class DoneActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        val task = ServiceLocator.database.taskDao().getById(taskId) ?: return
        ServiceLocator.taskRepository.markDone(task)
        TaskActionSyncScheduler.enqueue(WorkManager.getInstance(context))
        NextActionWidget().updateAll(context) // PRD M3: widget update < 5s after Done/skip
    }
}

class SkipActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        val task = ServiceLocator.database.taskDao().getById(taskId) ?: return
        ServiceLocator.taskRepository.markSkipped(task) // silently re-queued, no visible penalty
        TaskActionSyncScheduler.enqueue(WorkManager.getInstance(context))
        NextActionWidget().updateAll(context)
    }
}

class ToggleLowBatteryActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val currentValue = ServiceLocator.preferences.lowBatteryMode.first()
        ServiceLocator.preferences.setLowBatteryMode(!currentValue)
        NextActionWidget().updateAll(context)
    }
}
