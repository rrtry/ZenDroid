package com.example.volumeprofiler.database.repositories

import com.example.volumeprofiler.database.dao.AlarmDao
import com.example.volumeprofiler.database.dao.AlarmTriggerDao
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.AlarmTrigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
        private val alarmDao: AlarmDao,
        private val alarmTriggerDao: AlarmTriggerDao) {

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

    suspend fun getActiveAlarmTriggers(): List<AlarmTrigger>? {
        return withContext(Dispatchers.IO) {
            alarmTriggerDao.getActiveAlarmTriggers()
        }
    }

    suspend fun getActiveAlarmTriggersByProfileId(id: UUID): List<AlarmTrigger>? {
        return withContext(Dispatchers.IO) {
            alarmTriggerDao.getActiveAlarmTriggersByProfileId(id)
        }
    }

    fun observeAlarmTriggers(): Flow<List<AlarmTrigger>> = alarmTriggerDao.observeAlarmTriggers()

    fun observeAlarmTriggersByProfileId(id: UUID): Flow<List<AlarmTrigger>?> = alarmTriggerDao.observeAlarmTriggersByProfileId(id)

}