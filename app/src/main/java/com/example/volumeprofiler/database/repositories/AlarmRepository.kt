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
            alarmRelationDao.getActiveAlarmTriggers()
        }
    }

    suspend fun getScheduledAlarmsByProfileId(id: UUID): List<AlarmRelation>? {
        return withContext(Dispatchers.IO) {
            alarmRelationDao.getActiveAlarmTriggersByProfileId(id)
        }
    }

    fun observeAlarmTriggers(): Flow<List<AlarmRelation>> = alarmRelationDao.observeAlarmTriggers()

    fun observeAlarmTriggersByProfileId(id: UUID): Flow<List<AlarmRelation>?> = alarmRelationDao.observeAlarmTriggersByProfileId(id)

}