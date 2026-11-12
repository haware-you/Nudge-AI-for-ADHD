package com.nudge.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors the `tasks` table in supabase/migrations/0001_schema.sql. Field names use
// @SerialName rather than renaming Kotlin properties so this reads naturally on both sides.
@Serializable
data class TaskDto(
    val id: String,
    @SerialName("source_memo_id") val sourceMemoId: String? = null,
    val text: String,
    @SerialName("source_span") val sourceSpan: String? = null,
    val effort: String,
    val confidence: Float,
    @SerialName("low_confidence") val lowConfidence: Boolean,
    val status: String,
    @SerialName("skip_count") val skipCount: Int,
    @SerialName("priority_score") val priorityScore: Double,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("last_mentioned_at") val lastMentionedAt: String,
)

@Serializable
data class TaskStatusUpdate(
    val status: String,
    @SerialName("skip_count") val skipCount: Int? = null,
)
