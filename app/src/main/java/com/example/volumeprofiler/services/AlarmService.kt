package com.example.volumeprofiler.services

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService: Service() {

    private val job: Job = Job()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var profile: Profile
    private lateinit var alarm: Alarm

    @Inject
    lateinit var alarmUtil: AlarmUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var repository: AlarmRepository

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private suspend fun cancelAlarm(alarm: Alarm): Unit {
        alarm.isScheduled = 0
        repository.updateAlarm(alarm)
    }

    private suspend fun updateAlarmDate(alarm: Alarm): Unit {
        alarm.localDateTime = AlarmUtil.getAlarmNextDate(alarm.localDateTime.toLocalTime(), alarm.scheduledDays)
        repository.updateAlarm(alarm)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == Application.ACTION_ALARM_ALERT) {

            alarm = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_ALARM)!!, ParcelableUtil.getParcelableCreator())
            profile = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_PROFILE)!!, ParcelableUtil.getParcelableCreator())

            startForeground(SERVICE_ID, createAlarmAlertNotification(this, profile.title, alarm.localDateTime.toLocalTime()))

            val scheduled: Boolean = alarmUtil.scheduleAlarm(alarm, profile, true)
            scope.launch {
                try {
                    if (!scheduled) {
                        alarmUtil.cancelAlarm(alarm, profile)
                        cancelAlarm(alarm)
                    } else {
                        updateAlarmDate(alarm)
                    }
                } finally {
                    withContext(NonCancellable) {
                        if (profileUtil.grantedRequiredPermissions(profile)) {
                            profileUtil.setProfile(profile)
                            eventBus.updateProfilesFragment(profile.id)
                        } else {
                            notifyAboutDeniedPermissions()
                        }
                    }
                }
            }.invokeOnCompletion {
                stopService()
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun stopService(): Unit {
        if (Build.VERSION_CODES.N <= Build.VERSION.SDK_INT) {
            stopForeground(STOP_FOREGROUND_DETACH)
        }
        else {
            stopForeground(false)
        }
        stopSelf()
    }

    private fun notifyAboutDeniedPermissions(): Unit {
        val missingPermissions: List<String> = profileUtil.getMissingPermissions()
        if (missingPermissions.isNotEmpty()) {
            postNotification(this, createMissingPermissionNotification(this, missingPermissions), ID_PERMISSIONS)
        }
        if (!profileUtil.canModifySystemPreferences()) {
            postNotification(this, createSystemSettingsNotification(this), ID_SYSTEM_SETTINGS)
        }
        if (!profileUtil.isNotificationPolicyAccessGranted()) {
            postNotification(this, createInterruptionPolicyNotification(this), ID_INTERRUPTION_POLICY)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            postNotification(this, createAlarmAlertNotification(this, profile.title, alarm.localDateTime.toLocalTime()), ID_SCHEDULER)
        }
    }

    companion object {

        private const val SERVICE_ID: Int = 165
        const val EXTRA_ALARM: String = "extra_alarm"
        const val EXTRA_PROFILE: String = "extra_profile"
    }
}