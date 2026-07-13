package com.nudge.app.data.local

import androidx.room.TypeConverter
import com.nudge.app.data.local.entities.Effort
import com.nudge.app.data.local.entities.MemoUploadState
import com.nudge.app.data.local.entities.TaskStatus

class Converters {
    @TypeConverter
    fun effortFromString(value: String): Effort = Effort.valueOf(value)

    @TypeConverter
    fun effortToString(value: Effort): String = value.name

    @TypeConverter
    fun taskStatusFromString(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun taskStatusToString(value: TaskStatus): String = value.name

    @TypeConverter
    fun uploadStateFromString(value: String): MemoUploadState = MemoUploadState.valueOf(value)

    @TypeConverter
    fun uploadStateToString(value: MemoUploadState): String = value.name
}
