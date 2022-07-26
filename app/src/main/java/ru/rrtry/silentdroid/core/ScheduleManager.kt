package ru.rrtry.silentdroid.core

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.os.Build
import ru.rrtry.silentdroid.Application.Companion.ACTION_ALARM
import ru.rrtry.silentdroid.receivers.AlarmReceiver
import ru.rrtry.silentdroid.receivers.AlarmReceiver.Companion.EXTRA_ALARM
import ru.rrtry.silentdroid.receivers.AlarmReceiver.Companion.EXTRA_END_PROFILE
import ru.rrtry.silentdroid.receivers.AlarmReceiver.Companion.EXTRA_START_PROFILE
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.rrtry.silentdroid.entities.Alarm
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.entities.OngoingAlarm
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.util.ParcelableUtil
import java.time.*
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scheduleCalendar: ScheduleCalendar = ScheduleCalendar(ZonedDateTime.now())

    fun updateAlarmProfile(scheduledAlarms: List<AlarmRelation>?, profile: Profile) {
        scheduledAlarms?.forEach { alarmRelation ->
            if (alarmRelation.startProfile.id == profile.id) {
                scheduleAlarm(
                    alarmRelation.alarm,
                    profile,
                    alarmRelation.endProfile,
                )
            } else {
                scheduleAlarm(
                    alarmRelation.alarm,
                    alarmRelation.startProfile,
                    profile
                )
            }
        }
    }

    fun scheduleAlarm(alarm: Alarm, startProfile: Profile, endProfile: Profile): Boolean {

        scheduleCalendar.now = ZonedDateTime.now()
        scheduleCalendar.alarm = alarm

        scheduleCalendar.getNextOccurrence()?.also { nextOccurrence ->
            getPendingIntent(alarm, startProfile, endProfile, true)?.also {
                setAlarm(nextOccurrence.toInstant().toEpochMilli(), it)
                return true
            }
        }
        return false
    }

    @SuppressLint("MissingPermission", "ObsoleteSdkInt")
    private fun setAlarm(millis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        }
    }

    private fun getPendingIntent(
        alarm: Alarm,
        startProfile: Profile? = null,
        endProfile: Profile? = null,
        create: Boolean
    ): PendingIntent? {
        val intent: Intent = Intent(context, AlarmReceiver::class.java).apply {

            action = ACTION_ALARM

            putExtra(EXTRA_ALARM, ParcelableUtil.toByteArray(alarm))
            putExtra(EXTRA_START_PROFILE, ParcelableUtil.toByteArray(startProfile))
            putExtra(EXTRA_END_PROFILE, ParcelableUtil.toByteArray(endProfile))
        }
        return getBroadcast(
            context, alarm.id, intent,
            (if (create) FLAG_UPDATE_CURRENT else FLAG_NO_CREATE) or FLAG_IMMUTABLE
        )
    }

    fun cancelAlarm(alarm: Alarm): Boolean {
        getPendingIntent(alarm, create = false)?.let {
            alarmManager.cancel(it)
            return true
        }
        return false
    }

    fun cancelAlarms(alarms: List<AlarmRelation>?) {
        alarms?.forEach { alarm ->
            cancelAlarm(alarm.alarm)
        }
    }

    fun meetsSchedule(): Boolean {
        return scheduleCalendar.meetsSchedule
    }

    fun hasPreviouslyFired(alarm: Alarm): Boolean {
        scheduleCalendar.now = ZonedDateTime.now()
        scheduleCalendar.alarm = alarm
        return scheduleCalendar.getPreviousOccurrence() != null
    }

    fun isAlarmValid(alarm: Alarm): Boolean {
        scheduleCalendar.now = ZonedDateTime.now()
        scheduleCalendar.alarm = alarm
        return scheduleCalendar.isValid()
    }

    fun getOngoingAlarm(events: List<AlarmRelation>?): OngoingAlarm? {
        scheduleCalendar.now = ZonedDateTime.now()
        return scheduleCalendar.getOngoingAlarm(events)
    }

    fun getNextOccurrenceFormatted(relation: AlarmRelation): String {

        scheduleCalendar.now = ZonedDateTime.now()
        scheduleCalendar.alarm = relation.alarm

        val displayProfile: String = if (scheduleCalendar.meetsSchedule) {
            relation.endProfile.title
        } else {
            relation.startProfile.title
        }
        var millisBetween: Long = ChronoUnit.MILLIS.between(
            ZonedDateTime.now(), scheduleCalendar.getNextOccurrence()
        )
        return if (millisBetween < MILLIS_PER_MINUTE) {
            "'$displayProfile' is scheduled for less than a minute from now"
        } else {

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
                "'$displayProfile' scheduled for less than a minute from now",
                "'$displayProfile' scheduled %1${'$'}s days from now",
                "'$displayProfile' scheduled %2${'$'}s hours from now",
                "'$displayProfile' scheduled %1${'$'}s days and %2${'$'}s hours from now",
                "'$displayProfile' scheduled %3${'$'}s minutes from now",
                "'$displayProfile' scheduled %1${'$'}s days and %3${'$'}s minutes from now",
                "'$displayProfile' scheduled %2${'$'}s hours and %3${'$'}s minutes from now",
                "'$displayProfile' scheduled %1${'$'}s days, %2${'$'}s hours, %3${'$'}s minutes from now"
            )
            val index: Int = ((if (showDays) 1 else 0)
                    or (if (showHours) 2 else 0)
                    or (if (showMinutes) 4 else 0))
            String.format(templates[index], diffDays, diffHours, diffMinutes)
        }
    }

    companion object {

        private const val MILLIS_PER_MINUTE: Long = 60000L

        fun sortInstances(list: List<AlarmRelation>): List<AlarmRelation> {
            return list.sortedWith { previous, next ->
                val prevAlarm: Alarm = previous.alarm
                val nextAlarm: Alarm = next.alarm
                when {
                    prevAlarm.startTime < nextAlarm.startTime -> -1
                    prevAlarm.startTime == nextAlarm.startTime -> 0
                    else -> 1
                }
            }
        }
    }
}