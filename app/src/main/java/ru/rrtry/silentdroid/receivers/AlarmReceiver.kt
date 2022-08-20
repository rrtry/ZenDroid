package ru.rrtry.silentdroid.receivers

import android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
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
import ru.rrtry.silentdroid.entities.PreviousAndNextTrigger
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.event.EventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import ru.rrtry.silentdroid.entities.AlarmRelation
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
            ACTION_TIMEZONE_CHANGED, ACTION_TIME_CHANGED, ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                goAsync(context!!, GlobalScope, Dispatchers.IO) {
                    scheduleAlarmInstances(true)
                }
            }
            BOOT_COMPLETED -> {
                goAsync(context!!, GlobalScope, Dispatchers.IO) {
                    scheduleAlarmInstances(false)
                }
            }
        }
    }

    private suspend fun onAlarm(alarm: Alarm, startProfile: Profile, endProfile: Profile) {

        setNextAlarm(
            alarm,
            startProfile,
            endProfile
        )

        val profile: Profile = getProfile(alarm, startProfile, endProfile)
        val previousAndNextTrigger: PreviousAndNextTrigger? = scheduleManager.getPreviousAndNextTrigger(
            alarmRepository.getEnabledAlarms()
        )

        profileManager.setProfile<Alarm?>(
            profile,
            if (previousAndNextTrigger != null) TRIGGER_TYPE_ALARM else TRIGGER_TYPE_MANUAL,
            if (previousAndNextTrigger != null) alarm else null
        )
        notificationHelper.updateNotification(profile, previousAndNextTrigger)
    }

    private fun getProfile(alarm: Alarm, startProfile: Profile, endProfile: Profile): Profile {
        return if (scheduleManager.meetsSchedule() &&
            scheduleManager.isAlarmValid(alarm)) startProfile else endProfile
    }

    private suspend fun scheduleAlarmInstances(enforceScheduledProfile: Boolean) {
        alarmRepository.getEnabledAlarms()?.let { enabledAlarms ->
            enabledAlarms.forEach { relation ->
                setNextAlarm(relation)
            }
            profileManager.updateScheduledProfile(enabledAlarms, enforceScheduledProfile)
        }
    }

    private suspend fun setNextAlarm(relation: AlarmRelation) {
        scheduleManager.scheduleAlarm(
            relation.alarm,
            relation.startProfile,
            relation.endProfile
        ).also { scheduled ->
            if (!scheduled) cancelAlarm(relation.alarm)
        }
    }

    private suspend fun setNextAlarm(
        alarm: Alarm,
        startProfile: Profile,
        endProfile: Profile)
    {
        scheduleManager.scheduleAlarm(
            alarm,
            startProfile,
            endProfile
        ).also { scheduled ->
            if (!scheduled) cancelAlarm(alarm)
        }
    }

    private suspend fun cancelAlarm(alarm: Alarm) {
        scheduleManager.cancelAlarm(alarm)
        alarmRepository.cancelAlarm(alarm)
        eventBus.updateAlarmState(alarm)
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

        private val BOOT_COMPLETED: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ACTION_LOCKED_BOOT_COMPLETED
        } else {
            ACTION_BOOT_COMPLETED
        }

        internal const val EXTRA_ALARM: String = "extra_alarm"
        internal const val EXTRA_START_PROFILE: String = "extra_start_profile"
        internal const val EXTRA_END_PROFILE: String = "extra_end_profile"
    }
}