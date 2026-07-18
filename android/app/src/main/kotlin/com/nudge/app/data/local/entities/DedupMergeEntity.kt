package com.nudge.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local mirror of the server `dedup_merges` table — the audit trail behind M2's "near-duplicates
 * are merged, with merge visible in review history" requirement (closes the CLAUDE.md-flagged
 * gap: this table existed server-side with nothing on the client reading it). Read-only; merges
 * are only ever produced by the process-memo pipeline, never on-device.
 */
@Entity(tableName = "dedup_merges")
data class DedupMergeEntity(
    @PrimaryKey val id: String,
    val survivingTaskId: String,
    val mergedText: String,
    val mergedSourceMemoId: String?,
    val similarity: Float,
    val createdAt: Long,
)
