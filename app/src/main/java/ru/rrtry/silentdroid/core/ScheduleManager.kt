package ru.rrtry.silentdroid.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_RECEIVER_FOREGROUND
import android.os.Build
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import androidx.activity.result.ActivityResultLauncher
import ru.rrtry.silentdroid.Application.Companion.ACTION_ALARM
import ru.rrtry.silentdroid.receivers.AlarmReceiver
import ru.rrtry.silentdroid.receivers.AlarmReceiver.Companion.EXTRA_ALARM
import ru.rrtry.silentdroid.receivers.AlarmReceiver.Companion.EXTRA_END_PROFILE
import ru.rrtry.silentdroid.receivers.AlarmReceiver.Companion.EXTRA_START_PROFILE
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.db.repositories.AlarmRepository
import ru.rrtry.silentdroid.entities.Alarm
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.entities.PreviousAndNextTrigger
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.event.EventBus
import ru.rrtry.silentdroid.util.ParcelableUtil
import java.text.NumberFormat
import java.time.*
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleManager @Inject constructor(@ApplicationContext private val context: Context) {

    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var eventBus: EventBus

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scheduleCalendar: ScheduleCalendar = ScheduleCalendar(ZonedDateTime.now())
    val meetsSchedule: Boolean get() = scheduleCalendar.meetsSchedule

    fun scheduleAlarm(alarm: Alarm, startProfile: Profile, endProfile: Profile): Boolean {

        if (!canScheduleExactAlarms()) {
            return false
        }

        scheduleCalendar.now = ZonedDateTime.now()
        scheduleCalendar.alarm = alarm

        scheduleCalendar.getNextOccurrence()?.also { nextOccurrence ->
            setAlarm(
                nextOccurrence.toInstant().toEpochMilli(),
                getPendingIntent(alarm, startProfile, endProfile, true)!!
            )
            return true
        }
        return false
    }

    @Suppress("obsoleteSdkInt")
    private fun setAlarm(millis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        }
    }

    suspend fun updateSchedule(alarms: List<AlarmRelation>?) {
        alarms?.forEach { relation ->
            setNextAlarm(relation)
        }
    }

    suspend fun getPreviousAndNextTrigger(): PreviousAndNextTrigger? {
        scheduleCalendar.now = ZonedDateTime.now()
        return scheduleCalendar.getCurrentAlarmInstance(alarmRepository.getEnabledAlarms())
    }

    fun getPreviousAndNextTrigger(alarms: List<AlarmRelation>?): PreviousAndNextTrigger? {
        scheduleCalendar.now = ZonedDateTime.now()
        return scheduleCalendar.getCurrentAlarmInstance(alarms)
    }

    private suspend fun setNextAlarm(relation: AlarmRelation) {
        scheduleAlarm(
            relation.alarm,
            relation.startProfile,
            relation.endProfile
        ).also { scheduled ->
            if (!scheduled) setAlarmStateCancelled(relation.alarm)
        }
    }

    suspend fun setNextAlarm(
        alarm: Alarm,
        startProfile: Profile,
        endProfile: Profile)
    {
        scheduleAlarm(
            alarm,
            startProfile,
            endProfile
        ).also { scheduled ->
            if (!scheduled) setAlarmStateCancelled(alarm)
        }
    }

    private suspend fun setAlarmStateCancelled(alarm: Alarm) {
        cancelAlarm(alarm)
        alarmRepository.cancelAlarm(alarm)
        eventBus.updateAlarmState(alarm)
    }

    fun cancelAlarm(alarm: Alarm): Boolean {
        getPendingIntent(alarm, create = false)?.let {
            alarmManager.cancel(it)
            return true
        }
        return false
    }

    fun cancelAlarms(alarms: List<AlarmRelation>?) {
        alarms?.forEach { alarm -> cancelAlarm(alarm.alarm) }
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

    fun updateAlarms(scheduledAlarms: List<AlarmRelation>?, profile: Profile) {
        scheduledAlarms?.forEach { relation ->
            if (relation.startProfile.id == profile.id ||
                relation.endProfile.id == profile.id)
            {
                scheduleAlarm(
                    relation.alarm,
                    relation.startProfile,
                    relation.endProfile
                )
            }
        }
    }

    private fun getPendingIntent(
        alarm: Alarm,
        startProfile: Profile? = null,
        endProfile: Profile? = null,
        create: Boolean
    ): PendingIntent? {
        Intent(context, AlarmReceiver::class.java).apply {

            action = ACTION_ALARM

            addFlags(FLAG_RECEIVER_FOREGROUND)
            putExtra(EXTRA_ALARM, ParcelableUtil.toByteArray(alarm))
            putExtra(EXTRA_START_PROFILE, ParcelableUtil.toByteArray(startProfile))
            putExtra(EXTRA_END_PROFILE, ParcelableUtil.toByteArray(endProfile))

            return getBroadcast(
                context, alarm.id, this,
                (if (create) FLAG_UPDATE_CURRENT else FLAG_NO_CREATE) or FLAG_IMMUTABLE
            )
        }
    }

    fun requestExactAlarmPermission(launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launcher.launch(Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    fun getNextOccurrenceFormatted(relation: AlarmRelation): String {

        scheduleCalendar.now = ZonedDateTime.now()
        scheduleCalendar.alarm = relation.alarm

        var millisBetween: Long = ChronoUnit.MILLIS.between(scheduleCalendar.now, scheduleCalendar.getNextOccurrence())
        val displayProfile: String = if (scheduleCalendar.meetsSchedule) {
            relation.endProfile.title
        } else {
            relation.startProfile.title
        }
        if (millisBetween < MILLIS_PER_MINUTE) {
            return context.resources.getString(
                R.string.scheduled_for_less_than_a_minute,
                displayProfile
            )
        }

        val templates: Array<String> = context.resources.getStringArray(R.array.time_remaining_template)

        val remainder = millisBetween % MILLIS_PER_MINUTE
        if (remainder > 0) millisBetween += MILLIS_PER_MINUTE - remainder

        val diffMinutes: Int = ((millisBetween / (1000 * 60)) % 60).toInt()
        val diffHours: Int = ((millisBetween / (1000 * 60 * 60)) % 24).toInt()
        val diffDays: Int = ((millisBetween / (1000 * 60 * 60 * 24)) % 365).toInt()

        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        val days: String = context.resources.getQuantityString(R.plurals.days, diffDays, numberFormat.format(diffDays))
        val hours: String = context.resources.getQuantityString(R.plurals.hours, diffHours, numberFormat.format(diffHours))
        val minutes: String = context.resources.getQuantityString(R.plurals.minutes, diffMinutes, numberFormat.format(diffMinutes))

        val index: Int = ((if (diffDays > 0) 1 else 0)
                or (if (diffHours > 0) 2 else 0)
                or (if (diffMinutes > 0) 4 else 0))

        return context.resources.getString(
            R.string.time_remaining,
            displayProfile,
            templates[index].format(days, hours, minutes))
    }

    companion object {

        private const val MILLIS_PER_MINUTE: Long = 60000L
    }
}