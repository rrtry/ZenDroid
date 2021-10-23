package com.example.volumeprofiler.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import android.util.Log
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmRelation
import com.example.volumeprofiler.broadcastReceivers.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList

@Singleton
class AlarmUtil @Inject constructor (
        @ApplicationContext private val context: Context
        ) {

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm, profile: Profile, repeating: Boolean, showToast: Boolean = false): Boolean {

        val scheduledDays: ArrayList<Int> = alarm.scheduledDays
        val eventTime: LocalDateTime = alarm.localDateTime
        val pendingIntent: PendingIntent? = getPendingIntent(alarm, profile, true)
        val now: LocalDateTime = LocalDateTime.now()
        val delay: Long = getDelay(now, eventTime, scheduledDays, repeating)

        return if (delay >= 0) {
            setAlarm(now, delay, pendingIntent!!)
            true
        } else {
            false
        }
    }

    private fun setAlarm(now: LocalDateTime, delay: Long, pendingIntent: PendingIntent): Unit {
        val currentTimeInMillis: Long = toEpochMilli(now)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, currentTimeInMillis + delay, pendingIntent)
        }
        else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, currentTimeInMillis + delay, pendingIntent)
        }
    }

    private fun getDelay(now: LocalDateTime, eventTime: LocalDateTime, recurringDays: ArrayList<Int>, repeating: Boolean): Long {
        if ((recurringDays.contains(now.dayOfWeek.value) || recurringDays.isEmpty())
                && now.toLocalTime() < eventTime.toLocalTime()) {
            return localTimeDifference(eventTime.toLocalTime())
        }
        else {
            var nextDay: DayOfWeek? = getNextDayOnSchedule(recurringDays)
            return if (nextDay != null) {
                localDateTimeDifference(nextDay, eventTime.hour, eventTime.minute)
            } else {
                if (!repeating) {
                    nextDay = now.dayOfWeek.plus(1)
                    localDateTimeDifference(nextDay, eventTime.hour, eventTime.minute)
                } else {
                    -1
                }
            }
        }
    }

    fun delayAlarm(alarmRelation: AlarmRelation): Boolean {
        val alarm: Alarm = alarmRelation.alarm
        val profile: Profile = alarmRelation.profile
        return if (pendingIntentExists(alarm, profile) && alarm.scheduledDays.isNotEmpty()) {
            val nextDay: DayOfWeek = getNextDayOnSchedule(alarm.scheduledDays)!!
            val now: LocalDateTime = LocalDateTime.now()
            setAlarm(now, localDateTimeDifference(nextDay, alarm.localDateTime.hour, alarm.localDateTime.minute), getPendingIntent(alarm, profile, true)!!)
            true
        } else {
            false
        }
    }

    private fun getPendingIntent(alarm: Alarm, profile: Profile, createOrUpdate: Boolean): PendingIntent? {
        val id: Int = alarm.id.toInt()
        val intent: Intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Application.ACTION_ALARM_ALERT
            putExtra(AlarmReceiver.EXTRA_ALARM, ParcelableUtil.toByteArray(alarm))
            putExtra(AlarmReceiver.EXTRA_PROFILE, ParcelableUtil.toByteArray(profile))
        }
        return if (createOrUpdate) {
            PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        else {
            PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_NO_CREATE)
        }
    }

    private fun pendingIntentExists(alarm: Alarm, profile: Profile): Boolean {
        return getPendingIntent(alarm, profile, false) != null
    }

    fun cancelAlarm(alarm: Alarm, profile: Profile): Unit {
        if (pendingIntentExists(alarm, profile)) {
            alarmManager.cancel(getPendingIntent(alarm, profile, false))
        }
        else {
            Log.i("AlarmUtil", "failed to cancel alarm")
        }
    }

    fun cancelAlarms(list: List<AlarmRelation>): Unit {
        for (i in list) {
            cancelAlarm(i.alarm, i.profile)
        }
    }

    fun setAlarms(list: List<AlarmRelation>): Unit {
        for (i in list) {
            scheduleAlarm(i.alarm, i.profile, false)
        }
    }

    companion object {

        private const val LOG_TAG: String = "AlarmUtil"
        const val EXIT_SUCCESS: Long = -1

        // TODO provide implementation
        fun isDismissAvailable(): Boolean {
            return false
        }

        private fun toEpochMilli(now: LocalDateTime): Long {
            return now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        private fun localDateTimeDifference(nextDay: DayOfWeek, hour: Int, minute: Int): Long {
            val now: LocalDateTime = LocalDateTime.now()
            val nextDate: LocalDateTime = now.with(TemporalAdjusters.next(nextDay)).withHour(hour).withMinute(minute).withSecond(0)
            return ChronoUnit.MILLIS.between(now, nextDate)
        }

        private fun localTimeDifference(nextHour: LocalTime) = ChronoUnit.MILLIS.between(LocalTime.now(), nextHour)

        private fun getNextDayOnSchedule(eventOccurrences: ArrayList<Int>): DayOfWeek? {
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