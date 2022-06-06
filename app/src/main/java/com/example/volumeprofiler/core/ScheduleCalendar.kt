package com.example.volumeprofiler.core

import android.util.Log
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.OngoingAlarm
import com.example.volumeprofiler.entities.Profile
import java.time.*
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters

class ScheduleCalendar(var now: ZonedDateTime) {

    lateinit var alarm: Alarm
    var meetsSchedule: Boolean = false
        private set

    constructor(now: ZonedDateTime, alarm: Alarm): this(now) {
        this.alarm = alarm
    }

    fun getNextOccurrence(): ZonedDateTime? {
        return if (isValid()) {

            meetsSchedule = meetsSchedule()

            if (meetsSchedule) {
                getNextEndTime()
            } else {
                getNextStartTime()
            }
        } else null
    }

    private fun isRecurring(): Boolean {
        return alarm.scheduledDays != WeekDay.NONE
    }

    private fun isDayInSchedule(mask: Int, day: DayOfWeek): Boolean {
        return (mask and WeekDay.fromDay(day.value)) != WeekDay.NONE
    }

    fun isValid(): Boolean {
        return isValid(now, alarm)
    }

    fun getPreviousOccurrence(): ZonedDateTime? {

        meetsSchedule = meetsSchedule()

        return if (meetsSchedule) {
            getPreviousStartTime()
        } else {
            getPreviousEndTime()
        }
    }

    private fun getNextAlarmTime(alarms: List<AlarmRelation>): LocalDateTime? {
        return alarms
            .mapNotNull {
                alarm = it.alarm
                getNextOccurrence()
            }.minOrNull()?.toLocalDateTime()
    }

    fun getOngoingAlarm(alarms: List<AlarmRelation>?): OngoingAlarm? {

        if (alarms.isNullOrEmpty()) {
            return null
        }

        val nextAlarmTime: LocalDateTime? = getNextAlarmTime(alarms)
        return try {
            alarms.map { relation ->

                alarm = relation.alarm

                val previousAlarmTime: LocalDateTime? = getPreviousOccurrence()?.toLocalDateTime()
                val profile: Profile? = if (isRecurring() || previousAlarmTime != null) {
                    if (meetsSchedule) relation.startProfile else relation.endProfile
                } else {
                    null
                }
                OngoingAlarm(
                    profile,
                    nextAlarmTime,
                    previousAlarmTime,
                    relation
                )
            }.sortedByDescending {
                it.from
            }.first()
        } catch (e: NoSuchElementException) {
            null
        }
    }

    private fun getNextStartTime(): ZonedDateTime {
        return if (isRecurring()) {

            val startTime: LocalTime = alarm.startTime
            val inclusive: Boolean = now.toLocalTime() < startTime

            val nextDay: DayOfWeek = getNextWeekDay(inclusive)

            now.with(getDayOfWeekAdjuster(nextDay, inclusive))
                .withHour(startTime.hour)
                .withMinute(startTime.minute)
                .withSecond(0)
                .withNano(0)
        } else {
            alarm.startDateTime!!.atZone(alarm.zoneId)
        }
    }

    private fun getPreviousStartTime(): ZonedDateTime? {
        return if (isRecurring()) {

            val instanceStartTime: ZonedDateTime = now
                .withHour(alarm.startTime.hour)
                .withMinute(alarm.startTime.minute)
                .withSecond(0)
                .withNano(0)

            val isOvernight: Boolean =
                !isDayInSchedule(alarm.scheduledDays, instanceStartTime.dayOfWeek) &&
                        alarm.startTime >= alarm.endTime

            if (isOvernight) instanceStartTime.minusDays(1) else instanceStartTime

        } else {
            alarm.startDateTime!!.atZone(alarm.zoneId) // removed useless isValid() check
        }
    }

    private fun getNextEndTime(): ZonedDateTime {
        return if (isRecurring()) {

            val endTime: ZonedDateTime = now
                .withHour(alarm.endTime.hour)
                .withMinute(alarm.endTime.minute)
                .withSecond(0)
                .withNano(0)

            if (endTime <= now) endTime.plusDays(1) else endTime
        } else {
            alarm.endDateTime!!.atZone(alarm.zoneId)
        }
    }

