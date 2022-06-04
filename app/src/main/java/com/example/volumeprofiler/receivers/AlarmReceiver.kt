package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import android.os.Parcelable
import com.example.volumeprofiler.Application.Companion.ACTION_ALARM
import com.example.volumeprofiler.core.NotificationDelegate
import com.example.volumeprofiler.core.PreferencesManager
import com.example.volumeprofiler.core.PreferencesManager.Companion.TRIGGER_TYPE_ALARM
import com.example.volumeprofiler.core.PreferencesManager.Companion.TRIGGER_TYPE_MANUAL
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.core.ScheduleManager
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.OngoingAlarm
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver: BroadcastReceiver() {

    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var notificationDelegate: NotificationDelegate
    @Inject lateinit var eventBus: EventBus

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_ALARM -> {
                goAsync(context!!, GlobalScope, Dispatchers.Default) {
                    onAlarm(
                        getExtra(intent, EXTRA_ALARM),
                        getExtra(intent, EXTRA_START_PROFILE),
                        getExtra(intent, EXTRA_END_PROFILE)
                    )
                }
            }
            ACTION_TIMEZONE_CHANGED, ACTION_LOCKED_BOOT_COMPLETED, ACTION_TIME_CHANGED -> {
                goAsync(context!!, GlobalScope, Dispatchers.IO) {
                    updateAlarmInstances()
                }
            }
            ACTION_BOOT_COMPLETED -> {
                if (Build.VERSION_CODES.N > Build.VERSION.SDK_INT) {
                    goAsync(context!!, GlobalScope, Dispatchers.IO) {
                        updateAlarmInstances()
                    }
                }
            }
        }
    }

    private suspend fun onAlarm(alarm: Alarm, startProfile: Profile, endProfile: Profile) {
        scheduleManager.scheduleAlarm(alarm, startProfile, endProfile).also { scheduled ->
            if (!scheduled) {
                scheduleManager.cancelAlarm(alarm)
                cancelAlarm(alarm)
                eventBus.updateAlarmState(alarm)
            }
        }

        val profile: Profile = getProfile(alarm, startProfile, endProfile)
        val ongoingAlarm: OngoingAlarm? = getOngoingAlarm()

        profileManager.setProfile<Alarm?>(
            profile,
            if (ongoingAlarm != null) TRIGGER_TYPE_ALARM else TRIGGER_TYPE_MANUAL,
            if (ongoingAlarm != null) alarm else null
        )
        notificationDelegate.updateNotification(profile, ongoingAlarm)
    }

    private fun getProfile(alarm: Alarm, startProfile: Profile, endProfile: Profile): Profile {
        return if (scheduleManager.meetsSchedule() && scheduleManager.isAlarmValid(alarm)) {
            startProfile
        } else {
            endProfile
        }
    }

    private suspend fun cancelAlarm(alarm: Alarm) {
        alarmRepository.updateAlarm(alarm.apply {
            isScheduled = false
        })
    }

    private suspend fun getOngoingAlarm(): OngoingAlarm? {
        alarmRepository.getEnabledAlarms()?.let { enabledAlarms ->
            return scheduleManager.getOngoingAlarm(enabledAlarms)
        }
        return null
    }

    private suspend fun updateAlarmInstances() {
        alarmRepository.getEnabledAlarms()?.let { enabledAlarms ->
            enabledAlarms.forEach {
                scheduleManager.scheduleAlarm(it.alarm, it.startProfile, it.endProfile).also { scheduled ->
                    if (!scheduled) {
                        scheduleManager.cancelAlarm(it.alarm)
                        cancelAlarm(it.alarm)
                    }
                }
            }
            profileManager.updateScheduledProfile(enabledAlarms)
        }
    }

    companion object {

        private fun <T: Parcelable> getExtra(intent: Intent, name: String): T {
            return intent.getParcelableExtra(name)
                ?: throw IllegalStateException("$name cannot be null")
        }

        fun BroadcastReceiver.goAsync(
            context: Context,
            coroutineScope: CoroutineScope,
            dispatcher: CoroutineDispatcher,
            block: suspend () -> Unit
        ) {
            WakeLock.acquire(context)
            val pendingResult: PendingResult = goAsync()
            coroutineScope.launch(dispatcher) {
                block()
                WakeLock.release()
                pendingResult.finish()
            }
        }

        internal const val EXTRA_ALARM: String = "extra_alarm"
        internal const val EXTRA_START_PROFILE: String = "extra_start_profile"
        internal const val EXTRA_END_PROFILE: String = "extra_end_profile"
    }
}