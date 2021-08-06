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
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.receivers.AlarmReceiver
import java.time.LocalTime
import java.time.ZoneId
import kotlin.collections.ArrayList

class AlarmUtil private constructor (private val context: Context) {

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun setAlarm(alarm: Alarm, profile: Profile, onReschedule: Boolean): Long {
        val alarmId: Long = alarm.id
        val recurringDays: ArrayList<Int> = alarm.workingsDays
        val eventTime: LocalDateTime = alarm.localDateTime
        val pendingIntent: PendingIntent? = getPendingIntent(alarm, profile, true)
        val now: LocalDateTime = LocalDateTime.now()
        var delay: Long
        if ((recurringDays.contains(now.dayOfWeek.value) || recurringDays.isEmpty())
                && now.toLocalTime() < eventTime.toLocalTime()) {
            delay = diffBetweenHoursInMillis(eventTime.toLocalTime())
        }
        else {
            var nextDay: DayOfWeek? = getNextDayOnSchedule(recurringDays)
            if (nextDay != null) {
                delay = diffBetweenDatesInMillis(nextDay, eventTime.hour, eventTime.minute)
            }
            else {
                if (!onReschedule) {
                    nextDay = now.dayOfWeek.plus(1)
                    delay = diffBetweenDatesInMillis(nextDay, eventTime.hour, eventTime.minute)
                }
                else {
                    return alarmId
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
        return EXIT_SUCCESS
    }

    private fun getPendingIntent(alarm: Alarm, profile: Profile, shouldCreate: Boolean): PendingIntent? {
        val id: Int = alarm.id.toInt()
        val intent: Intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = Application.ACTION_ALARM_TRIGGER
            this.putExtra(AlarmReceiver.EXTRA_ALARM, ParcelableUtil.toByteArray(alarm))
            this.putExtra(AlarmReceiver.EXTRA_PROFILE, ParcelableUtil.toByteArray(profile))
        }
        return if (shouldCreate) {
            PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        else {
            PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_NO_CREATE)
        }
    }

    fun cancelAlarm(alarm: Alarm, profile: Profile): Unit {
        val pendingIntent: PendingIntent? = getPendingIntent(alarm, profile, false)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
        else {
            Log.i("AlarmUtil", "failed to cancel alarm")
        }
    }

    fun cancelMultipleAlarms(list: List<AlarmTrigger>): Unit {
        for (i in list) {
            cancelAlarm(i.alarm, i.profile)
        }
    }

    fun setMultipleAlarms(list: List<AlarmTrigger>): Unit {
        for (i in list) {
            setAlarm(i.alarm, i.profile, false)
        }
    }

    companion object {

        private const val LOG_TAG: String = "AlarmUtil"
        const val EXIT_SUCCESS: Long = -1

        private var INSTANCE: AlarmUtil? = null

        fun getInstance(): AlarmUtil {

            if (INSTANCE != null) {
                return INSTANCE!!
            }
            else {
                throw IllegalStateException("Singleton must be initialized")
            }
        }

        fun initialize(context: Context) {

            if (INSTANCE == null) {
                INSTANCE = AlarmUtil(context)
            }
        }

        private fun diffBetweenDatesInMillis(nextDay: DayOfWeek, hour: Int, minute: Int): Long {
            val now: LocalDateTime = LocalDateTime.now()
            val nextDate: LocalDateTime = now.with(TemporalAdjusters.next(nextDay)).withHour(hour).withMinute(minute).withSecond(0)
            return ChronoUnit.MILLIS.between(now, nextDate)
        }

        private fun diffBetweenHoursInMillis(nextHour: LocalTime) = ChronoUnit.MILLIS.between(LocalTime.now(), nextHour)

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