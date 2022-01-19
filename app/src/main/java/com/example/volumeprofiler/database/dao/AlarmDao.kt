package com.example.volumeprofiler.database.dao

import androidx.room.*
import com.example.volumeprofiler.entities.Alarm

@Dao
interface AlarmDao {

    @Query("SELECT * FROM Alarm WHERE Alarm.eventId = (:id)")
    suspend fun getAlarm(id: Long): Alarm

    @Insert
    suspend fun addAlarm(alarm: Alarm)

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun removeAlarm(alarm: Alarm)
}