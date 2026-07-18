package com.nudge.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local mirror of the server `notes` table (supabase/migrations/0001_schema.sql). Non-actionable
 * extraction output — venting/journaling that the pipeline explicitly declined to turn into a
 * task (M2: "the AI must never fabricate a task"). Read-only on the client; notes are never
 * created, edited, or promoted to tasks from the app.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val sourceMemoId: String?,
    val text: String,
    val createdAt: Long,
)
