package com.example.volumeprofiler.util

import android.Manifest
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*
import kotlin.collections.ArrayList

class TextUtil {

    companion object {

        fun getPermissionName(permission: String): String {
            return when (permission) {
                Manifest.permission.READ_PHONE_STATE -> "Phone"
                Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage"
                Manifest.permission.ACCESS_FINE_LOCATION -> "Location"
                else -> ""
            }
        }

        @JvmStatic
        fun weekDaysToString(scheduledDays: ArrayList<Int>, startTime: LocalDateTime): String {
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
                return if (startTime.toLocalTime() > LocalTime.now()) {
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