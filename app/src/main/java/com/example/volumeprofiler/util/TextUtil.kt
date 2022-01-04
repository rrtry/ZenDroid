package com.example.volumeprofiler.util

import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlin.collections.ArrayList
import android.Manifest.permission.*
import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import com.example.volumeprofiler.entities.Event
import java.time.*
import java.time.format.FormatStyle

class TextUtil {

    companion object {

         fun formatRecurrenceRule(rrule: String): String {
            val argsMap: Map<String, String> = rruleToArgsMap(rrule)
            val stringBuilder: StringBuilder = StringBuilder()
            val results: Array<String> = arrayOf(parseFrequencyRule(argsMap), parseWeekdays(argsMap), parseEndRule(argsMap))
            for ((index, result) in results.withIndex()) {
                if (result.isNotEmpty()) {
                    stringBuilder.append(result)
                    if (index != results.size - 1) {
                        stringBuilder.append("; ")
                    }
                }
            }
            return stringBuilder.toString()
        }

        private fun rruleToArgsMap(rrule: String): Map<String, String> {
            Log.i("TextUtil", rrule)
            val argsMap: Map<String, String> = rrule.split(";").associate {
                val (rule, arg) = it.split("=")
                rule to arg
            }
            return argsMap
        }

        private fun untilToLocalDate(until: String): LocalDate {
            val regex: Regex = Regex("(\\d{4})(\\d{2})(\\d{2})")
            val groupValues: List<Int> = regex.findAll(until).first().groupValues.map { it.toInt() }
            return LocalDate.of(
                groupValues[1],
                groupValues[2],
                groupValues[3]
            )
        }

        private fun parseEndRule(args: Map<String, String>): String {
            val count: Int? = args["COUNT"]?.toInt()
            val until: String? = args["UNTIL"]
            if (count != null) {
                return "for $count times"
            }
            if (until != null) {
                val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault())
                return "until ${untilToLocalDate(until).format(formatter)}"
            }
            return ""
        }

        private fun dayOfWeekToString(day: String): String {
            return when (day) {
                "MO" -> "Monday"
                "TU" -> "Tuesday"
                "WE" -> "Wednesday"
                "TH" -> "Thursday"
                "FR" -> "Friday"
                "SA" -> "Saturday"
                "SU" -> "Sunday"
                else -> ""
            }
        }

        private fun parseWeekdays(args: Map<String, String>): String {
            val freq: String = args["FREQ"]!!
            return if (freq != "YEARLY" && freq != "DAILY") {
                val weekdays: String = args["BYDAY"]!!
                when (freq) {
                    "MONTHLY" -> {
                        val dayOfWeek: String = weekdays.substring(1)
                        "on every first ${dayOfWeekToString(dayOfWeek)}"
                    }
                    "WEEKLY" -> {
                        "on $weekdays"
                    }
                    else -> throw IllegalArgumentException("Invalid recurrence rule")
                }
            } else {
                ""
            }
        }

        private fun parseFrequencyRule(args: Map<String, String>): String {
            val freq: String = args["FREQ"]!!
            val interval: Int = args["INTERVAL"]?.toInt() ?: 1

            return when (freq) {
                "WEEKLY" -> if (interval > 1) "Repeats every $interval weeks" else "Repeats weekly"
                "MONTHLY" -> if (interval > 1) "Repeats every $interval months" else "Repeats monthly"
                "YEARLY" -> if (interval > 1) "Repeats every $interval years" else "Repeats yearly"
                "DAILY" -> if (interval > 1) "Repeats every $interval days" else "Repeats daily"
                else -> throw IllegalArgumentException("Invalid recurrence rule")
            }
        }

        fun formatEventTimestamp(context: Context, event: Event, millis: Long): String {
            val diff: Long = event.instanceEndTime - event.instanceBeginTime
            val diffDays: Long = ((diff / (1000 * 60 * 60 * 24)) % 365)
            val pattern: String = if (diffDays > 0) {
                "d MMM HH:mm a"
            } else {
                "HH:mm a"
            }
            if (DateFormat.is24HourFormat(context)) {
                pattern.replace("HH", "hh")
                pattern.replace("a", "")
            }
            val instant: Instant = Instant.ofEpochMilli(millis)
            val zonedDateTime: ZonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.systemDefault())
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
            return formatter.format(zonedDateTime)
        }

        fun getPermissionName(permission: String): String {
            return when (permission) {
                READ_PHONE_STATE -> "Phone"
                READ_EXTERNAL_STORAGE -> "Storage"
                ACCESS_FINE_LOCATION -> "Location"
                ACCESS_BACKGROUND_LOCATION -> "Background location"
                ACCESS_NOTIFICATION_POLICY -> "Do not disturb"
                else -> "System settings"
            }
        }

        @JvmStatic
        fun weekDaysToString(scheduledDays: ArrayList<Int>, startTime: LocalTime): String {
            val stringBuilder: StringBuilder = StringBuilder()
            if (scheduledDays.isNotEmpty()) {
                if (scheduledDays.size == 1) {
                    return DayOfWeek.of(scheduledDays[0]).getDisplayName(TextStyle.FULL, Locale.getDefault())
                }
                else if (scheduledDays.size == 7) {
                    return "Every day"
                }
                for ((index, i) in scheduledDays.withIndex()) {
                    if (index < scheduledDays.size - 1) {
                        stringBuilder.append(DayOfWeek.of(i).getDisplayName(TextStyle.SHORT, Locale.getDefault()) + ", ")
                    }
                    else {
                        stringBuilder.append(DayOfWeek.of(i).getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                    }
                }
                return stringBuilder.toString()
            }
            else {
                return if (startTime > LocalTime.now()) {
                    "Today"
                } else {
                    "Tomorrow"
                }
            }
        }

        @JvmStatic
        fun formatLocalTime(context: Context, time: LocalTime): String {
            val pattern: String = if (DateFormat.is24HourFormat(context)) {
                "HH:mm"
            } else {
                "hh:mm a"
            }
            val formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC)
            return time.format(formatter)
        }

        fun validateCoordinatesInput(source: CharSequence?): Boolean {
            return if (source != null && source.isNotEmpty()) {
                val double: Double? = source.toString().toDoubleOrNull()
                double != null
            } else {
                false
            }
        }
    }
}