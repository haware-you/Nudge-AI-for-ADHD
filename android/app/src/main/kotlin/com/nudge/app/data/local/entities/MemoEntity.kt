package com.nudge.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MemoUploadState { PENDING, UPLOADING, UPLOADED, PROCESSING, DONE, FAILED, STUCK }

/**
 * Local capture queue (PRD §7: "Room DB local queue for audio + pending tasks"). [id] is
 * generated on-device at record time and reused as the `memos.id` primary key once uploaded,
 * so a memo has one identity from the moment the user taps the widget through to it appearing
 * in Review — no client/server id remapping, and the same id survives app kill/reboot.
 */
@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val localAudioFilePath: String,
    val durationSeconds: Int,
    val createdAt: Long,
    val uploadState: MemoUploadState = MemoUploadState.PENDING,
    val retryCount: Int = 0,
    val lastError: String? = null,
)
