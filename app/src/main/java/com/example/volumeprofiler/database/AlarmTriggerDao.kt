package com.example.volumeprofiler.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.volumeprofiler.models.AlarmTrigger
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface AlarmTriggerDao {

    @Transaction
    @Query("SELECT * FROM Profile INNER JOIN Alarm ON profile.id = Alarm.profileUUID WHERE profile.id = (:id) AND Alarm.isScheduled = 1")
    suspend fun getAlarmsByProfileId(id: UUID): List<AlarmTrigger>?

    @Transaction
    @Query("SELECT * FROM Profile INNER JOIN Alarm ON Alarm.eventId = (:id) AND Alarm.isScheduled = 1")
    fun observeScheduledAlarmWithProfile(id: Long): LiveData<AlarmTrigger?>

    @Transaction
    @Query("SELECT * FROM Profile INNER JOIN Alarm ON profile.id = Alarm.profileUUID")
    fun observeProfilesWithAlarms(): Flow<List<AlarmTrigger>>

    @Transaction
    @Query("SELECT * FROM Profile INNER JOIN Alarm ON profile.id = Alarm.profileUUID WHERE profile.id = (:id) AND Alarm.isScheduled = 1")
    fun observeProfileWithScheduledAlarms(id: UUID): Flow<List<AlarmTrigger>?>

    @Transaction
    @Query("SELECT * FROM Profile INNER JOIN Alarm ON profile.id = Alarm.profileUUID WHERE Alarm.isScheduled = 1")
    suspend fun getProfilesWithScheduledAlarms(): List<AlarmTrigger>?
}