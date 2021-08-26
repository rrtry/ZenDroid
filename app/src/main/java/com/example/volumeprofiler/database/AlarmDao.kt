package com.example.volumeprofiler.database

import androidx.room.*
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.AlarmTrigger
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM Alarm WHERE Alarm.eventId = (:id)")
    fun observeAlarm(id: Long): Flow<Alarm>

    @Query("SELECT * FROM Alarm WHERE Alarm.eventId = (:id)")
    suspend fun getAlarm(id: Long): Alarm

    @Insert
    suspend fun addAlarm(alarm: Alarm)

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun removeAlarm(alarm: Alarm)

    @Transaction
    @Query("SELECT * FROM Profile INNER JOIN Alarm ON Alarm.eventId = (:id) AND Alarm.isScheduled = 1")
    fun observeScheduledAlarmWithProfile(id: Long): Flow<AlarmTrigger?>
}