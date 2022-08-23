package ru.rrtry.silentdroid.db.repositories

import ru.rrtry.silentdroid.db.dao.AlarmDao
import ru.rrtry.silentdroid.db.dao.AlarmRelationDao
import ru.rrtry.silentdroid.entities.Alarm
import ru.rrtry.silentdroid.entities.AlarmRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao,
    private val alarmRelationDao: AlarmRelationDao
) {

    suspend fun addAlarm(alarm: Alarm): Int {
        return withContext(Dispatchers.IO) {
            alarmDao.addAlarm(alarm).toInt()
        }
    }

    suspend fun updateAlarm(alarm: Alarm) {
        withContext(Dispatchers.IO) {
            alarmDao.updateAlarm(alarm)
        }
    }

    suspend fun cancelAlarm(alarm: Alarm) {
        withContext(Dispatchers.IO) {
            alarmDao.updateAlarm(alarm.apply {
                isScheduled = false
            })
        }
    }

    suspend fun scheduleAlarm(alarm: Alarm) {
        withContext(Dispatchers.IO) {
            alarmDao.updateAlarm(alarm.apply {
                isScheduled = true
            })
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

    fun observeAlarms(): Flow<List<AlarmRelation>> {
        return alarmRelationDao.observeAlarms()
    }

    fun observeScheduledAlarmsByProfileId(id: UUID): Flow<List<AlarmRelation>?> {
        return alarmRelationDao.observeScheduledAlarmsByProfileId(id)
    }

}