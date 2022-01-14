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
import com.example.volumeprofiler.entities.*
import com.example.volumeprofiler.services.AlarmService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.*
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjuster
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmUtil @Inject constructor (
        @ApplicationContext private val context: Context
        ) {

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(event: Event, profile: Profile, state: Event.State): Unit {
        val pendingIntent: PendingIntent = getEventPendingIntent(event, profile, state, true)!!
        when (state) {
            Event.State.START -> {
                setAlarm(event.instanceBeginTime, pendingIntent)
            }
            Event.State.END -> {
                setAlarm(event.instanceEndTime, pendingIntent)
            }
        }
    }

    fun scheduleAlarm(alarm: Alarm, profile: Profile, showToast: Boolean = false): Unit {
        val pendingIntent: PendingIntent = getAlarmPendingIntent(alarm, profile, true)!!
        val millis: Long = toEpochMilli(getNextAlarmTime(alarm))
        setAlarm(millis, pendingIntent)
        if (showToast) {
            Toast.makeText(context, formatRemainingTimeUtilAlarm(alarm), Toast.LENGTH_LONG)
                .show()
        }
    }

    @Suppress("obsoleteSdkInt")
    private fun setAlarm(timestamp: Long, pendingIntent: PendingIntent): Unit {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent)
        }
    }

    private fun getEventPendingIntent(event: Event, profile: Profile?, state: Event.State?, create: Boolean): PendingIntent? {
        val id: Int = event.id
        val intent: Intent = Intent(context, AlarmService::class.java).apply {
            action = Application.ACTION_CALENDAR_EVENT_TRIGGER
            putExtra(AlarmService.EXTRA_EVENT, ParcelableUtil.toByteArray(event))
            if (profile != null) {
                putExtra(AlarmService.EXTRA_PROFILE, ParcelableUtil.toByteArray(profile))
            }
            putExtra(AlarmService.EXTRA_EVENT_STATE, state)
        }
        var flags: Int = if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        flags = flags or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
            PendingIntent.getForegroundService(context, id, intent, flags)
        } else {
            PendingIntent.getService(context, id, intent, flags)
        }
    }

    private fun getAlarmPendingIntent(alarm: Alarm, profile: Profile?, create: Boolean): PendingIntent? {
        val id: Int = alarm.id.toInt()
        val intent: Intent = Intent(context, AlarmService::class.java).apply {
            action = Application.ACTION_ALARM_TRIGGER
            putExtra(AlarmService.EXTRA_ALARM, ParcelableUtil.toByteArray(alarm))
            if (profile != null) {
                putExtra(AlarmService.EXTRA_PROFILE, ParcelableUtil.toByteArray(profile))
            }
        }
        var flags: Int = if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        flags = flags or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
            PendingIntent.getForegroundService(context, id, intent, flags)
        } else {
            PendingIntent.getService(context, id, intent, flags)
        }
    }

    fun cancelAlarm(event: Event): Unit {
        val pendingIntent: PendingIntent? = getEventPendingIntent(event, null, null, false)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        } else {
            Log.w("AlarmUtil", "failed to cancel alarm")
        }
    }

    fun cancelAlarm(alarm: Alarm, profile: Profile): Unit {
        val pendingIntent: PendingIntent? = getAlarmPendingIntent(alarm, null, false)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
        else {
            Log.w("AlarmUtil", "failed to cancel alarm")
        }
    }

    fun cancelAlarms(list: List<AlarmRelation>): Unit {
        for (i in list) {
            cancelAlarm(i.alarm, i.profile)
        }
    }

    companion object {

        private fun isBitSet(mask: Int, day: Int): Boolean {
            val bit: Int = WeekDay.fromDay(day)
            return (mask and bit) != 0
        }

        internal fun getNextAlarmTime(days: Int, alarmStartTime: LocalTime): ZonedDateTime {

            val now: ZonedDateTime = ZonedDateTime.now()
            val inclusive: Boolean = now.toLocalTime() < alarmStartTime
            val nextDay: DayOfWeek = getNextAlarmDay(now, days, inclusive)

            return now.with(getDayOfWeekAdjuster(nextDay, inclusive))
                .withHour(alarmStartTime.hour)
                .withMinute(alarmStartTime.minute)
                .withSecond(0)
                .withNano(0)
        }

        private fun getNextAlarmDay(now: ZonedDateTime, days: Int, inclusive: Boolean): DayOfWeek {

            val today: DayOfWeek = now.dayOfWeek

            return when {
                days != 0 -> {
                    getNextAlarmDay(today, days, inclusive)
                }
                inclusive -> {
                    today
                }
                else -> {
                    today.plus(1)
                }
            }
        }

        private fun getNextAlarmDay(today: DayOfWeek, days: Int, inclusive: Boolean): DayOfWeek {
            var nextDay: DayOfWeek = today

            if (inclusive && isBitSet(days, nextDay.value)) {
                return nextDay
            }
            while (true) {
                nextDay = nextDay.plus(1)
                if (isBitSet(days, nextDay.value)) {
                    break
                }
            }
            return nextDay
        }

        private fun getDayOfWeekAdjuster(day: DayOfWeek, inclusive: Boolean): TemporalAdjuster {
            return if (inclusive) {
                TemporalAdjusters.nextOrSame(day)
            } else {
                TemporalAdjusters.next(day)
            }
        }

        internal fun getNextAlarmTime(alarm: Alarm): ZonedDateTime {

            val now: ZonedDateTime = ZonedDateTime.now()
            val startTime: LocalTime = alarm.localStartTime
            val inclusive: Boolean = now.toLocalTime() < startTime
            val nextDay: DayOfWeek = getNextAlarmDay(now, alarm.scheduledDays, inclusive)

            return now.with(getDayOfWeekAdjuster(nextDay, inclusive))
                .withHour(startTime.hour)
                .withMinute(startTime.minute)
                .withSecond(0)
                .withNano(0)
        }

        internal fun isAlarmValid(alarm: Alarm): Boolean {
            return if (alarm.scheduledDays == 0) {
                !isAlarmInstanceObsolete(alarm)
            } else {
                true
            }
        }

        internal fun isAlarmInstanceObsolete(alarm: Alarm): Boolean {
            val now: LocalDateTime = LocalDateTime.now()
            val instanceStartTime: LocalDateTime = alarm.instanceStartTime
                .atZone(alarm.zoneId)
                .toLocalDateTime()
            return now.isAfter(instanceStartTime)
        }

        internal fun toEpochMilli(now: LocalDateTime): Long {
            return now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        internal fun toEpochMilli(zdt: ZonedDateTime): Long {
            return zdt.toInstant().toEpochMilli()
        }

        private fun formatRemainingTimeUtilAlarm(alarm: Alarm): String {

            var millisBetween: Long = ChronoUnit.MILLIS.between(LocalDateTime.now(), getNextAlarmTime(alarm))

            return if (millisBetween < MILLIS_PER_MINUTE) {
                "Alarm is set for less than a minute from now"
            }
            else {
                val remainder = millisBetween % MILLIS_PER_MINUTE
                if (remainder > 0) {
                    millisBetween += MILLIS_PER_MINUTE - remainder
                }

                val diffMinutes: Long = ((millisBetween / (1000 * 60)) % 60)
                val diffHours: Long = ((millisBetween / (1000 * 60 * 60)) % 24)
                val diffDays: Long = ((millisBetween / (1000 * 60 * 60 * 24)) % 365)

                val showMinutes: Boolean = diffMinutes > 0
                val showHours: Boolean = diffHours > 0
                val showDays: Boolean = diffDays > 0

                val templates: Array<String> = arrayOf(
                    "Alarm set for less than a minute from now",
                    "Alarm set for %1${'$'}s days from now",
                    "Alarm set for %2${'$'}s hours from now",
                    "Alarm set for %1${'$'}s days and %2${'$'}s hours from now",
                    "Alarm set for %3${'$'}s minutes from now",
                    "Alarm set for %1${'$'}s days and %3${'$'}s minutes from now",
                    "Alarm set for %2${'$'}s hours and %3${'$'}s minutes from now",
                    "Alarm set for %1${'$'}s hours, %2${'$'}s hours, %3${'$'}s minutes"
                )
                val index = ((if (showDays) 1 else 0)
                        or (if (showHours) 2 else 0)
                        or (if (showMinutes) 4 else 0))
                String.format(templates[index], diffDays, diffHours, diffMinutes)
            }
        }

        fun sortInstances(list: List<AlarmRelation>): List<AlarmRelation> {
            return list.sortedWith { previous, next ->
                val prevAlarm: Alarm = previous.alarm
                val nextAlarm: Alarm = next.alarm
                when {
                    prevAlarm.localStartTime < nextAlarm.localStartTime -> -1
                    prevAlarm.localStartTime == nextAlarm.localStartTime -> 0
                    else -> 1
                }
            }
        }

        fun getLocalTimeUpdateTaskDelay(): Long {

            val now: LocalTime = LocalTime.now()
            val adjusted: LocalTime = now.withMinute(now.minute + 1).withSecond(0)

            val currentMillis: Int = now.get(ChronoField.MILLI_OF_DAY)
            val adjustedMillis: Int = adjusted.get(ChronoField.MILLI_OF_DAY)

            return (adjustedMillis - currentMillis).toLong()
        }

        private const val MILLIS_PER_MINUTE: Long = 60000L
    }
}