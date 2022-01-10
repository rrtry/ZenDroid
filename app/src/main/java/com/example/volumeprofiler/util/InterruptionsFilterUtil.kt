package com.example.volumeprofiler.util.interruptionPolicy

import android.os.Build
import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*

fun maskContainsBit(mask: Int, bit: Int): Boolean {
    return (mask and bit) != 0
}

fun extractPriorityCategories(mask: Int): List<Int> {
    val categories: MutableList<Int> = mutableListOf(
        PRIORITY_CATEGORY_EVENTS,
        PRIORITY_CATEGORY_REMINDERS,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        categories += PRIORITY_CATEGORY_CONVERSATIONS
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        categories += arrayOf(
            PRIORITY_CATEGORY_SYSTEM,
            PRIORITY_CATEGORY_MEDIA,
            PRIORITY_CATEGORY_ALARMS
        )
    }
    return categories.filter { category -> maskContainsBit(mask, category) }
}

fun interruptionFilterAllowsNotifications(notificationInterruptionFilter: Int, notificationPriorityCategories: Int): Boolean {
    return when (notificationInterruptionFilter) {
        INTERRUPTION_FILTER_PRIORITY -> {
            if (Build.VERSION_CODES.R >= Build.VERSION.SDK_INT) {
                maskContainsBit(notificationPriorityCategories, PRIORITY_CATEGORY_MESSAGES) ||
                        maskContainsBit(notificationPriorityCategories, PRIORITY_CATEGORY_REMINDERS) ||
                        maskContainsBit(notificationPriorityCategories, PRIORITY_CATEGORY_EVENTS)
            } else {
                maskContainsBit(notificationPriorityCategories, PRIORITY_CATEGORY_MESSAGES) ||
                        maskContainsBit(notificationPriorityCategories, PRIORITY_CATEGORY_REMINDERS) ||
                        maskContainsBit(notificationPriorityCategories, PRIORITY_CATEGORY_EVENTS) ||
                        maskContainsBit(notificationPriorityCategories, PRIORITY_CATEGORY_CONVERSATIONS)
            }
        }
        INTERRUPTION_FILTER_ALL -> true
        INTERRUPTION_FILTER_NONE, INTERRUPTION_FILTER_ALARMS -> false
        else -> false
    }
}

fun interruptionPolicyAllowsNotificationStream(
    notificationInterruptionFilter: Int,
    notificationPriorityCategories: Int,
    notificationAccessGranted: Boolean): Boolean {
    return if (!notificationAccessGranted) {
        true
    } else {
        return interruptionFilterAllowsNotifications(notificationInterruptionFilter, notificationPriorityCategories)
    }
}

fun interruptionPolicyAllowsNotificationStream(
    notificationInterruptionFilter: Int,
    notificationPriorityCategories: Int,
    notificationAccessGranted: Boolean,
    streamsUnlinked: Boolean): Boolean {

    val state: Boolean = interruptionFilterAllowsNotifications(notificationInterruptionFilter, notificationPriorityCategories)

    return if (!streamsUnlinked) {
        false
    } else if (notificationAccessGranted) {
        state
    } else {
        true
    }
}

fun interruptionPolicyAllowsRingerStream(
    interruptionFilter: Int,
    priorityCategories: Int,
    notificationAccessGranted: Boolean,
    streamsUnlinked: Boolean): Boolean {
    return if (!notificationAccessGranted) {
        true
    } else {
        val state: Boolean = when (interruptionFilter) {
            INTERRUPTION_FILTER_PRIORITY -> maskContainsBit(priorityCategories, PRIORITY_CATEGORY_CALLS) || maskContainsBit(
                priorityCategories, PRIORITY_CATEGORY_REPEAT_CALLERS
            )
            INTERRUPTION_FILTER_ALL -> true
            INTERRUPTION_FILTER_NONE, INTERRUPTION_FILTER_ALARMS -> false
            else -> false
        }
        if (streamsUnlinked) {
            state
        }
        else {
            state || interruptionPolicyAllowsNotificationStream(interruptionFilter, priorityCategories, notificationAccessGranted)
        }
    }
}

fun interruptionPolicyAllowsAlarmsStream(interruptionFilter: Int,
                                         priorityCategories: Int,
                                         notificationAccessGranted: Boolean): Boolean {
    return if (!notificationAccessGranted) {
        true
    } else {
        if (interruptionFilter == INTERRUPTION_FILTER_PRIORITY) {
            if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
                maskContainsBit(priorityCategories, PRIORITY_CATEGORY_ALARMS)
            } else {
                true
            }
        } else interruptionFilter == INTERRUPTION_FILTER_ALL || interruptionFilter == INTERRUPTION_FILTER_ALARMS
    }
}

fun interruptionPolicyAllowsMediaStream(interruptionFilter: Int,
                                        priorityCategories: Int,
                                        notificationAccessGranted: Boolean): Boolean {
    return if (!notificationAccessGranted) {
        true
    } else {
        if (interruptionFilter == INTERRUPTION_FILTER_PRIORITY) {
            if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
                maskContainsBit(priorityCategories, PRIORITY_CATEGORY_MEDIA)
            } else {
                true
            }
        } else interruptionFilter == INTERRUPTION_FILTER_ALL || interruptionFilter == INTERRUPTION_FILTER_ALARMS
    }
}

fun canMuteAlarmStream(index: Int): Boolean {
    return when {
        index > 0 -> {
            false
        }
        Build.VERSION_CODES.P <= Build.VERSION.SDK_INT -> {
            false
        }
        else -> {
            true
        }
    }
}