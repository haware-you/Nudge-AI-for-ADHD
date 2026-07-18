package com.nudge.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors the `dedup_merges` table in supabase/migrations/0001_schema.sql.
@Serializable
data class DedupMergeDto(
    val id: String,
    @SerialName("surviving_task_id") val survivingTaskId: String,
    @SerialName("merged_text") val mergedText: String,
    @SerialName("merged_source_memo_id") val mergedSourceMemoId: String? = null,
    val similarity: Float,
    @SerialName("created_at") val createdAt: String,
)
