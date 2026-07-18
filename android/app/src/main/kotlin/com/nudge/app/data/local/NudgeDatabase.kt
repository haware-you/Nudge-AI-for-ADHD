package com.nudge.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nudge.app.data.local.dao.DedupMergeDao
import com.nudge.app.data.local.dao.MemoDao
import com.nudge.app.data.local.dao.NoteDao
import com.nudge.app.data.local.dao.TaskDao
import com.nudge.app.data.local.entities.DedupMergeEntity
import com.nudge.app.data.local.entities.MemoEntity
import com.nudge.app.data.local.entities.NoteEntity
import com.nudge.app.data.local.entities.TaskEntity

@Database(
    entities = [TaskEntity::class, MemoEntity::class, NoteEntity::class, DedupMergeEntity::class],
    version = 2,
    // TODO: turn on schema export + commit schemas/ once this app has a real migration to test.
    // Not worth the ceremony yet with no shipped installs — fallbackToDestructiveMigration is
    // fine pre-launch (see Room builder below) since there's no user data to preserve across
    // this version bump.
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class NudgeDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun memoDao(): MemoDao
    abstract fun noteDao(): NoteDao
    abstract fun dedupMergeDao(): DedupMergeDao

    companion object {
        @Volatile private var instance: NudgeDatabase? = null

        fun getInstance(context: Context): NudgeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NudgeDatabase::class.java,
                    "nudge.db",
                )
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
