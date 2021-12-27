package com.example.volumeprofiler.services

import android.app.Service
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.IBinder
import android.provider.CalendarContract
import android.util.Log
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.Event
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import com.example.volumeprofiler.Application.Companion.ACTION_ALARM_TRIGGER
import com.example.volumeprofiler.Application.Companion.ACTION_CALENDAR_EVENT_TRIGGER
import com.example.volumeprofiler.database.repositories.EventRepository
import java.time.LocalDateTime

@AndroidEntryPoint
class AlarmService: Service(), EventInstanceQueryHandler.AsyncQueryCallback {

    private data class QueryCookie(
        var event: Event,
        var profile: Profile,
        var state: Event.State,
    )

    private val job: Job = Job()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var profile: Profile
    private lateinit var alarm: Alarm

    @Inject
    lateinit var alarmUtil: AlarmUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    @Inject
    lateinit var contentUtil: ContentUtil

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var eventRepository: EventRepository

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private suspend fun updateAlarmCancelledState(alarm: Alarm): Unit {
        alarm.isScheduled = 0
        alarmRepository.updateAlarm(alarm)
    }

    private suspend fun updateAlarmNextInstanceDate(alarm: Alarm): Unit {
        alarm.instanceStartTime = AlarmUtil.getNextAlarmTime(alarm)
        alarmRepository.updateAlarm(alarm)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {

            ACTION_ALARM_TRIGGER -> {

                alarm = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_ALARM)!!, ParcelableUtil.getParcelableCreator())
                profile = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_PROFILE)!!, ParcelableUtil.getParcelableCreator())

                startForeground(SERVICE_ID, createAlarmAlertNotification(this, profile.title, alarm.instanceStartTime.toLocalTime()))

                val reschedule: Boolean = alarm.scheduledDays.isNotEmpty()
                scope.launch {
                    try {
                        if (!reschedule) {
                            alarmUtil.cancelAlarm(alarm, profile)
                            updateAlarmCancelledState(alarm)
                        } else {
                            alarmUtil.scheduleAlarm(alarm, profile, false, false)
                            updateAlarmNextInstanceDate(alarm)
                        }
                    } finally {
                        withContext(NonCancellable) {
                            if (profileUtil.grantedRequiredPermissions(profile)) {
                                profileUtil.setProfile(profile)
                                eventBus.onProfileSet(profile.id)
                            } else {
                                sendPermissionDeniedNotifications()
                            }
                        }
                    }
                }.invokeOnCompletion {
                    if (it != null) {
                        Log.e("AlarmService", "failed to update alarm", it)
                    }
                    stopService()
                }
            }
            ACTION_CALENDAR_EVENT_TRIGGER -> {

                val event: Event = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_EVENT)!!, ParcelableUtil.getParcelableCreator())
                val profile: Profile = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_PROFILE)!!, ParcelableUtil.getParcelableCreator())
                val state: Event.State = intent.getSerializableExtra(EXTRA_EVENT_STATE) as Event.State

                startForeground(SERVICE_ID, createCalendarEventNotification(this, event, profile, state))

                contentUtil.queryEventInstances(event.id, TOKEN_QUERY_EVENT_INSTANCES, QueryCookie(
                    event, profile, state
                ),this)
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

    private fun sendPermissionDeniedNotifications(): Unit {
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
            postNotification(this, createAlarmAlertNotification(this, profile.title, alarm.instanceStartTime.toLocalTime()), ID_SCHEDULER)
        }
    }

    override fun onQueryComplete(cursor: Cursor?, cookie: Any?) {

        val queryCookie: QueryCookie = cookie!! as QueryCookie
        val state: Event.State = queryCookie.state
        val event: Event = queryCookie.event
        val profile: Profile = queryCookie.profile

        when (state) {
            Event.State.STARTED -> {
                alarmUtil.scheduleAlarm(event, profile, Event.State.COMPLETED)
                stopService()
            }
            Event.State.COMPLETED -> {
                var schedule: Boolean = false
                if (event.endTime > 0 && event.endTime > AlarmUtil.toEpochMilli(LocalDateTime.now())) {
                    schedule = true
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val begin: Long = it.getLong(it.getColumnIndex(CalendarContract.Instances.BEGIN))
                            val end: Long = it.getLong(it.getColumnIndex(CalendarContract.Instances.END))
                            event.currentInstanceStartTime = begin
                            event.currentInstanceEndTime = end
                        }
                    }
                }
                scope.launch {
                    if (schedule) {
                        eventRepository.updateEvent(event)
                        alarmUtil.scheduleAlarm(event, profile, Event.State.STARTED)
                    } else {
                        Log.i("AlarmService", "cancelling event with an id of ${event.id}")
                    }
                }.invokeOnCompletion {
                    stopService()
                }
            }
        }
    }

    companion object {

        private const val SERVICE_ID: Int = 165
        private const val TOKEN_QUERY_EVENT_INSTANCES: Int = 2

        internal const val EXTRA_EVENT: String = "extra_event"
        internal const val EXTRA_EVENT_STATE: String = "extra_event_state"
        internal const val EXTRA_ALARM: String = "extra_alarm"
        internal const val EXTRA_PROFILE: String = "extra_profile"
    }
}