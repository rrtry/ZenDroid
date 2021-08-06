package com.example.volumeprofiler.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.AlarmTrigger
import java.util.*

@Dao
interface AlarmDao {

    @Query("SELECT * FROM Alarm WHERE Alarm.eventId = (:id)")
    fun observeAlarm(id: Long): LiveData<Alarm>

    @Query("SELECT * FROM Alarm WHERE Alarm.eventId = (:id)")
    fun getAlarm(id: Long): Alarm

    @Insert
    suspend fun addAlarm(alarm: Alarm)

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun removeAlarm(alarm: Alarm)

    @Transaction
    @Query("SELECT * FROM Profile INNER JOIN Alarm ON profile.id = Alarm.profileUUID WHERE profile.id = (:id) AND Alarm.isScheduled = 1")
    suspend fun getAlarmsByProfileId(id: UUID): List<AlarmTrigger>?

    @Transaction
    @Query("SELECT * FROM Profile INNER JOIN Alarm ON Alarm.eventId = (:id) AND Alarm.isScheduled = 1")
    fun observeScheduledAlarmWithProfile(id: Long): LiveData<AlarmTrigger?>
}