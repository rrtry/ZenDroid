package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import com.example.volumeprofiler.Application.Companion.ACTION_ALARM
import com.example.volumeprofiler.core.PreferencesManager
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.core.ScheduleManager
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver: BroadcastReceiver() {

    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_ALARM -> {
                goAsync(context!!, GlobalScope, Dispatchers.Default) {
                    onAlarm(
                        context, getAlarm(intent),
                        getProfile(intent, EXTRA_START_PROFILE),
                        getProfile(intent, EXTRA_END_PROFILE)
                    )
                }
            }
            ACTION_TIMEZONE_CHANGED, ACTION_LOCKED_BOOT_COMPLETED, ACTION_TIME_CHANGED -> {
                goAsync(context!!, GlobalScope, Dispatchers.IO) {
                    updateAlarmInstances(context)
                }
            }
            ACTION_BOOT_COMPLETED -> {
                if (Build.VERSION_CODES.N > Build.VERSION.SDK_INT) {
                    goAsync(context!!, GlobalScope, Dispatchers.IO) {
                        updateAlarmInstances(context)
                    }
                }
            }
        }
    }

    private suspend fun onAlarm(context: Context, alarm: Alarm, startProfile: Profile, endProfile: Profile) {
        scheduleManager.scheduleAlarm(alarm, startProfile, endProfile).also { scheduled ->
            if (!scheduled) {
                scheduleManager.cancelAlarm(alarm)
                cancelAlarm(alarm)
            }
        }

        val profile: Profile = if (!scheduleManager.meetsSchedule()) endProfile else startProfile
        val time: LocalTime = if (!scheduleManager.meetsSchedule()) alarm.endTime else alarm.startTime

        profileManager.setProfile(profile)
        postNotification(
            context,
            createAlarmAlertNotification(
                context,
                alarm.title,
                profile.title,
                time
            ), ID_SCHEDULER)
    }

    private suspend fun cancelAlarm(alarm: Alarm) {
        withContext(Dispatchers.IO) {
            alarmRepository.updateAlarm(alarm.apply {
                isScheduled = false
            })
        }
    }

    private suspend fun updateAlarmInstances(context: Context) {
        alarmRepository.getEnabledAlarms()?.let { enabledAlarms ->
            enabledAlarms.forEach {
                scheduleManager.scheduleAlarm(it.alarm, it.startProfile, it.endProfile).also { scheduled ->
                    if (!scheduled) {
                        scheduleManager.cancelAlarm(it.alarm)
                        cancelAlarm(it.alarm)
                    }
                }
            }
            scheduleManager.getRecentAlarm(enabledAlarms)?.let {

                profileManager.setProfile(it.profile)

                postNotification(
                    context,
                    createAlarmAlertNotification(
                        context,
                        it.alarm.title,
                        it.profile.title,
                        it.time.toLocalTime()
                    ), ID_SCHEDULER)
            }
        }
    }

    companion object {

        private fun getAlarm(intent: Intent): Alarm {
            /*
            return ParcelableUtil.toParcelable(
                intent.getByteArrayExtra(EXTRA_ALARM)!!,
                ParcelableUtil.getParcelableCreator()) as Alarm */
            return intent.getParcelableExtra<Alarm>(EXTRA_ALARM) ?: throw IllegalStateException("Alarm cannot be null")
        }

        private fun getProfile(intent: Intent, name: String): Profile {
            /*
            return ParcelableUtil.toParcelable(
                intent.getByteArrayExtra(name)!!,
                ParcelableUtil.getParcelableCreator()) as Profile */
            return intent.getParcelableExtra<Profile>(name) ?: throw IllegalStateException("Profile cannot be null")
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