package com.example.volumeprofiler.core

import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.OngoingAlarm
import java.time.*
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters

class ScheduleCalendar(
    var now: ZonedDateTime,
) {

    lateinit var alarm: Alarm

    var meetsSchedule: Boolean = false
        private set

    var currentAlarm: Alarm? = null
        private set

    constructor(now: ZonedDateTime, alarm: Alarm): this(now) {
        this.alarm = alarm
    }

    internal fun getNextOccurrence(): ZonedDateTime? {
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

    private fun isValid(): Boolean {
        if (alarm.scheduledDays != WeekDay.NONE) {
            return true
        }
        return now
            .toLocalDateTime()
            .isBefore(alarm.endDateTime)
    }

    private fun getPreviousOccurrence(): ZonedDateTime? {

        meetsSchedule = meetsSchedule()

        return if (meetsSchedule) {
            getPreviousStartTime()
        } else {
            getPreviousEndTime()
        }
    }

    internal fun getOngoingAlarm(alarms: List<AlarmRelation>?): OngoingAlarm? {
        if (alarms.isNullOrEmpty()) {
            return null
        }

        val nextAlarmTime: ZonedDateTime = alarms.minOf { nextAlarm ->
            this.alarm = nextAlarm.alarm
            getNextOccurrence()!!
        }

        return alarms.mapNotNull { relation ->

            alarm = relation.alarm
            val previousTime: ZonedDateTime? = getPreviousOccurrence()

            if (previousTime != null) {
                OngoingAlarm(
                    if (meetsSchedule) relation.startProfile else relation.endProfile,
                    nextAlarmTime,
                    previousTime,
                    relation.alarm
                )
            } else null
        }.maxByOrNull {
            it.from
                .toInstant()
                .toEpochMilli()
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

        } else if (isValid()) {
            alarm.startDateTime!!.atZone(alarm.zoneId)
        } else null
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

    private fun meetsScheduledHours(localTime: LocalTime, startTime: LocalTime, endTime: LocalTime): Boolean {
        return if (startTime.isAfter(endTime)) {
            !localTime.isBefore(startTime) || localTime.isBefore(endTime)
        } else {
            !localTime.isBefore(startTime) && localTime.isBefore(endTime)
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

        while (true) {

            previousDay -= 1

            if (isDayInSchedule(alarm.scheduledDays, previousDay)) {
                if (alarm.startTime >= alarm.endTime) {
                    previousDay += 1
                }
                break
            }
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
        while (true) {
            nextDay += 1
            if (isDayInSchedule(alarm.scheduledDays, nextDay)) {
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

    companion object {

        fun meetsScheduledDate(now: LocalDateTime, startTime: LocalDateTime, endTime: LocalDateTime): Boolean {
            return if (startTime.isAfter(endTime)) {
                !now.isBefore(startTime) || now.isBefore(endTime)
            } else {
                !now.isBefore(startTime) && now.isBefore(endTime)
            }
        }
    }
}