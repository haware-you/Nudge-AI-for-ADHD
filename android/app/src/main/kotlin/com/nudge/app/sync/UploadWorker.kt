package com.nudge.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.glance.appwidget.updateAll
import com.nudge.app.ServiceLocator
import com.nudge.app.data.local.entities.MemoUploadState
import com.nudge.app.widget.NextActionWidget
import java.util.concurrent.TimeUnit

private const val KEY_MEMO_ID = "memo_id"
private const val MAX_RETRIES = 3

/**
 * Retryable upload/processing job (PRD §7: "WorkManager for retryable upload/processing jobs").
 * One instance handles one memo end to end: upload audio -> insert memos row -> invoke the
 * process-memo Edge Function. WorkManager itself retries per its backoff policy on transient
 * failure; retryCount tracked in Room is what enforces M1's "3 retries -> stuck" rule, which is
 * a durability/UX ceiling independent of however many times WorkManager itself has retried.
 */
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val memoId = inputData.getString(KEY_MEMO_ID) ?: return Result.failure()
        val memoDao = ServiceLocator.database.memoDao()
        val memo = memoDao.getById(memoId) ?: return Result.failure()

        ServiceLocator.captureRepository.markUploadState(memoId, MemoUploadState.UPLOADING)

        return try {
            ServiceLocator.captureRepository.uploadAndProcess(memo)
            ServiceLocator.captureRepository.markUploadState(memoId, MemoUploadState.DONE)
            ServiceLocator.taskRepository.syncDown()
            NextActionWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            val retryCount = memo.retryCount + 1
            if (retryCount >= MAX_RETRIES) {
                // "item marked stuck, user notified once" (M1) — the notification itself is a
                // follow-up; observeStuckMemos() in MemoDao is what a notification listener
                // would subscribe to.
                ServiceLocator.captureRepository.markUploadState(
                    memoId,
                    MemoUploadState.STUCK,
                    error = e.message,
                    retryCount = retryCount,
                )
                Result.failure()
            } else {
                ServiceLocator.captureRepository.markUploadState(
                    memoId,
                    MemoUploadState.FAILED,
                    error = e.message,
                    retryCount = retryCount,
                )
                Result.retry()
            }
        }
    }
}

object UploadScheduler {
    fun enqueue(workManager: WorkManager, memoId: String) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(KEY_MEMO_ID to memoId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // Unique per memo: a retry after process death shouldn't double-enqueue the same upload.
        workManager.enqueueUniqueWork(memoId, ExistingWorkPolicy.KEEP, request)
    }
}
