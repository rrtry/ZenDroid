package com.example.volumeprofiler.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.volumeprofiler.entities.AlarmRelation
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface AlarmRelationDao {

    @Transaction
    @Query("SELECT * FROM Alarm WHERE Alarm.startProfileUUID = (:id) OR Alarm.endProfileUUID = (:id) AND Alarm.isScheduled = 1")
    suspend fun getActiveAlarmsByProfileId(id: UUID): List<AlarmRelation>?

    @Transaction
    @Query("SELECT * FROM Alarm")
    fun observeAlarms(): Flow<List<AlarmRelation>>

    @Transaction
    @Query("SELECT * FROM Alarm WHERE Alarm.startProfileUUID = (:id) OR Alarm.endProfileUUID = (:id) AND Alarm.isScheduled = 1")
    fun observeScheduledAlarmsByProfileId(id: UUID): Flow<List<AlarmRelation>?>

    @Transaction
    @Query("SELECT * FROM Alarm WHERE Alarm.isScheduled = 1")
    suspend fun getActiveAlarms(): List<AlarmRelation>?

    /*
    @Transaction
    @Query("SELECT * FROM Profile INNER JOIN Alarm ON profile.id = Alarm.startProfileUUID OR profile.id = Alarm.endProfileUUID WHERE profile.id = (:id) AND Alarm.isScheduled = 1")
    suspend fun getActiveAlarmsByProfileId(id: UUID): List<AlarmRelation>?

    @Transaction
    @Query("SELECT * FROM Profile INNER JOIN Alarm ON profile.id = Alarm.startProfileUUID OR profile.id = Alarm.endProfileUUID")
    fun observeAlarms(): Flow<List<AlarmRelation>>

    @Transaction
    @Query("SELECT * FROM Profile INNER JOIN Alarm ON profile.id = Alarm.startProfileUUID OR profile.id = Alarm.endProfileUUID WHERE profile.id = (:id) AND Alarm.isScheduled = 1")
    fun observeAlarmsByProfileId(id: UUID): Flow<List<AlarmRelation>?>

    @Transaction
    @Query("SELECT * FROM Profile INNER JOIN Alarm ON profile.id = Alarm.startProfileUUID OR profile.id = Alarm.endProfileUUID WHERE Alarm.isScheduled = 1")
    suspend fun getActiveAlarms(): List<AlarmRelation>?
     */
}