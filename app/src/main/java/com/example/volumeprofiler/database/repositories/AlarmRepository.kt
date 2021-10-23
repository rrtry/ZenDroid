package com.example.volumeprofiler.database.repositories

import com.example.volumeprofiler.database.dao.AlarmDao
import com.example.volumeprofiler.database.dao.AlarmRelationDao
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.AlarmRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
        private val alarmDao: AlarmDao,
        private val alarmRelationDao: AlarmRelationDao) {

    suspend fun addAlarm(alarm: Alarm) {
        withContext(Dispatchers.IO) {
            alarmDao.addAlarm(alarm)
        }
    }

    suspend fun updateAlarm(alarm: Alarm) {
        withContext(Dispatchers.IO) {
            alarmDao.updateAlarm(alarm)
        }
    }

    suspend fun removeAlarm(alarm: Alarm) {
        withContext(Dispatchers.IO) {
            alarmDao.removeAlarm(alarm)
        }
    }

    suspend fun getEnabledAlarms(): List<AlarmRelation>? {
        return withContext(Dispatchers.IO) {
            alarmRelationDao.getActiveAlarms()
        }
    }

    suspend fun getScheduledAlarmsByProfileId(id: UUID): List<AlarmRelation>? {
        return withContext(Dispatchers.IO) {
            alarmRelationDao.getActiveAlarmsByProfileId(id)
        }
    }

    suspend fun getScheduledAlarm(id: Long): Alarm {
        return withContext(Dispatchers.IO) {
            alarmDao.getAlarm(id)
        }
    }

    fun observeAlarms(): Flow<List<AlarmRelation>> = alarmRelationDao.observeAlarms()

    fun observeAlarmsByProfileId(id: UUID): Flow<List<AlarmRelation>?> = alarmRelationDao.observeAlarmsByProfileId(id)

}