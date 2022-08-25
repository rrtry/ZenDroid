package ru.rrtry.silentdroid.core

import ru.rrtry.silentdroid.entities.Alarm
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.entities.PreviousAndNextTrigger
import ru.rrtry.silentdroid.entities.Profile
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
        if (!isValid()) return null
        meetsSchedule = meetsSchedule()
        return if (meetsSchedule) getNextEndTime() else getNextStartTime()
    }

    private fun isRecurring(): Boolean {
        return alarm.scheduledDays != WeekDay.NONE
    }

    private fun isDayInSchedule(day: DayOfWeek): Boolean {
        return (alarm.scheduledDays and WeekDay.fromDay(day.value)) != WeekDay.NONE
    }

    fun isValid(): Boolean {
        return isValid(now, alarm)
    }

    fun getPreviousOccurrence(): ZonedDateTime? {
        meetsSchedule = meetsSchedule()
        return if (meetsSchedule) getPreviousStartTime() else getPreviousEndTime()
    }

    private fun getNextAlarmTime(alarms: List<AlarmRelation>): LocalDateTime? {
        return alarms
            .mapNotNull {
                alarm = it.alarm
                getNextOccurrence()
            }.minOrNull()?.toLocalDateTime()
    }

    private fun getNextStartTime(): ZonedDateTime {
        return if (isRecurring()) {
            now.with(getNextDayOfWeekAdjuster(getNextWeekDay(), now.toLocalTime() < alarm.startTime))
                .withHour(alarm.startTime.hour)
                .withMinute(alarm.startTime.minute)
                .withSecond(0)
                .withNano(0)
        } else {
            alarm.startDateTime!!.atZone(alarm.zoneId)
        }
    }

    private fun getPreviousStartTime(): ZonedDateTime? {
        return if (isRecurring()) {

            var startTime: ZonedDateTime = now
                .withHour(alarm.startTime.hour)
                .withMinute(alarm.startTime.minute)
                .withSecond(0)
                .withNano(0)

            if (alarm.startTime >= alarm.endTime &&
                now.toLocalTime() < alarm.startTime)
            {
                startTime = startTime.minusDays(1)
            }
            startTime
        } else {
            alarm.startDateTime!!.atZone(alarm.zoneId)
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

    private fun meetsSchedule(): Boolean {
        return if (isRecurring()) {
            if (alarm.startTime >= alarm.endTime) {
                (isDayInSchedule(now.dayOfWeek) && now.toLocalTime() >= alarm.startTime) ||
                        (isDayInSchedule(now.dayOfWeek - 1) && now.toLocalTime() < alarm.endTime)
            } else {
                isDayInSchedule(now.dayOfWeek) && meetsScheduledHours(
                    now.toLocalTime(),
                    alarm.startTime,
                    alarm.endTime
                )
            }
        } else {
            val now: LocalDateTime = now.toLocalDateTime()
            return !now.isBefore(alarm.startDateTime) &&
                    now.isBefore(alarm.endDateTime)
        }
    }

    private fun getPreviousEndTime(): ZonedDateTime? {
        return if (isRecurring()) {

            val isToday: Boolean = isPreviousInstanceToday()
            val previousDay: DayOfWeek = if (isToday) now.dayOfWeek else getPreviousWeekDay()

            now.with(getPreviousDayOfWeekAdjuster(previousDay, isToday))
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

    private fun isPreviousInstanceToday(): Boolean {
        val dayOfWeek: DayOfWeek = if (alarm.startTime >= alarm.endTime) {
            now.dayOfWeek - 1
        } else {
            now.dayOfWeek
        }
        return now.toLocalTime() >= alarm.endTime && isDayInSchedule(dayOfWeek)
    }

    private fun getPreviousWeekDay(): DayOfWeek {

        var previousDay: DayOfWeek = now.dayOfWeek

        if (isDayInSchedule(previousDay)) {
            previousDay -= 1
        }
        while (!isDayInSchedule(previousDay)) {
            previousDay -= 1
        }
        if (alarm.startTime >= alarm.endTime) previousDay += 1
        return previousDay
    }

    private fun getNextWeekDay(): DayOfWeek {

        var nextDay: DayOfWeek = now.dayOfWeek

        if (isDayInSchedule(nextDay)) {
            if (now.toLocalTime() < alarm.startTime) return nextDay else nextDay += 1
        }
        while (!isDayInSchedule(nextDay)) {
            nextDay += 1
        }
        return nextDay
    }

    private fun getPreviousDayOfWeekAdjuster(day: DayOfWeek, inclusive: Boolean): TemporalAdjuster {
        return if (inclusive) {
            TemporalAdjusters.previousOrSame(day)
        } else {
            TemporalAdjusters.previous(day)
        }
    }

    private fun getNextDayOfWeekAdjuster(day: DayOfWeek, inclusive: Boolean): TemporalAdjuster {
        return if (inclusive) {
            TemporalAdjusters.nextOrSame(day)
        } else {
            TemporalAdjusters.next(day)
        }
    }

    fun getPreviousAndNextTriggers(alarms: List<AlarmRelation>?): PreviousAndNextTrigger? {
        if (alarms.isNullOrEmpty()) return null
        val nextAlarmTime: LocalDateTime? = getNextAlarmTime(alarms)
        return try {
            alarms.map { relation ->

                alarm = relation.alarm

                val previousAlarmTime: LocalDateTime? = getPreviousOccurrence()?.toLocalDateTime()
                val hasPreviousInstance: Boolean = isRecurring() || previousAlarmTime != null
                val currentProfile: Profile? = if (hasPreviousInstance) {
                    if (meetsSchedule) relation.startProfile else relation.endProfile
                } else {
                    null
                }
                PreviousAndNextTrigger(
                    currentProfile,
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