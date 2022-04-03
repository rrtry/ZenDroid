package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import com.example.volumeprofiler.Application.Companion.ACTION_ALARM_TRIGGER
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
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
        intent?.let {

            val context: Context = context!!

            when (it.action) {

                ACTION_ALARM_TRIGGER -> {

                    val alarm = ParcelableUtil.toParcelable(
                        intent.getByteArrayExtra(EXTRA_ALARM)!!,
                        ParcelableUtil.getParcelableCreator()) as Alarm

                    val profile = ParcelableUtil.toParcelable(
                        intent.getByteArrayExtra(EXTRA_PROFILE)!!,
                        ParcelableUtil.getParcelableCreator()) as Profile

                    goAsync(context, GlobalScope, Dispatchers.IO) {

                        if (ScheduleManager.isAlarmValid(alarm)) {
                            scheduleManager.scheduleAlarm(alarm, profile)
                            updateAlarmNextInstanceTime(alarm)
                        } else {
                            scheduleManager.cancelAlarm(alarm, profile)
                            cancelAlarm(alarm)
                        }
                        profileManager.setProfile(profile)
                        postNotification(
                            context,
                            createAlarmAlertNotification(context, "Scheduler", LocalTime.now()),
                            ID_SCHEDULER
                        )
                    }
                }
                ACTION_TIMEZONE_CHANGED, ACTION_LOCKED_BOOT_COMPLETED -> {
                    goAsync(context, GlobalScope, Dispatchers.IO) {
                        updateAlarmInstances(context)
                    }
                }
                ACTION_BOOT_COMPLETED -> {
                    if (Build.VERSION_CODES.N > Build.VERSION.SDK_INT) {
                        goAsync(context, GlobalScope, Dispatchers.IO) {
                            updateAlarmInstances(context)
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateAlarmInstances(context: Context): Unit {
        val alarms: List<AlarmRelation>? = alarmRepository.getEnabledAlarms()
        if (!alarms.isNullOrEmpty()) {

            for (i in ScheduleManager.sortInstances(alarms)) {

                val alarm: Alarm = i.alarm
                val profile: Profile = i.profile
                var obsolete = false

                if (ScheduleManager.isAlarmInstanceObsolete(alarm)) {
                    obsolete = true
                    profileManager.setProfile(profile)
                    postNotification(
                        context,
                        createAlarmAlertNotification(context, profile.title, alarm.localStartTime),
                        ID_SCHEDULER
                    )
                }
                if (!obsolete && preferencesManager.isProfileEnabled(profile)) {
                    profileManager.setProfile(profile)
                }
                if (ScheduleManager.isAlarmValid(alarm)) {
                    scheduleManager.scheduleAlarm(alarm, profile)
                    if (obsolete) {
                        updateAlarmNextInstanceTime(alarm)
                    }
                } else {
                    scheduleManager.cancelAlarm(i.alarm, i.profile)
                    cancelAlarm(i.alarm)
                }
            }
        }
    }

    private suspend fun cancelAlarm(alarm: Alarm): Unit {
        alarm.isScheduled = 0
        alarmRepository.updateAlarm(alarm)
    }

    private suspend fun updateAlarmNextInstanceTime(alarm: Alarm): Unit {
        alarm.instanceStartTime = ScheduleManager.getNextAlarmTime(alarm).toInstant()
        alarmRepository.updateAlarm(alarm)
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
        internal const val EXTRA_PROFILE: String = "extra_profile"
    }
}