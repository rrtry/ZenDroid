package com.example.volumeprofiler.core

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
        if (!isValid()) return null
        meetsSchedule = meetsSchedule()
        return if (meetsSchedule) getNextEndTime() else getNextStartTime()
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
            now.with(getDayOfWeekAdjuster(getNextWeekDay(alarm.startTime), now.toLocalTime() < alarm.startTime))
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

    private fun meetsDayOfWeek(): Boolean {
        return isDayInSchedule(alarm.scheduledDays, now.dayOfWeek) ||
                (isDayInSchedule(alarm.scheduledDays, now.dayOfWeek - 1) &&
                        alarm.startTime >= alarm.endTime)
    }

    private fun meetsScheduledHours(): Boolean {

        var inRange: Boolean = isDayInSchedule(alarm.scheduledDays, now.dayOfWeek)

        if (alarm.startTime >= alarm.endTime && now.toLocalTime() < alarm.startTime) {
            inRange = isDayInSchedule(alarm.scheduledDays, now.dayOfWeek - 1) &&
                    now.toLocalTime() < alarm.endTime
        }
        return meetsScheduledHours(now.toLocalTime(), alarm.startTime, alarm.endTime) && inRange
    }

    private fun meetsSchedule(): Boolean {
        if (isRecurring()) {
            return meetsDayOfWeek() && meetsScheduledHours()
        } else {
            now.toLocalDateTime().also {
                return !it.isBefore(alarm.startDateTime) &&
                        it.isBefore(alarm.endDateTime)
            }
        }
    }

    private fun getPreviousEndTime(): ZonedDateTime? {
        return if (isRecurring()) {

            val previousDay: DayOfWeek = getPreviousWeekDay(alarm.endTime)
            val dayOfWeekAdjuster: TemporalAdjuster = if (isToday(alarm.endTime)) {
                TemporalAdjusters.previousOrSame(previousDay)
            } else {
                TemporalAdjusters.previous(previousDay)
            }

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

    private fun isToday(alarmTime: LocalTime): Boolean {

        var today: Boolean = now.toLocalTime() >= alarm.endTime

        if (alarm.startTime >= alarmTime) {
            today = today && isDayInSchedule(alarm.scheduledDays, now.dayOfWeek - 1)
        }
        return today
    }

    private fun getPreviousWeekDay(alarmTime: LocalTime): DayOfWeek {

        var previousDay: DayOfWeek = now.dayOfWeek

        if (isDayInSchedule(alarm.scheduledDays, previousDay)) {
            if (isToday(alarmTime)) return previousDay else previousDay -= 1
        }
        while (!isDayInSchedule(alarm.scheduledDays, previousDay)) {
            previousDay -= 1
        }
        if (alarm.startTime >= alarm.endTime) previousDay += 1
        return previousDay
    }

    private fun getNextWeekDay(alarmTime: LocalTime): DayOfWeek {

        var nextDay: DayOfWeek = now.dayOfWeek

        if (isDayInSchedule(alarm.scheduledDays, nextDay)) {
            if (now.toLocalTime() < alarmTime) return nextDay else nextDay += 1
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

    fun getOngoingAlarm(alarms: List<AlarmRelation>?): OngoingAlarm? {
        if (alarms.isNullOrEmpty()) return null
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