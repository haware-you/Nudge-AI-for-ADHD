package com.nudge.app.data.repository

import com.nudge.app.data.PriorityScore
import com.nudge.app.data.local.dao.TaskDao
import com.nudge.app.data.local.entities.Effort
import com.nudge.app.data.local.entities.TaskEntity
import com.nudge.app.data.local.entities.TaskStatus
import com.nudge.app.data.remote.SupabaseClientProvider
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
                // Recency comes from lastMentionedAt, NOT now: using now here made a skip reset
                // recency to "fresh", which outweighed the skip penalty and sent the skipped
                // task straight back to the top of the widget.
                priorityScore = PriorityScore.compute(task.effort, task.lastMentionedAt, newSkipCount, now),
                pendingSync = true,
            ),
        )
    }

    /** One-tap confirmation of an ambiguous/low-confidence task (M2 edge case) — the task text
     * itself is never rewritten, only the flag that surfaces the "confirm this one" affordance. */
    suspend fun confirmLowConfidence(task: TaskEntity) {
        taskDao.update(task.copy(lowConfidence = false, pendingSync = true))
    }

    suspend fun deleteTask(taskId: String) {
        taskDao.delete(taskId)
        SupabaseClientProvider.client.postgrest.from("tasks").delete {
            filter { eq("id", taskId) }
        }
    }

    /** Pulls the user's open/skipped tasks down from Postgrest and reconciles local rows. */
    suspend fun syncDown() {
        val remoteTasks = SupabaseClientProvider.client.postgrest.from("tasks")
            .select(columns = Columns.ALL) {
                filter { or { eq("status", "open"); eq("status", "skipped") } }
            }
            .decodeList<TaskDto>()

        taskDao.upsertAll(remoteTasks.map { it.toEntity() })
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

private fun TaskDto.toEntity(): TaskEntity = TaskEntity(
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
    lastMentionedAt = Instant.parse(lastMentionedAt).toEpochMilliseconds(),
    pendingSync = false,
)
