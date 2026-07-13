package com.nudge.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nudge.app.ServiceLocator

private const val UNIQUE_WORK_NAME = "task-action-sync"

/**
 * Flushes locally-applied Done/Skip actions to Supabase (see TaskRepository.flushPendingActions).
 * The widget/Review UI already reflects the action instantly from Room — this worker is purely
 * about eventual consistency with the server, so a single retry-with-backoff, non-urgent job is
 * enough; nothing here blocks the <5s UI update requirement.
 */
class TaskActionSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        ServiceLocator.taskRepository.flushPendingActions()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}

object TaskActionSyncScheduler {
    fun enqueue(workManager: WorkManager) {
        val request = OneTimeWorkRequestBuilder<TaskActionSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
