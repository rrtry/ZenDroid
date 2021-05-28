 package com.example.volumeprofiler.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import android.util.Log
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.receivers.AlarmReceiver
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.Serializable
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

 /*
   *  Utility class which has useful methods for settings alarms, dealing with date objects and schedules
  */

class AlarmUtil constructor (val context: Context) {

    fun setAlarm(
            volumeSettingsMapPair: Pair<Map<Int, Int>, Map<String, Int>>,
            eventOccurrences: Array<Int>,
            eventTime: LocalDateTime,
            id: Long, onReschedule: Boolean = false, profileId: UUID): Unit {
        val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent: Intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = AlarmReceiver.ACTION_TRIGGER_ALARM
            this.putExtra(AlarmReceiver.EXTRA_PRIMARY_VOLUME_SETTINGS, volumeSettingsMapPair.first as Serializable)
            this.putExtra(AlarmReceiver.EXTRA_OPTIONAL_VOLUME_SETTINGS, volumeSettingsMapPair.second as Serializable)
            this.putExtra(AlarmReceiver.EXTRA_EVENT_OCCURRENCES, eventOccurrences)
            this.putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
            this.putExtra(AlarmReceiver.EXTRA_PROFILE_ID, profileId)
            this.putExtra(AlarmReceiver.EXTRA_ALARM_TRIGGER_TIME, eventTime)
        }
        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_CANCEL_CURRENT)
        val now: LocalDateTime = LocalDateTime.now()
        var delay: Long
        if ((eventOccurrences.contains(now.dayOfWeek.value) || eventOccurrences.isEmpty()) && now.toLocalTime() < eventTime.toLocalTime()) {
            delay = diffBetweenHoursInMillis(eventTime.toLocalTime())
        }
        else {
            var nextDay: DayOfWeek? = getNextDayOnSchedule(eventOccurrences)
            if (nextDay != null) {
                delay = diffBetweenDatesInMillis(nextDay, eventTime.hour, eventTime.minute)
                Log.i("AlarmHelper", "scheduled for the next day, delay: $delay, alarmId: $id, day: $nextDay")
            }
            else {
                if (!onReschedule) {
                    nextDay = now.dayOfWeek.plus(1)
                    delay = diffBetweenDatesInMillis(nextDay, eventTime.hour, eventTime.minute)
                    Log.i("AlarmHelper", "no days on schedule, settings alarm for tomorrow: $delay, alarmId: $id, day: $nextDay")
                }
                else {
                    cancelAlarm(volumeSettingsMapPair, eventOccurrences, eventTime, id, profileId)
                    GlobalScope.launch {
                        val repository: Repository = Repository.get()
                        val event: Event = repository.getEvent(id)
                        event.isScheduled = 0
                        repository.updateEvent(event)
                    }
                    return
                }
            }
        }
        val currentTimeInMillis: Long = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, currentTimeInMillis + delay, pendingIntent)
        }
        else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, currentTimeInMillis + delay, pendingIntent)
        }
    }

    fun cancelAlarm(
            volumeSettingsMapPair: Pair<Map<Int, Int>, Map<String, Int>>,
            eventOccurrences: Array<Int>,
            eventTime: LocalDateTime,
            id: Long,
            profileId: UUID): Unit {
        Log.i("AlarmHelper", "request to cancel alarm with an id of $id")
        val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent: Intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = AlarmReceiver.ACTION_TRIGGER_ALARM
            this.putExtra(AlarmReceiver.EXTRA_PRIMARY_VOLUME_SETTINGS, volumeSettingsMapPair.first as Serializable)
            this.putExtra(AlarmReceiver.EXTRA_OPTIONAL_VOLUME_SETTINGS, volumeSettingsMapPair.second as Serializable)
            this.putExtra(AlarmReceiver.EXTRA_EVENT_OCCURRENCES, eventOccurrences)
            this.putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
            this.putExtra(AlarmReceiver.EXTRA_PROFILE_ID, profileId)
            this.putExtra(AlarmReceiver.EXTRA_ALARM_TRIGGER_TIME, eventTime)
        }
        val pendingIntent: PendingIntent? = PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            Log.i("AlarmHelper", "pendingIntent exists, cancelling alarm...")
            alarmManager.cancel(pendingIntent)
        }
        else {
            Log.i("AlarmHelper", "failed to cancel alarm with an id of $id")
        }
    }

    fun cancelMultipleAlarms(list: List<ProfileAndEvent>): Unit {
        for (i in list) {
            val event: Event = i.event
            val profile: Profile = i.profile
            val eventOccurrences: Array<Int>
            if (event.workingDays.isNotEmpty()) {
                eventOccurrences = event.workingDays.split("").slice(1..event.workingDays.length).map { it.toInt() }.toTypedArray()
            }
            else {
                eventOccurrences = arrayOf()
            }
            val volumeSettingsMap = AudioUtil.getVolumeSettingsMapPair(profile)
            cancelAlarm(
                    volumeSettingsMap, eventOccurrences,
                    event.localDateTime, event.eventId, profile.id)
        }
    }

    fun setMultipleAlarms(list: List<ProfileAndEvent>): Unit {
        for (i in list) {
            val event: Event = i.event
            val profile: Profile = i.profile
            val eventOccurrences: Array<Int>
            if (event.workingDays.isNotEmpty()) {
                eventOccurrences = event.workingDays.split("").slice(1..event.workingDays.length).map { it.toInt() }.toTypedArray()
            }
            else {
                eventOccurrences = arrayOf()
            }
            val volumeSettingsMap = AudioUtil.getVolumeSettingsMapPair(profile)
            setAlarm(
                    volumeSettingsMap, eventOccurrences,
                    event.localDateTime, event.eventId, false, profile.id)
        }
    }

    companion object {

        private fun diffBetweenDatesInMillis(nextDay: DayOfWeek, hour: Int, minute: Int): Long {
            val now: LocalDateTime = LocalDateTime.now()
            val nextDate: LocalDateTime = now.with(TemporalAdjusters.next(nextDay)).withHour(hour).withMinute(minute).withSecond(0)
            return ChronoUnit.MILLIS.between(now, nextDate)
        }

        private fun diffBetweenHoursInMillis(nextHour: LocalTime) = ChronoUnit.MILLIS.between(LocalTime.now(), nextHour)

        private fun getNextDayOnSchedule(eventOccurrences: Array<Int>): DayOfWeek? {
            if (eventOccurrences.isEmpty()) {
                return null
            }
            if (eventOccurrences.size == 1) {
                return DayOfWeek.of(eventOccurrences[0])
            }
            val today = LocalDateTime.now().dayOfWeek.value
            for (i in eventOccurrences) {
                if (i != today && today < i) {
                    return DayOfWeek.of(i)
                }
            }
            for (i in eventOccurrences) {
                if (i != today) {
                    return DayOfWeek.of(i)
                }
            }
            return null
        }
    }
}