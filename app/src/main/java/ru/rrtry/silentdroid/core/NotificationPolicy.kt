package ru.rrtry.silentdroid.core

import android.os.Build
import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*
import android.media.AudioManager.RINGER_MODE_SILENT
import android.media.AudioManager.RINGER_MODE_VIBRATE

fun containsCategory(categories: Int, category: Int): Boolean {
    return (categories and category) != 0
}

fun getOtherInterruptionsList(mask: Int): List<Int> {
    val categories: MutableList<Int> = mutableListOf(
        PRIORITY_CATEGORY_EVENTS,
        PRIORITY_CATEGORY_REMINDERS,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        categories.addAll(
            arrayOf(
                PRIORITY_CATEGORY_SYSTEM,
                PRIORITY_CATEGORY_MEDIA,
                PRIORITY_CATEGORY_ALARMS
            )
        )
    }
    return categories.filter { category -> containsCategory(mask, category) }
}

fun ringerModeMutesNotifications(
    ringerMode: Int,
    hasSeparateNotificationStream: Boolean,
): Boolean {
    return hasSeparateNotificationStream &&
            (ringerMode == RINGER_MODE_SILENT ||
            ringerMode == RINGER_MODE_VIBRATE)
}

fun interruptionPolicyAllowsNotificationStream(
    notificationInterruptionFilter: Int,
    notificationPriorityCategories: Int,
    notificationAccessGranted: Boolean,
    streamsUnlinked: Boolean): Boolean
{
    if (!streamsUnlinked) return false
    if (notificationAccessGranted) {
        return interruptionPolicyAllowsRingerStream(
            notificationInterruptionFilter,
            notificationPriorityCategories,
            notificationAccessGranted,
            streamsUnlinked
        )
    }
    return true
}

fun interruptionPolicyAllowsRingerStream(
    interruptionFilter: Int,
    priorityCategories: Int,
    notificationAccessGranted: Boolean,
    streamsUnlinked: Boolean): Boolean
{
    if (!notificationAccessGranted) return true
    return when (interruptionFilter) {
        INTERRUPTION_FILTER_PRIORITY -> {
            val allowedCategories: MutableList<Int> = mutableListOf(
                PRIORITY_CATEGORY_CALLS,
                PRIORITY_CATEGORY_REPEAT_CALLERS,
                PRIORITY_CATEGORY_REMINDERS,
                PRIORITY_CATEGORY_EVENTS,
                PRIORITY_CATEGORY_MESSAGES
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                allowedCategories.add(PRIORITY_CATEGORY_SYSTEM)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                allowedCategories.add(PRIORITY_CATEGORY_CONVERSATIONS)
            }
            allowedCategories.any { category -> containsCategory(priorityCategories, category) }
        }
        INTERRUPTION_FILTER_ALL -> true
        else -> false
    }
}

fun interruptionPolicyAllowsAlarmsStream(interruptionFilter: Int,
                                         priorityCategories: Int,
                                         notificationAccessGranted: Boolean): Boolean {
    if (!notificationAccessGranted) return true
    return when (interruptionFilter) {
        INTERRUPTION_FILTER_PRIORITY -> {
            if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
                containsCategory(priorityCategories, PRIORITY_CATEGORY_ALARMS)
            } else {
                true
            }
        }
        INTERRUPTION_FILTER_ALL, INTERRUPTION_FILTER_ALARMS -> true
        else -> false
    }
}

fun interruptionPolicyAllowsMediaStream(interruptionFilter: Int,
                                        priorityCategories: Int,
                                        notificationAccessGranted: Boolean): Boolean {
    if (!notificationAccessGranted) return true
    return when (interruptionFilter) {
        INTERRUPTION_FILTER_PRIORITY -> {
            if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
                containsCategory(priorityCategories, PRIORITY_CATEGORY_MEDIA)
            } else {
                true
            }
        }
        INTERRUPTION_FILTER_ALL, INTERRUPTION_FILTER_ALARMS -> true
        else -> false
    }
}

fun canMuteAlarmStream(index: Int): Boolean {
    return when {
        index > 0 -> false
        Build.VERSION_CODES.P <= Build.VERSION.SDK_INT -> false
        else -> true
    }
}