package com.example.volumeprofiler.util.interruptionPolicy

import android.os.Build
import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*

fun interruptionPolicyAllowsNotificationStream(
    notificationInterruptionFilter: Int,
    notificationPriorityCategories: List<Int>,
    notificationAccessGranted: Boolean,
    streamsUnlinked: Boolean): Boolean {
    return if (!notificationAccessGranted) {
        true
    } else {
        val state: Boolean = when (notificationInterruptionFilter) {
            INTERRUPTION_FILTER_PRIORITY -> {
                if (Build.VERSION_CODES.R >= Build.VERSION.SDK_INT) {
                    notificationPriorityCategories.contains(PRIORITY_CATEGORY_MESSAGES) ||
                            notificationPriorityCategories.contains(PRIORITY_CATEGORY_REMINDERS) ||
                            notificationPriorityCategories.contains(PRIORITY_CATEGORY_EVENTS)
                } else {
                    notificationPriorityCategories.contains(PRIORITY_CATEGORY_MESSAGES) ||
                            notificationPriorityCategories.contains(PRIORITY_CATEGORY_REMINDERS) ||
                            notificationPriorityCategories.contains(PRIORITY_CATEGORY_EVENTS) ||
                            notificationPriorityCategories.contains(PRIORITY_CATEGORY_CONVERSATIONS)
                }
            }
            INTERRUPTION_FILTER_ALL -> true
            INTERRUPTION_FILTER_NONE, INTERRUPTION_FILTER_ALARMS -> false
            else -> false
        }
        if (streamsUnlinked) {
            state
        } else {
            false
        }
    }
}

fun interruptionPolicyAllowsRingerStream(
    interruptionFilter: Int,
    priorityCategories: List<Int>,
    notificationAccessGranted: Boolean,
    streamsUnlinked: Boolean): Boolean {
    return if (!notificationAccessGranted) {
        true
    } else {
        val state: Boolean = when (interruptionFilter) {
            INTERRUPTION_FILTER_PRIORITY -> priorityCategories.contains(PRIORITY_CATEGORY_CALLS) || priorityCategories.contains(
                PRIORITY_CATEGORY_REPEAT_CALLERS
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
                                         priorityCategories: List<Int>,
                                         notificationAccessGranted: Boolean): Boolean {
    return if (!notificationAccessGranted) {
        true
    } else {
        if (interruptionFilter == INTERRUPTION_FILTER_PRIORITY) {
            if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
                priorityCategories.contains(PRIORITY_CATEGORY_ALARMS)
            } else {
                true
            }
        } else interruptionFilter == INTERRUPTION_FILTER_ALL || interruptionFilter == INTERRUPTION_FILTER_ALARMS
    }
}

fun interruptionPolicyAllowsMediaStream(interruptionFilter: Int,
                                        priorityCategories: List<Int>,
                                        notificationAccessGranted: Boolean): Boolean {
    return if (!notificationAccessGranted) {
        true
    } else {
        if (interruptionFilter == INTERRUPTION_FILTER_PRIORITY) {
            if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
                priorityCategories.contains(PRIORITY_CATEGORY_MEDIA)
            } else {
                true
            }
        } else interruptionFilter == INTERRUPTION_FILTER_ALL || interruptionFilter == INTERRUPTION_FILTER_ALARMS
    }
}