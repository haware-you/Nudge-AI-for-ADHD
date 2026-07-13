package com.nudge.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nudge.app.data.local.dao.MemoDao
import com.nudge.app.data.local.dao.TaskDao
import com.nudge.app.data.local.entities.MemoEntity
import com.nudge.app.data.local.entities.TaskEntity

@Database(
    entities = [TaskEntity::class, MemoEntity::class],
    version = 1,
    // TODO: turn on schema export + commit schemas/ once this app has a real migration to test.
    // Not worth the ceremony yet at version 1 with no shipped installs.
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class NudgeDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun memoDao(): MemoDao

    companion object {
        @Volatile private var instance: NudgeDatabase? = null

        fun getInstance(context: Context): NudgeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NudgeDatabase::class.java,
                    "nudge.db",
                ).build().also { instance = it }
            }
    }
}
