package com.nudge.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nudge.app.data.local.entities.MemoEntity
import com.nudge.app.data.local.entities.MemoUploadState
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Insert
    suspend fun insert(memo: MemoEntity)

    @Update
    suspend fun update(memo: MemoEntity)

    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getById(id: String): MemoEntity?

    @Query("SELECT * FROM memos WHERE uploadState IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getPendingUploads(): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE uploadState = 'STUCK'")
    fun observeStuckMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MemoEntity>>
}
