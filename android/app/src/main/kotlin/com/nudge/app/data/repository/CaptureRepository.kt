package com.nudge.app.data.repository

import android.content.Context
import com.nudge.app.data.local.dao.MemoDao
import com.nudge.app.data.local.entities.MemoEntity
import com.nudge.app.data.local.entities.MemoUploadState
import com.nudge.app.data.remote.SupabaseClientProvider
import com.nudge.app.data.remote.dto.MemoInsertDto
import com.nudge.app.data.remote.dto.ProcessMemoRequest
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import java.io.File

/**
 * Owns the local capture queue (PRD §7: "Room DB local queue for audio + pending tasks").
 * AudioRecorderController writes the actual bytes; this repository is what makes the memo
 * durable across app kill/reboot the moment recording stops, before any network is involved.
 */
class CaptureRepository(
    private val context: Context,
    private val memoDao: MemoDao,
) {
    private val memosDir: File
        get() = File(context.filesDir, "memos").apply { mkdirs() }

    fun newAudioFile(memoId: String): File = File(memosDir, "$memoId.m4a")

    suspend fun createPendingMemo(memoId: String, audioFile: File, durationSeconds: Int) {
        memoDao.insert(
            MemoEntity(
                id = memoId,
                localAudioFilePath = audioFile.absolutePath,
                durationSeconds = durationSeconds,
                createdAt = System.currentTimeMillis(),
                uploadState = MemoUploadState.PENDING,
            ),
        )
    }

    /** Discard a capture that never produced meaningful audio (e.g. instant tap-tap). */
    suspend fun discardMemo(memoId: String) {
        val memo = memoDao.getById(memoId) ?: return
        File(memo.localAudioFilePath).delete()
    }

    /**
     * Runs on a WorkManager background thread (see UploadWorker). Uploads the audio file,
     * inserts the `memos` row, then invokes the process-memo Edge Function. Any exception
     * here is caught by the caller, which increments retryCount per M1's "3 retries -> stuck"
     * rule — this function itself just does the work and throws on failure.
     */
    suspend fun uploadAndProcess(memo: MemoEntity) {
        val audioFile = File(memo.localAudioFilePath)
        val storagePath = "${currentUserId()}/${memo.id}.m4a"

        SupabaseClientProvider.client.storage.from("audio")
            .upload(storagePath, audioFile.readBytes())

        SupabaseClientProvider.client.postgrest.from("memos").insert(
            MemoInsertDto(
                id = memo.id,
                audioPath = storagePath,
                durationSeconds = memo.durationSeconds,
            ),
        )

        SupabaseClientProvider.client.functions.invoke(
            "process-memo",
            body = ProcessMemoRequest(memoId = memo.id),
        )

        audioFile.delete() // server has it now; local copy was only for offline durability
    }

    suspend fun markUploadState(memoId: String, state: MemoUploadState, error: String? = null, retryCount: Int? = null) {
        val memo = memoDao.getById(memoId) ?: return
        memoDao.update(
            memo.copy(
                uploadState = state,
                lastError = error,
                retryCount = retryCount ?: memo.retryCount,
            ),
        )
    }

    private fun currentUserId(): String =
        SupabaseClientProvider.client.auth.currentUserOrNull()?.id
            ?: error("uploadAndProcess called with no signed-in user")
}
