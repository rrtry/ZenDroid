package com.example.volumeprofiler.services

import android.app.*
import android.content.Intent
import android.database.Cursor
import android.os.IBinder
import android.provider.CalendarContract
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.database.repositories.EventRepository
import com.example.volumeprofiler.entities.*
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class SchedulerService: Service(), ContentQueryHandler.AsyncQueryCallback {

    private val job: Job = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + job)

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
        alarm.instanceStartTime = AlarmUtil.getNextAlarmTime(alarm).toInstant()
        repository.updateAlarm(alarm)
    }

    private suspend fun cancelAlarm(alarm: Alarm): Unit {
        alarm.isScheduled = 0
        repository.updateAlarm(alarm)
    }

    override fun onQueryComplete(cursor: Cursor?, cookie: Any?, token: Int) {
        val eventRelation: EventRelation = cookie as EventRelation
        val event: Event = eventRelation.event
        when (token) {
            QUERY_NEXT_INSTANCES_TOKEN -> {
                cursor?.use {
                    if (it.moveToFirst()) {
                        event.instanceBeginTime = it.getLong(it.getColumnIndex(CalendarContract.Instances.BEGIN))
                        event.instanceEndTime = it.getLong(it.getColumnIndex(CalendarContract.Instances.END))
                    }
                }
                serviceScope.launch {
                    eventRepository.updateEvent(event)
                }.ensureActive()

                if (event.isInstanceObsolete(event.instanceBeginTime)) {
                    alarmUtil.scheduleAlarm(event, eventRelation.eventEndsProfile, Event.State.END)
                } else {
                    alarmUtil.scheduleAlarm(event, eventRelation.eventStartsProfile, Event.State.START)
                }
            }
            QUERY_PREVIOUS_INSTANCES_TOKEN -> {
                cursor?.use {
                    while (it.moveToNext()) {
                        val begin: Long = it.getLong(it.getColumnIndex(CalendarContract.Instances.BEGIN))
                        val end: Long = it.getLong(it.getColumnIndex(CalendarContract.Instances.END))
                        val timezone: String = it.getString(it.getColumnIndex(CalendarContract.Instances.EVENT_TIMEZONE))

                        val now: LocalDateTime = LocalDateTime.now()
                        val zoneId: ZoneId = ZoneId.of(timezone)
                        val adjustedBegin: LocalDateTime = Instant.ofEpochMilli(begin).atZone(zoneId).toLocalDateTime()
                        val adjustedEnd: LocalDateTime = Instant.ofEpochMilli(end).atZone(zoneId).toLocalDateTime()

                        if (adjustedBegin < now) {
                            profileUtil.setProfile(eventRelation.eventStartsProfile)
                            eventBus.onProfileSet(eventRelation.eventStartsProfile.id)
                            postNotification(this, createCalendarEventNotification(
                                this, event, eventRelation.eventStartsProfile, Event.State.START
                            ), ID_CALENDAR_EVENT)
                        }
                        if (adjustedEnd < now) {
                            profileUtil.setProfile(eventRelation.eventEndsProfile)
                            eventBus.onProfileSet(eventRelation.eventEndsProfile.id)
                            postNotification(this, createCalendarEventNotification(
                                this, event, eventRelation.eventEndsProfile, Event.State.END
                            ), ID_CALENDAR_EVENT)
                        }
                    }
                }
            }
        }
    }

    private suspend fun scheduleEvents(): Unit {
        val events: List<EventRelation> = eventRepository.getEventsByState(true)
        if (events.isNotEmpty()) {

            for (i in Event.sortEvents(events)) {
                val event: Event = i.event
                if (event.isInstanceObsolete(event.instanceBeginTime)) {
                    contentUtil.queryMissedEventInstances(event.id, event.instanceBeginTime, QUERY_PREVIOUS_INSTANCES_TOKEN, i, this)
                }
                if (event.isObsolete()) {
                    eventRepository.deleteEvent(event)
                    alarmUtil.cancelAlarm(event)
                } else {
                    contentUtil.queryEventNextInstances(event.id, QUERY_NEXT_INSTANCES_TOKEN, i, this)
                }
            }
        }
    }

    private suspend fun scheduleAlarms(): Unit {
        val alarms: List<AlarmRelation>? = repository.getEnabledAlarms()
        if (!alarms.isNullOrEmpty()) {

            for (i in AlarmUtil.sortInstances(alarms)) {

                val alarm: Alarm = i.alarm
                val profile: Profile = i.profile
                var obsolete: Boolean = false

                if (AlarmUtil.isAlarmInstanceObsolete(alarm)) {
                    obsolete = true
                    profileUtil.setProfile(profile)
                    eventBus.onProfileSet(profile.id)
                    postNotification(
                        this,
                        createAlarmAlertNotification(this, profile.title, alarm.localStartTime),
                        ID_SCHEDULER)
                }
                if (!obsolete && sharedPreferencesUtil.isProfileEnabled(profile)) {
                    eventBus.onProfileSet(profile.id)
                    profileUtil.setProfile(profile)
                }
                if (AlarmUtil.isAlarmValid(alarm)) {
                    alarmUtil.scheduleAlarm(alarm, profile, false)
                    if (obsolete) {
                        updateAlarmInstanceTime(alarm)
                    }
                } else {
                    alarmUtil.cancelAlarm(i.alarm, i.profile)
                    cancelAlarm(i.alarm)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        WakeLock.acquire(this)
        startForeground(SERVICE_ID, createSchedulerNotification(this))

        serviceScope.launch {
            scheduleEvents()
            scheduleAlarms()
        }.invokeOnCompletion {
            stopService()
        }
        return START_STICKY
    }

    private fun stopService(): Unit {
        WakeLock.release()
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

        private const val QUERY_PREVIOUS_INSTANCES_TOKEN: Int = 4
        private const val QUERY_NEXT_INSTANCES_TOKEN: Int = 3
        private const val SERVICE_ID: Int = 162
    }
}