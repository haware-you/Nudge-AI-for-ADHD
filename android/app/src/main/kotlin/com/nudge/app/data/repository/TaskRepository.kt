package com.nudge.app.data.repository

import com.nudge.app.data.PriorityScore
import com.nudge.app.data.local.dao.TaskDao
import com.nudge.app.data.local.entities.Effort
import com.nudge.app.data.local.entities.TaskEntity
import com.nudge.app.data.local.entities.TaskStatus
import com.nudge.app.data.remote.SupabaseClientProvider
import com.nudge.app.data.remote.dto.DedupMergeDto
import com.nudge.app.data.remote.dto.TaskDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Local Room is the source of truth for what the widget/Review screen render (works offline,
 * meets the <5s Done/skip UI requirement). Supabase is authoritative for anything the
 * server-side pipeline produced (new tasks, dedup merges, recomputed scores) and is
 * reconciled in via [syncDown]. See SupabaseClientProvider's note: this file's exact
 * supabase-kt call shapes are unverified against a live Gradle sync.
 */
class TaskRepository(private val taskDao: TaskDao) {

    /** maxEffortRank: 0 = low battery mode (low only), 2 = normal (any effort). */
    fun observeNextAction(lowBatteryMode: Boolean): Flow<TaskEntity?> =
        taskDao.observeTopTask(if (lowBatteryMode) 0 else 2)

    fun observeHasAnyOpenTask(): Flow<Boolean> = taskDao.observeHasAnyOpenTask()

    fun observeReviewList(): Flow<List<TaskEntity>> = taskDao.observeReviewList()

    suspend fun markDone(task: TaskEntity) {
        taskDao.update(task.copy(status = TaskStatus.DONE, pendingSync = true))
    }

    /** "Not Today": silently re-queued, never a visible penalty (PRD M3). */
    suspend fun markSkipped(task: TaskEntity) {
        val newSkipCount = task.skipCount + 1
        val now = System.currentTimeMillis()
        taskDao.update(
            task.copy(
                skipCount = newSkipCount,
                updatedAt = now,
                priorityScore = PriorityScore.compute(task.effort, now, newSkipCount, now),
                pendingSync = true,
            ),
        )
    }

    /** One-tap confirmation of an ambiguous/low-confidence task (M2 edge case) — the task text
     * itself is never rewritten, only the flag that surfaces the "confirm this one" affordance. */
    suspend fun confirmLowConfidence(task: TaskEntity) {
        taskDao.update(task.copy(lowConfidence = false, pendingSync = true))
    }

    /**
     * Review delete is two-step so it can be undone: the row disappears locally at once
     * (PRD M5: "deletion is instant and unceremonious"), and the server delete only happens
     * once the undo window has passed. If the app dies in between, the task comes back on the
     * next [syncDown] — the safe direction to fail in.
     */
    suspend fun deleteTaskLocally(taskId: String) {
        taskDao.delete(taskId)
    }

    suspend fun restoreTask(task: TaskEntity) {
        taskDao.upsertAll(listOf(task))
    }

    suspend fun deleteTaskRemotely(taskId: String) {
        runCatching {
            SupabaseClientProvider.client.postgrest.from("tasks").delete {
                filter { eq("id", taskId) }
            }
        }
    }

    /** Pulls the user's open/skipped tasks down from Postgrest and reconciles local rows. */
    suspend fun syncDown() {
        val remoteTasks = SupabaseClientProvider.client.postgrest.from("tasks")
            .select(columns = Columns.ALL) {
                filter { or { eq("status", "open"); eq("status", "skipped") } }
            }
            .decodeList<TaskDto>()

        // RLS limits this to the signed-in user's rows. A failure here only loses the "merged"
        // label, so it must not fail the whole sync.
        val mergeCounts = runCatching {
            SupabaseClientProvider.client.postgrest.from("dedup_merges")
                .select(columns = Columns.list("surviving_task_id"))
                .decodeList<DedupMergeDto>()
                .groupingBy { it.survivingTaskId }
                .eachCount()
        }.getOrDefault(emptyMap())

        taskDao.upsertAll(remoteTasks.map { it.toEntity(mergeCount = mergeCounts[it.id] ?: 0) })
    }

    /** Flushes any locally-applied Done/Skip actions that haven't reached the server yet. */
    suspend fun flushPendingActions() {
        val pending = taskDao.getPendingSync()
        for (task in pending) {
            runCatching {
                SupabaseClientProvider.client.postgrest.from("tasks").update(
                    mapOf(
                        "status" to task.status.name.lowercase(),
                        "skip_count" to task.skipCount,
                        "low_confidence" to task.lowConfidence,
                    ),
                ) {
                    filter { eq("id", task.id) }
                }
            }.onSuccess {
                taskDao.update(task.copy(pendingSync = false))
            }
            // On failure, the row stays pendingSync=true and is retried on the next flush
            // (invoked by TaskActionSyncWorker, WorkManager handles the retry cadence).
        }
    }
}

private fun TaskDto.toEntity(mergeCount: Int): TaskEntity = TaskEntity(
    id = id,
    sourceMemoId = sourceMemoId,
    text = text,
    sourceSpan = sourceSpan,
    effort = Effort.valueOf(effort.uppercase()),
    confidence = confidence,
    lowConfidence = lowConfidence,
    status = TaskStatus.valueOf(status.uppercase()),
    skipCount = skipCount,
    priorityScore = priorityScore,
    createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
    updatedAt = Instant.parse(updatedAt).toEpochMilliseconds(),
    pendingSync = false,
    mergeCount = mergeCount,
)
