package com.example.volumeprofiler.util

import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.FormatStyle

class TextUtil {

    companion object {

        fun formatRecurrenceRule(rrule: String): String {

            val argsMap: Map<String, String> = rruleToArgsMap(rrule)
            val results: List<String> = listOf(parseFrequencyRule(argsMap), parseWeekdays(argsMap), parseEndRule(argsMap))

            return results.joinToString(separator = ";")
        }

        private fun rruleToArgsMap(rrule: String): Map<String, String> {
            val argsMap: Map<String, String> = rrule.split(";").associate {
                val (rule, arg) = it.split("=")
                rule to arg
            }
            return argsMap
        }

        private fun parseEndDate(until: String): LocalDate {

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
                return "until ${parseEndDate(until).format(formatter)}"
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
                        "on every first ${dayOfWeekToString(weekdays.substring(1))}"
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

        @JvmStatic
        fun formatWeekDays(scheduledDays: Int): String {
            return when (scheduledDays) {
                WeekDay.ALL_DAYS -> "Every day"
                WeekDay.NONE -> "Not repeating"
                else -> {

                    val days: List<WeekDay> = WeekDay.values.filter { (scheduledDays and it.value) != 0 }
                    val displayStyle: TextStyle = if (days.size == 1) TextStyle.FULL else TextStyle.SHORT

                    days.joinToString(transform = {
                        DayOfWeek.of(it.num).getDisplayName(displayStyle, Locale.getDefault())
                    })
                }
            }
        }

        private fun getTimeFormat(context: Context): String {
            return (DateFormat.getTimeFormat(context) as SimpleDateFormat).toPattern()
        }

        @JvmStatic
        fun formatLocalTime(context: Context, localTime: LocalTime): String {
            DateTimeFormatter
                .ofPattern(getTimeFormat(context))
                .withZone(ZoneOffset.UTC).also {
                    return localTime.format(it)
                }
        }
    }
}