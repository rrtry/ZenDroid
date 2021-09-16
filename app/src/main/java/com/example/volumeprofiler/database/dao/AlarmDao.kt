package com.example.volumeprofiler.database.dao

import androidx.room.*
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.AlarmTrigger
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Insert
    suspend fun addAlarm(alarm: Alarm)

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun removeAlarm(alarm: Alarm)
}