    private fun meetsDayOfWeek(): Boolean {
        return isDayInSchedule(alarm.scheduledDays, now.dayOfWeek) ||
                (isDayInSchedule(alarm.scheduledDays, now.dayOfWeek - 1) &&
                        alarm.startTime >= alarm.endTime)
    }

    private fun meetsSchedule(): Boolean {
        if (isRecurring()) {
            return meetsDayOfWeek() && meetsScheduledHours(
                now.toLocalTime(), alarm.startTime, alarm.endTime
            )
        } else {
            now.toLocalDateTime().also {
                return !it.isBefore(alarm.startDateTime) && it.isBefore(alarm.endDateTime)
            }
        }
    }

    private fun getPreviousEndTime(): ZonedDateTime? {
        return if (isRecurring()) {

            val dayOfWeekAdjuster: TemporalAdjuster = TemporalAdjusters.previousOrSame(getPreviousWeekDay())

            now.with(dayOfWeekAdjuster)
                .withHour(alarm.endTime.hour)
                .withMinute(alarm.endTime.minute)
                .withSecond(0)
                .withNano(0)

        } else if (!isValid()) {
            alarm.endDateTime?.atZone(alarm.zoneId)
        } else {
            null
        }
    }

    private fun getPreviousWeekDay(): DayOfWeek {

        var previousDay: DayOfWeek = now.dayOfWeek
        val isOvernight: Boolean = alarm.startTime >= alarm.endTime

        if (isDayInSchedule(alarm.scheduledDays, previousDay) &&
            alarm.startTime > now.toLocalTime())
        {
            previousDay -= 1
        }
        while (!isDayInSchedule(alarm.scheduledDays, previousDay)) {
            previousDay -= 1
        }
        if (isOvernight) {
            previousDay += 1
        }
        return previousDay
    }

    private fun getNextWeekDay(inclusive: Boolean): DayOfWeek {

        val today: DayOfWeek = now.dayOfWeek

        return when {
            alarm.scheduledDays != WeekDay.NONE -> getNextWeekDay()
            inclusive -> today
            else -> today.plus(1)
        }
    }

    private fun getNextWeekDay(): DayOfWeek {

        var nextDay: DayOfWeek = now.dayOfWeek
        val inclusive: Boolean = now.toLocalTime() < alarm.startTime

        if (inclusive && isDayInSchedule(alarm.scheduledDays, nextDay)) {
            return nextDay
        }
        while (!isDayInSchedule(alarm.scheduledDays, nextDay)) {
            nextDay += 1
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

    companion object {

        fun isValid(now: ZonedDateTime, alarm: Alarm): Boolean {
            if (alarm.scheduledDays != WeekDay.NONE) {
                return true
            }
            return now
                .toLocalDateTime()
                .isBefore(alarm.endDateTime)
        }

        private fun meetsScheduledHours(localTime: LocalTime, startTime: LocalTime, endTime: LocalTime): Boolean {
            return if (startTime >= endTime) {
                !localTime.isBefore(startTime) || localTime.isBefore(endTime)
            } else {
                !localTime.isBefore(startTime) && localTime.isBefore(endTime)
            }
        }

        fun getStartAndEndDate(startTime: LocalTime, endTime: LocalTime, scheduleForNextDay: Boolean = false): Pair<LocalDateTime, LocalDateTime> {

            val now: LocalDateTime = LocalDateTime.now()

            var start: LocalDateTime = now
                .withHour(startTime.hour)
                .withMinute(startTime.minute)
                .withSecond(0)
                .withNano(0)

            var end: LocalDateTime = now
                .withHour(endTime.hour)
                .withMinute(endTime.minute)
                .withSecond(0)
                .withNano(0)

            if (scheduleForNextDay) {
                if (now >= start) {
                    start = start.plusDays(1)
                }
                while (start >= end) {
                    end = end.plusDays(1)
                }
                return Pair(start, end)
            }

            if (!meetsScheduledHours(now.toLocalTime(), startTime, endTime)) {
                if (now >= start) {
                    start = start.plusDays(1)
                }
                if (now >= end) {
                    end = end.plusDays(1)
                }
            } else if (start >= end) {
                end = end.plusDays(1)
            }
            return Pair(start, end)
        }
    }
}