package ru.rrtry.silentdroid.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import ru.rrtry.silentdroid.Application.Companion.ACTION_ALARM
import ru.rrtry.silentdroid.core.NotificationHelper
import ru.rrtry.silentdroid.core.PreferencesManager
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_ALARM
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_MANUAL
import ru.rrtry.silentdroid.core.ProfileManager
import ru.rrtry.silentdroid.core.ScheduleManager
import ru.rrtry.silentdroid.db.repositories.AlarmRepository
import ru.rrtry.silentdroid.entities.Alarm
import ru.rrtry.silentdroid.entities.CurrentAlarmInstance
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.eventBus.EventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import ru.rrtry.silentdroid.util.ParcelableUtil.Companion.getExtra
import ru.rrtry.silentdroid.util.WakeLock
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver: BroadcastReceiver() {

    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var notificationHelper: NotificationHelper
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
                    scheduleAlarmInstances()
                }
            }
            ACTION_BOOT_COMPLETED -> {
                if (Build.VERSION_CODES.N > Build.VERSION.SDK_INT) {
                    goAsync(context!!, GlobalScope, Dispatchers.IO) {
                        scheduleAlarmInstances()
                    }
                }
            }
        }
    }

    private suspend fun onAlarm(alarm: Alarm, startProfile: Profile, endProfile: Profile) {

        scheduleManager.scheduleAlarm(alarm, startProfile, endProfile).also { scheduled ->
            if (!scheduled) {
                scheduleManager.cancelAlarm(alarm)
                alarmRepository.cancelAlarm(alarm)
                eventBus.updateAlarmState(alarm)
            }
        }

        val profile: Profile = getProfile(alarm, startProfile, endProfile)
        val currentAlarmInstance: CurrentAlarmInstance? = scheduleManager.getCurrentAlarmInstance(alarmRepository.getEnabledAlarms())

        profileManager.setProfile<Alarm?>(
            profile,
            if (currentAlarmInstance != null) TRIGGER_TYPE_ALARM else TRIGGER_TYPE_MANUAL,
            if (currentAlarmInstance != null) alarm else null
        )
        notificationHelper.updateNotification(profile, currentAlarmInstance)
    }

    private fun getProfile(alarm: Alarm, startProfile: Profile, endProfile: Profile): Profile {
        return if (scheduleManager.meetsSchedule() && scheduleManager.isAlarmValid(alarm)) startProfile else endProfile
    }

    private suspend fun scheduleAlarmInstances() {
        alarmRepository.getEnabledAlarms()?.let { enabledAlarms ->
            enabledAlarms.forEach {
                scheduleManager.scheduleAlarm(it.alarm, it.startProfile, it.endProfile).also { scheduled ->
                    if (!scheduled) {
                        scheduleManager.cancelAlarm(it.alarm)
                        alarmRepository.cancelAlarm(it.alarm)
                    }
                }
            }
            profileManager.updateScheduledProfile(enabledAlarms)
        }
    }

    companion object {

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