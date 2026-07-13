package com.nudge.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Insert payload for a new `memos` row. `id` is the client-generated UUID (see MemoEntity) so
// the same identity is used locally and on the server from the moment of capture.
@Serializable
data class MemoInsertDto(
    val id: String,
    @SerialName("audio_path") val audioPath: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    val status: String = "queued",
)

@Serializable
data class ProcessMemoRequest(@SerialName("memo_id") val memoId: String)

@Serializable
data class ProcessMemoResponse(
    @SerialName("memo_id") val memoId: String,
    val status: String,
    val error: String? = null,
)
