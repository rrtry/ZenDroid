package ru.example.silentdroid

import org.junit.Test
import org.junit.Assert.assertEquals

import ru.rrtry.silentdroid.core.ScheduleCalendar
import ru.rrtry.silentdroid.core.WeekDay
import ru.rrtry.silentdroid.entities.Alarm
import java.time.*
import java.util.*

/**
 * Scheduler unit test
 */
class SchedulerUnitTest {

    private var zdt: ZonedDateTime = ZonedDateTime.of(
        LocalDateTime.of(2022, Month.AUGUST, 1, 0, 0, 0, 0),
        ZoneId.systemDefault()
    )

    private val scheduleCalendar: ScheduleCalendar = ScheduleCalendar(zdt)

    private var alarm: Alarm = Alarm(
        0,
        "Test alarm",
        UUID.randomUUID(),
        UUID.randomUUID(),
        null,
        null,
        LocalTime.of(16, 0, 0, 0),
        LocalTime.of(18, 0, 0, 0),
        ZoneId.systemDefault(),
        true,
        WeekDay.MONDAY.value
    )

    @Test
    fun testGetNextOccurrenceOfFinishedNonRepeatingEvent() {
        scheduleCalendar.now = zdt.withDayOfMonth(2).withHour(18)
        scheduleCalendar.alarm = alarm.apply {
            scheduledDays = WeekDay.NONE
            startDateTime = zdt.toLocalDateTime()
                .withDayOfMonth(2)
                .withHour(16)
            endDateTime = zdt.toLocalDateTime()
                .withDayOfMonth(2)
                .withHour(18)
        }
        assertEquals(
            null,
            scheduleCalendar.getNextOccurrence()
        )
    }

    @Test
    fun testGetNextEndDateOfStartedNonRepeatingEvent() {
        scheduleCalendar.now = zdt.withDayOfMonth(2).withHour(17)
        scheduleCalendar.alarm = alarm.apply {
            scheduledDays = WeekDay.NONE
            startDateTime = zdt.toLocalDateTime()
                .withDayOfMonth(2)
                .withHour(16)
            endDateTime = zdt.toLocalDateTime()
                .withDayOfMonth(2)
                .withHour(18)
        }
        assertEquals(
            scheduleCalendar.alarm.endDateTime?.atZone(ZoneId.systemDefault()),
            scheduleCalendar.getNextOccurrence()
        )
    }

    @Test
    fun testGetNextStartDateOfNonRepeatingEvent() {
        scheduleCalendar.now = zdt
        scheduleCalendar.alarm = alarm.apply {
            scheduledDays = WeekDay.NONE
            startDateTime = zdt.toLocalDateTime()
                .withDayOfMonth(2)
                .withHour(16)
            endDateTime = zdt.toLocalDateTime()
                .withDayOfMonth(2)
                .withHour(18)
        }
        assertEquals(
            scheduleCalendar.alarm.startDateTime?.atZone(ZoneId.systemDefault()),
            scheduleCalendar.getNextOccurrence()
        )
    }

    @Test
    fun testGetPreviousOccurrenceOfStartedNonRepeatingEvent() {
        scheduleCalendar.now = zdt.withDayOfMonth(2).withHour(17)
        scheduleCalendar.alarm = alarm.apply {
            scheduledDays = WeekDay.NONE
            startDateTime = zdt.toLocalDateTime()
                .withDayOfMonth(2)
                .withHour(16)
            endDateTime = zdt.toLocalDateTime()
                .withDayOfMonth(2)
                .withHour(18)
        }
        assertEquals(
            scheduleCalendar.alarm.startDateTime?.atZone(ZoneId.systemDefault()),
            scheduleCalendar.getPreviousOccurrence()
        )
    }

    @Test
    fun testGetPreviousOccurrenceOfFinishedNonRepeatingEvent() {
        scheduleCalendar.now = zdt.withDayOfMonth(2).withHour(18)
        scheduleCalendar.alarm = alarm.apply {
            scheduledDays = WeekDay.NONE
            startDateTime = zdt.toLocalDateTime()
                .withDayOfMonth(2)
                .withHour(16)
            endDateTime = zdt.toLocalDateTime()
                .withDayOfMonth(2)
                .withHour(18)
        }
        assertEquals(
            scheduleCalendar.alarm.endDateTime?.atZone(ZoneId.systemDefault()),
            scheduleCalendar.getPreviousOccurrence()
        )
    }

