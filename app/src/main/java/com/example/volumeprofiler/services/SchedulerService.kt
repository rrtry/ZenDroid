package com.example.volumeprofiler.services

import android.app.*
import android.content.Intent
import android.database.Cursor
import android.os.IBinder
import android.provider.CalendarContract
import android.util.Log
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.database.repositories.EventRepository
import com.example.volumeprofiler.entities.*
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import java.time.LocalDateTime

@AndroidEntryPoint
class SchedulerService: Service(), EventInstanceQueryHandler.AsyncQueryCallback {

    private val job: Job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    @Inject
    lateinit var repository: AlarmRepository

    @Inject
    lateinit var eventRepository: EventRepository

    @Inject
    lateinit var contentUtil: ContentUtil

    @Inject
    lateinit var alarmUtil: AlarmUtil

    @Inject
    lateinit var sharedPreferencesUtil: SharedPreferencesUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    @Inject
    lateinit var eventBus: EventBus

    private suspend fun updateAlarmInstanceTime(alarm: Alarm): Unit {
        alarm.instanceStartTime = AlarmUtil.getNextAlarmTime(alarm)
        repository.updateAlarm(alarm)
    }

    private suspend fun cancelAlarm(alarm: Alarm): Unit {
        alarm.isScheduled = 0
        repository.updateAlarm(alarm)
    }

    override fun onQueryComplete(cursor: Cursor?, cookie: Any?) {
        val eventRelation: EventRelation = cookie as EventRelation
        val event: Event = eventRelation.event
        cursor?.use {
            if (it.moveToFirst()) {
                event.currentInstanceStartTime = it.getLong(it.getColumnIndex(CalendarContract.Instances.BEGIN))
                event.currentInstanceEndTime = it.getLong(it.getColumnIndex(CalendarContract.Instances.END))
            }
        }
        scope.launch {
            eventRepository.updateEvent(event)
        }
        if (event.currentInstanceStartTime > AlarmUtil.toEpochMilli(LocalDateTime.now())) {
            alarmUtil.scheduleAlarm(event, eventRelation.eventEndsProfile!!, Event.State.COMPLETED)
        } else {
            alarmUtil.scheduleAlarm(event, eventRelation.eventStartsProfile, Event.State.STARTED)
        }
    }

    private suspend fun scheduleEvents(): Unit {
        val events: List<EventRelation> = eventRepository.getEventsByState(true)
        if (events.isNotEmpty()) {

            val now: LocalDateTime = LocalDateTime.now()
            var lastMissedEvent: Pair<Event.State, EventRelation>? = null

            for (i in events) {

                val millis: Long = AlarmUtil.toEpochMilli(now)
                val event: Event = i.event

                if (event.currentInstanceStartTime < millis) {
                    lastMissedEvent = Pair(Event.State.STARTED, i)
                    if (event.currentInstanceEndTime < millis) {
                        lastMissedEvent = Pair(Event.State.COMPLETED, i)
                    }
                }
                if (event.endTime != 0L && event.endTime < millis) {
                    event.scheduled = false
                    eventRepository.updateEvent(event)
                } else {
                    contentUtil.queryEventInstances(event.id, 2, i, this)
                }
                if (lastMissedEvent != null) {

                    val state: Event.State = lastMissedEvent.first
                    val eventRelation: EventRelation = lastMissedEvent.second
                    when (state) {
                        Event.State.STARTED -> {
                            profileUtil.setProfile(eventRelation.eventStartsProfile)
                            eventBus.onProfileSet(eventRelation.eventStartsProfile.id)
                            postNotification(
                                this,
                                createCalendarEventNotification(this, eventRelation.event, eventRelation.eventStartsProfile, state),
                                ID_CALENDAR_EVENT)
                        }
                        Event.State.COMPLETED -> {
                            profileUtil.setProfile(eventRelation.eventEndsProfile!!)
                            eventBus.onProfileSet(eventRelation.eventEndsProfile!!.id)
                            postNotification(
                                this,
                                createCalendarEventNotification(this, eventRelation.event, eventRelation.eventEndsProfile!!, state),
                                ID_CALENDAR_EVENT)
                        }
                    }
                }
            }
        }
    }

