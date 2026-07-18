package com.nudge.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nudge.app.data.local.entities.DedupMergeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DedupMergeDao {
    @Query("SELECT * FROM dedup_merges ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DedupMergeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(merges: List<DedupMergeEntity>)
}