    @Test
    fun testGetPreviousOccurrenceOfNewNonRepeatingEvent() {
        scheduleCalendar.now = zdt
        scheduleCalendar.alarm = alarm.apply {
            scheduledDays = WeekDay.NONE
            startDateTime = zdt.toLocalDateTime()
                .withDayOfMonth(2)
                .withHour(16)
            endDateTime = zdt.toLocalDateTime()
                .withDayOfMonth(2)
                .withHour(18)
        }
        assertEquals(
            null,
            scheduleCalendar.getPreviousOccurrence()
        )
    }

    @Test
    fun testGetEventPreviousEndTimeOvernight() {
        zdt = zdt.withDayOfMonth(2).withHour(6)
        scheduleCalendar.now = zdt
        scheduleCalendar.alarm = alarm.apply {
            startTime = LocalTime.of(22, 0, 0, 0)
            endTime = LocalTime.of(6, 0, 0, 0)
        }
        assertEquals(
            zdt.withHour(6),
            scheduleCalendar.getPreviousOccurrence()
        )
    }

    @Test
    fun testGetEventPreviousStartTimeOvernightInSchedule() {
        zdt = zdt.withDayOfMonth(2).withHour(5)
        scheduleCalendar.now = zdt
        scheduleCalendar.alarm = alarm.apply {
            startTime = LocalTime.of(22, 0, 0, 0)
            endTime = LocalTime.of(6, 0, 0, 0)
        }
        assertEquals(
            zdt.withHour(22).minusDays(1),
            scheduleCalendar.getPreviousOccurrence()
        )
    }

    @Test
    fun testGetEventNextEndTimeOvernightInSchedule() {
        zdt = zdt.withDayOfMonth(2).withHour(2)
        scheduleCalendar.now = zdt
        scheduleCalendar.alarm = alarm.apply {
            startTime = LocalTime.of(22, 0, 0, 0)
            endTime = LocalTime.of(6, 0, 0, 0)
        }
        assertEquals(
            zdt.withHour(6),
            scheduleCalendar.getNextOccurrence()
        )
    }

    @Test
    fun testGetEventNextEndTimeOvernight() {
        zdt = zdt.withHour(22)
        scheduleCalendar.now = zdt
        scheduleCalendar.alarm = alarm.apply {
            startTime = LocalTime.of(22, 0, 0, 0)
            endTime = LocalTime.of(6, 0, 0, 0)
        }
        assertEquals(
            zdt.withHour(6).plusDays(1),
            scheduleCalendar.getNextOccurrence()
        )
    }

    @Test
    fun testGetEventPreviousEndTimeOvernightPreviousWeek() {
        zdt = zdt.withHour(12)
        scheduleCalendar.now = zdt
        scheduleCalendar.alarm = alarm.apply {
            startTime = LocalTime.of(18, 0, 0, 0)
            endTime = LocalTime.of(16, 0, 0, 0)
        }
        assertEquals(
            zdt.withMonth(Month.JULY.value)
                .withDayOfMonth(26)
                .withHour(16),
            scheduleCalendar.getPreviousOccurrence()
        )
    }

    @Test
    fun testGetEventPreviousEndTimePreviousWeek() {
        zdt = zdt.withHour(12)
        scheduleCalendar.now = zdt
        scheduleCalendar.alarm = alarm
        assertEquals(
            zdt.withMonth(Month.JULY.value)
                .withDayOfMonth(25)
                .withHour(18),
            scheduleCalendar.getPreviousOccurrence()
        )
    }

    @Test
    fun testGetEventPreviousEndTimeToday() {
        zdt = zdt.withHour(18)
        scheduleCalendar.now = zdt
        scheduleCalendar.alarm = alarm
        assertEquals(
            zdt.withHour(18),
            scheduleCalendar.getPreviousOccurrence()
        )
    }

    @Test
    fun testGetEventNextStartTimeNextWeek() {
        zdt = zdt.withHour(18)
        scheduleCalendar.now = zdt
        scheduleCalendar.alarm = alarm
        assertEquals(
            zdt.withDayOfMonth(8).withHour(16),
            scheduleCalendar.getNextOccurrence()
        )
    }

    @Test
    fun testGetEventNextStartTime() {
        zdt = zdt.withHour(12)
        scheduleCalendar.now = zdt
        scheduleCalendar.alarm = alarm
        assertEquals(zdt.withHour(16), scheduleCalendar.getNextOccurrence())
    }

    @Test
    fun testGetEventNextEndTime() {
        zdt = zdt.withHour(16)
        scheduleCalendar.now = zdt
        scheduleCalendar.alarm = alarm
        assertEquals(zdt.withHour(18), scheduleCalendar.getNextOccurrence())
    }
}