    private suspend fun getAlarmInstances(): Unit {
        val alarms: List<AlarmRelation>? = repository.getEnabledAlarms()
        if (!alarms.isNullOrEmpty()) {

            val now: LocalDateTime = LocalDateTime.now()
            var lastMissedAlarm: AlarmRelation? = null

            for (i in AlarmUtil.sortAlarms(alarms)) {
                val alarm: Alarm = i.alarm
                val profile: Profile = i.profile

                Log.i("SchedulerService", "current instance start time: ${alarm.instanceStartTime}")
                Log.i("SchedulerService", "now: $now")

                if (alarm.instanceStartTime < now) {
                    Log.i("SchedulerService", "missed alarm: $profile")
                    lastMissedAlarm = i
                }
                if (lastMissedAlarm == null && sharedPreferencesUtil.isProfileEnabled(profile)) {
                    Log.i("SchedulerService", "restoring previous profile")
                }

                val schedule: Boolean = (alarm.scheduledDays.isEmpty() && lastMissedAlarm?.alarm?.id != alarm.id) || alarm.scheduledDays.isNotEmpty()
                if (!schedule) {
                    Log.i("SchedulerService", "cancelling alarm $profile")
                } else {
                    Log.i("SchedulerService", "scheduling alarm $profile")
                    if (lastMissedAlarm?.alarm?.id == alarm.id) {
                        Log.i("SchedulerService", "updating instance of $profile")
                    }
                }
            }
        }
    }

    private suspend fun scheduleAlarms(): Unit {
        val alarms: List<AlarmRelation>? = repository.getEnabledAlarms()
        if (!alarms.isNullOrEmpty()) {

            val now: LocalDateTime = LocalDateTime.now()
            var lastMissedAlarm: AlarmRelation? = null

            for (i in AlarmUtil.sortAlarms(alarms)) {

                val alarm: Alarm = i.alarm
                val profile: Profile = i.profile

                if (alarm.instanceStartTime < now) {
                    Log.i("SchedulerService", "missed alarm: $profile")
                    lastMissedAlarm = i
                }
                if (lastMissedAlarm == null && sharedPreferencesUtil.isProfileEnabled(profile)) {
                    eventBus.onProfileSet(profile.id)
                    profileUtil.setProfile(profile)
                }

                val schedule: Boolean = (alarm.scheduledDays.isEmpty() && lastMissedAlarm?.alarm?.id != alarm.id) || alarm.scheduledDays.isNotEmpty()
                if (!schedule) {
                    Log.i("SchedulerService", "cancelling alarm $profile")
                    alarmUtil.cancelAlarm(i.alarm, i.profile)
                    cancelAlarm(i.alarm)
                } else {
                    Log.i("SchedulerService", "scheduling alarm $profile")
                    alarmUtil.scheduleAlarm(alarm, profile, true)
                    if (lastMissedAlarm?.alarm?.id == alarm.id) {
                        Log.i("SchedulerService", "updating instance of $profile")
                        updateAlarmInstanceTime(alarm)
                    }
                }
            }
            if (lastMissedAlarm != null) {
                val profile: Profile = lastMissedAlarm.profile
                val alarm: Alarm = lastMissedAlarm.alarm
                profileUtil.setProfile(profile)
                eventBus.onProfileSet(profile.id)
                postNotification(
                    this,
                    createAlarmAlertNotification(this, profile.title, alarm.instanceStartTime.toLocalTime()),
                    ID_SCHEDULER)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(SERVICE_ID, createSchedulerNotification(this))

        scope.launch {
            //scheduleEvents()
            //scheduleAlarms()
            getAlarmInstances()
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