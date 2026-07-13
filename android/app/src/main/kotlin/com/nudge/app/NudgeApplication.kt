package com.nudge.app

import android.app.Application
import androidx.work.WorkManager
import com.nudge.app.sync.TaskActionSyncScheduler
import com.nudge.app.sync.UploadScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NudgeApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        appScope.launch {
            ServiceLocator.ensureSignedIn()
            reconcilePendingWork()
        }
    }

    /**
     * Covers the case where the process died (or the device rebooted) between a memo being
     * queued/an action being applied locally and its WorkManager job actually running — PRD
     * §8: "captured audio must survive app kill, reboot, and 72h offline." WorkManager's own
     * queue already survives reboot given RECEIVE_BOOT_COMPLETED-free re-enqueue on next app
     * start is enough at this MVP's scale (no push-triggered background wake needed).
     */
    private suspend fun reconcilePendingWork() {
        val workManager = WorkManager.getInstance(this)
        ServiceLocator.database.memoDao().getPendingUploads().forEach { memo ->
            UploadScheduler.enqueue(workManager, memo.id)
        }
        if (ServiceLocator.database.taskDao().getPendingSync().isNotEmpty()) {
            TaskActionSyncScheduler.enqueue(workManager)
        }
    }
}
