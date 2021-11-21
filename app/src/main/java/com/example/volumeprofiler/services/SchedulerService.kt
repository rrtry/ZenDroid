package com.example.volumeprofiler.services

import android.app.*
import android.content.Intent
import android.os.IBinder
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import java.time.LocalDateTime

@AndroidEntryPoint
class SchedulerService: Service() {

    private val job: Job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    @Inject
    lateinit var repository: AlarmRepository

    @Inject
    lateinit var alarmUtil: AlarmUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    private suspend fun cancelAlarm(alarm: Alarm): Unit {
        alarm.isScheduled = 0
        repository.updateAlarm(alarm)
    }

    private suspend fun scheduleAlarms(): Unit {
        val alarms: List<AlarmRelation>? = repository.getEnabledAlarms()
        val now: LocalDateTime = LocalDateTime.now()
        if (!alarms.isNullOrEmpty()) {

            for (i in AlarmUtil.sortAlarms(alarms)) {

                val alarm: Alarm = i.alarm
                val profile: Profile = i.profile

                //Check if alarm has already fired off while device was powered off
                if (alarm.localDateTime < now) {
                    profileUtil.setProfile(profile)
                    postNotification(this, createAlarmAlertNotification(this, profile.title, alarm.localDateTime.toLocalTime()), ID_SCHEDULER)
                }

                val scheduled: Boolean = alarmUtil.scheduleAlarm(alarm, profile, true)
                if (!scheduled) {
                    alarmUtil.cancelAlarm(i.alarm, i.profile)
                    cancelAlarm(i.alarm)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(SERVICE_ID, createSchedulerNotification(this))

        scope.launch {
            scheduleAlarms()
        }.invokeOnCompletion {
            stopService()
        }
        return START_STICKY
    }

    private fun stopService(): Unit {
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {

        private const val SERVICE_ID: Int = 162
    }
}