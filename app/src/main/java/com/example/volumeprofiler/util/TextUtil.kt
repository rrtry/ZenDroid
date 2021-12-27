package com.example.volumeprofiler.util

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*
import kotlin.collections.ArrayList
import android.Manifest.permission.*
import android.content.Context
import android.text.format.DateFormat
import com.example.volumeprofiler.R
import com.example.volumeprofiler.entities.Event
import com.google.android.gms.maps.model.LatLng
import java.time.*
import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalAdjusters

class TextUtil {

    companion object {

        fun formatEventTimestamp(context: Context, event: Event, millis: Long): String {
            val diff: Long = event.currentInstanceEndTime - event.currentInstanceStartTime
            val diffDays: Long = ((diff / (1000 * 60 * 60 * 24)) % 365)
            val pattern: String = if (diffDays > 0) {
                "d MMM HH:mm a"
            } else {
                "HH:mm a"
            }
            if (DateFormat.is24HourFormat(context)) {
                pattern.replace("HH", "hh")
            }
            val instant: Instant = Instant.ofEpochMilli(millis)
            val zonedDateTime: ZonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.systemDefault())
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
            return formatter.format(zonedDateTime)
        }

        fun formatAddress(string: String): String {
            val s: List<String> = string.split(",")
            return "${s[0]}, ${s[1]}"
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
            val stringBuilder: java.lang.StringBuilder = java.lang.StringBuilder()
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
        fun localizedTimeToString(time: LocalTime): String {
            val pattern = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
            return time.format(pattern)
        }

        fun validateCoordinatesInput(source: CharSequence?): Boolean {
            return if (source != null && source.isNotEmpty()) {
                val double: Double? = source.toString().toDoubleOrNull()
                double != null
            } else {
                false
            }
        }

        fun filterCoordinatesInput(source: CharSequence?): CharSequence {
            return if (source != null) {
                val stringBuilder: StringBuilder = StringBuilder()
                for (i in source) {
                    if (Character.isDigit(i) || i == '.' || i == '-') {
                        stringBuilder.append(i)
                    }
                }
                stringBuilder.toString()
            } else {
                ""
            }
        }
    }
}