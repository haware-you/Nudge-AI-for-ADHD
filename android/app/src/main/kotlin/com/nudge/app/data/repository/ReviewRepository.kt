package com.nudge.app.data.repository

import com.nudge.app.data.local.dao.DedupMergeDao
import com.nudge.app.data.local.dao.NoteDao
import com.nudge.app.data.local.entities.DedupMergeEntity
import com.nudge.app.data.local.entities.NoteEntity
import com.nudge.app.data.remote.SupabaseClientProvider
import com.nudge.app.data.remote.dto.DedupMergeDto
import com.nudge.app.data.remote.dto.NoteDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/**
 * Backs the Review & Correct screen's two read-only supporting surfaces (M2/M5): notes kept
 * verbatim from venting/journaling memos, and the dedup-merge audit trail ("merge visible in
 * review history"). Split out from [TaskRepository] because both are pull-only mirrors of
 * pipeline output — nothing here is ever written from the client.
 */
class ReviewRepository(
    private val noteDao: NoteDao,
    private val dedupMergeDao: DedupMergeDao,
) {
    fun observeNotes(): Flow<List<NoteEntity>> = noteDao.observeAll()

    /** Task id -> number of memos merged into it, for the "merged from N memos" badge. */
    fun observeMergeCounts(): Flow<Map<String, Int>> =
        dedupMergeDao.observeAll().map { merges -> merges.groupingBy { it.survivingTaskId }.eachCount() }

    /** Pulls notes and dedup-merge records down from Postgrest. Called alongside
     * [TaskRepository.syncDown] after a memo finishes processing (see UploadWorker). */
    suspend fun syncDown() {
        val remoteNotes = SupabaseClientProvider.client.postgrest.from("notes")
            .select(columns = Columns.ALL)
            .decodeList<NoteDto>()
        noteDao.upsertAll(remoteNotes.map { it.toEntity() })

        val remoteMerges = SupabaseClientProvider.client.postgrest.from("dedup_merges")
            .select(columns = Columns.ALL)
            .decodeList<DedupMergeDto>()
        dedupMergeDao.upsertAll(remoteMerges.map { it.toEntity() })
    }
}

private fun NoteDto.toEntity(): NoteEntity = NoteEntity(
    id = id,
    sourceMemoId = sourceMemoId,
    text = text,
    createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
)

private fun DedupMergeDto.toEntity(): DedupMergeEntity = DedupMergeEntity(
    id = id,
    survivingTaskId = survivingTaskId,
    mergedText = mergedText,
    mergedSourceMemoId = mergedSourceMemoId,
    similarity = similarity,
    createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
)
