package com.example.volumeprofiler.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import android.util.Log
import android.widget.Toast
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.broadcastReceivers.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList

@Singleton
class AlarmUtil @Inject constructor (
        @ApplicationContext private val context: Context
        ) {

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm, profile: Profile, repeating: Boolean, showToast: Boolean = false): Boolean {
        val pendingIntent: PendingIntent = createPendingIntent(alarm, profile)
        val now: LocalDateTime = LocalDateTime.now()
        val delay: Long = getDelay(alarm, repeating)

        return if (delay >= 0) {
            setAlarm(now, delay, pendingIntent)
            if (showToast) {
                Toast.makeText(context, formatRemainingTimeUtilAlarm(alarm), Toast.LENGTH_LONG)
                    .show()
            }
            true
        } else {
            false
        }
    }

    @Suppress("obsoleteSdkInt")
    private fun setAlarm(now: LocalDateTime, delay: Long, pendingIntent: PendingIntent): Unit {
        val currentTimeInMillis: Long = toEpochMilli(now)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, currentTimeInMillis + delay, pendingIntent)
        }
        else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, currentTimeInMillis + delay, pendingIntent)
        }
    }

    /*
    fun delayAlarm(alarmRelation: AlarmRelation): Boolean {
        val alarm: Alarm = alarmRelation.alarm
        val profile: Profile = alarmRelation.profile
        val pendingIntent: PendingIntent? = getPendingIntent(alarm, profile)
        return if (pendingIntent != null && alarm.scheduledDays.isNotEmpty()) {
            val nextDay: DayOfWeek = getNextDayOnSchedule(alarm.scheduledDays)!!
            val now: LocalDateTime = LocalDateTime.now()
            setAlarm(now, localDateTimeDifference(nextDay, alarm.localDateTime.hour, alarm.localDateTime.minute), pendingIntent)
            true
        } else {
            false
        }
    }
     */

    private fun createPendingIntent(alarm: Alarm, profile: Profile): PendingIntent {
        val id: Int = alarm.id.toInt()
        val intent: Intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Application.ACTION_ALARM_ALERT
            putExtra(AlarmReceiver.EXTRA_ALARM, ParcelableUtil.toByteArray(alarm))
            putExtra(AlarmReceiver.EXTRA_PROFILE, ParcelableUtil.toByteArray(profile))
        }
        return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getPendingIntent(alarm: Alarm, profile: Profile): PendingIntent? {
        val id: Int = alarm.id.toInt()
        val intent: Intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Application.ACTION_ALARM_ALERT
            putExtra(AlarmReceiver.EXTRA_ALARM, ParcelableUtil.toByteArray(alarm))
            putExtra(AlarmReceiver.EXTRA_PROFILE, ParcelableUtil.toByteArray(profile))
        }
        return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
    }

    fun cancelAlarm(alarm: Alarm, profile: Profile): Unit {
        val pendingIntent: PendingIntent? = getPendingIntent(alarm, profile)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
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

    fun setAlarms(list: List<AlarmRelation>, repeating: Boolean = false): Unit {
        for (i in list) {
            scheduleAlarm(i.alarm, i.profile, false)
        }
    }

    companion object {

        private fun toEpochMilli(now: LocalDateTime): Long {
            return now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        private fun localDateTimeDifference(nextDay: DayOfWeek, hour: Int, minute: Int, second: Int): Long {
            val now: LocalDateTime = LocalDateTime.now()
            val nextDate: LocalDateTime = now.with(TemporalAdjusters.next(nextDay)).withHour(hour).withMinute(minute).withSecond(second)
            return ChronoUnit.MILLIS.between(now, nextDate)
        }

        private fun getDelay(alarm: Alarm, repeating: Boolean): Long {
            val now: LocalDateTime = LocalDateTime.now()
            val recurringDays: ArrayList<Int> = alarm.scheduledDays
            val eventTime: LocalDateTime = alarm.localDateTime
            if ((recurringDays.contains(now.dayOfWeek.value) || recurringDays.isEmpty())
                && now.toLocalTime() < eventTime.toLocalTime()) {
                return localTimeDifference(eventTime.toLocalTime())
            }
            else {
                var nextDay: DayOfWeek? = getAlarmNextDay(recurringDays)
                return if (nextDay != null) {
                    localDateTimeDifference(nextDay, eventTime.hour, eventTime.minute, 0)
                } else {
                    if (!repeating) {
                        nextDay = now.dayOfWeek.plus(1)
                        localDateTimeDifference(nextDay, eventTime.hour, eventTime.minute, 0)
                    } else {
                        -1
                    }
                }
            }
        }

        private fun localTimeDifference(localTime: LocalTime): Long {
            return ChronoUnit.MILLIS.between(LocalTime.now(), localTime)
        }

        fun sortAlarms(list: List<AlarmRelation>): List<AlarmRelation> {
            return list.sortedWith { previous, next ->
                val prevDelay: Long = getDelay(previous.alarm, false)
                val nextDelay: Long = getDelay(next.alarm, false)
                when {
                    prevDelay < nextDelay -> -1
                    prevDelay == nextDelay -> 0
                    else -> 1
                }
            }
        }

        private fun formatRemainingTimeUtilAlarm(alarm: Alarm): String {

            var diffMillis: Long = getDelay(alarm, false)

            return if (diffMillis < MILLIS_PER_MINUTE) {
                "Alarm is set for less than a minute from now"
            }
            else {
                val remainder = diffMillis % MILLIS_PER_MINUTE
                if (remainder > 0) {
                    diffMillis += MILLIS_PER_MINUTE - remainder
                }

                val diffMinutes: Long = ((diffMillis / (1000 * 60)) % 60)
                val diffHours: Long = ((diffMillis / (1000 * 60 * 60)) % 24)
                val diffDays: Long = ((diffMillis / (1000 * 60 * 60 * 24)) % 365)

                val showMinutes: Boolean = diffMinutes > 0
                val showHours: Boolean = diffHours > 0
                val showDays: Boolean = diffDays > 0

                val templates: Array<String> = arrayOf(
                    "Alarm set for less than a minute from now",
                    "Alarm set for %1${'$'}s days from now",
                    "Alarm set for %2${'$'}s hours from now",
                    "Alarm set for %1${'$'}s and %2${'$'}s hours from now",
                    "Alarm set for %3${'$'}s minutes from now",
                    "Alarm set for %1${'$'}s days and %3${'$'}s minutes from now",
                    "Alarm set for %2${'$'}s and %3${'$'}s minutes from now",
                    "Alarm set for %1${'$'}s hours, %2${'$'}s hours, %3${'$'}s minutes"
                )
                val index = ((if (showDays) 1 else 0)
                        or (if (showHours) 2 else 0)
                        or (if (showMinutes) 4 else 0))
                String.format(templates[index], diffDays, diffHours, diffMinutes)
            }
        }

        private fun getAlarmNextDay(days: ArrayList<Int>): DayOfWeek? {
            if (days.isEmpty()) {
                return null
            }
            if (days.size == 1) {
                return DayOfWeek.of(days[0])
            }
            val today = LocalDateTime.now().dayOfWeek.value
            for (i in days) {
                if (i != today && today < i) {
                    return DayOfWeek.of(i)
                }
            }
            for (i in days) {
                if (i != today) {
                    return DayOfWeek.of(i)
                }
            }
            return null
        }

        private const val MILLIS_PER_MINUTE: Long = 60000L
    }
}