package com.nudge.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors the `notes` table in supabase/migrations/0001_schema.sql.
@Serializable
data class NoteDto(
    val id: String,
    @SerialName("source_memo_id") val sourceMemoId: String? = null,
    val text: String,
    @SerialName("created_at") val createdAt: String,
)
