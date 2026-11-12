package com.nudge.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Effort { LOW, MEDIUM, HIGH }
enum class TaskStatus { OPEN, DONE, SKIPPED }

/**
 * Local mirror of the server `tasks` table (supabase/migrations/0001_schema.sql). The widget
 * and Review screen read this, not the network directly, so both work offline and update
 * within the widget's <5s Done/skip requirement (PRD §8) before a sync round-trip completes.
 *
 * [id] is always the server UUID — tasks are only ever created by the Edge Function pipeline,
 * never on-device, so there's no local-id/remote-id reconciliation to do here (contrast
 * [com.nudge.app.data.local.entities.MemoEntity], which the client creates first).
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val sourceMemoId: String?,
    val text: String,
    val sourceSpan: String?,
    val effort: Effort,
    val confidence: Float,
    val lowConfidence: Boolean,
    val status: TaskStatus,
    val skipCount: Int,
    val priorityScore: Double,
    val createdAt: Long,
    val updatedAt: Long,
    // True once a Done/Skip action has been applied optimistically here but not yet confirmed
    // synced to Supabase. TaskActionSyncWorker flushes rows where this is true.
    val pendingSync: Boolean = false,
    // How many near-duplicate mentions the server merged into this task (dedup_merges rows).
    // Display-only, for M2's "merge visible in review history".
    val mergeCount: Int = 0,
)
