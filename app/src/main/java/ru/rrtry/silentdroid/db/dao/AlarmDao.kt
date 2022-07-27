package ru.rrtry.silentdroid.db.dao

import androidx.room.*
import ru.rrtry.silentdroid.entities.Alarm

@Dao
interface AlarmDao {

    @Query("SELECT * FROM Alarm WHERE Alarm.eventId = (:id)")
    suspend fun getAlarm(id: Long): Alarm

    @Insert
    suspend fun addAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun removeAlarm(alarm: Alarm)
}