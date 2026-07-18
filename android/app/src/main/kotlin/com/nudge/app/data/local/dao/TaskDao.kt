package com.nudge.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nudge.app.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    /**
     * The widget's single source of truth (M3: "resting state shows exactly ONE task").
     * [maxEffortRank] implements Low Battery Mode (S1) as a plain filter: 0 = low only,
     * 1 = low+medium, 2 = everything. Kept as an int rather than joining on the Effort enum
     * so the ORDER BY/WHERE stays a single simple query.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE status = 'OPEN'
        AND (
            (:maxEffortRank >= 2) OR
            (:maxEffortRank >= 1 AND effort != 'HIGH') OR
            (:maxEffortRank >= 0 AND effort = 'LOW')
        )
        ORDER BY priorityScore DESC
        LIMIT 1
        """,
    )
    fun observeTopTask(maxEffortRank: Int): Flow<TaskEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM tasks WHERE status = 'OPEN')")
    fun observeHasAnyOpenTask(): Flow<Boolean>

    @Query("SELECT * FROM tasks WHERE sourceMemoId = :memoId ORDER BY createdAt DESC")
    fun observeTasksForMemo(memoId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status != 'DONE' ORDER BY createdAt DESC")
    fun observeReviewList(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT id FROM tasks WHERE status = 'OPEN'")
    suspend fun getOpenTaskIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tasks WHERE status = 'OPEN'")
    suspend fun deleteAllOpen()
}
