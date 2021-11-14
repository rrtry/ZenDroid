package com.example.volumeprofiler.util.interruptionPolicy

import android.os.Build
import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*

fun isNotificationStreamActive(notificationInterruptionFilter: Int, notificationPriorityCategories: List<Int>, notificationAccessGranted: Boolean): Boolean {
    return if (!notificationAccessGranted) {
        true
    } else {
        when (notificationInterruptionFilter) {
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
    }
}

fun isRingerStreamActive(interruptionFilter: Int, priorityCategories: List<Int>, notificationAccessGranted: Boolean, streamsUnlinked: Boolean): Boolean {
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
            state || isNotificationStreamActive(interruptionFilter, priorityCategories, notificationAccessGranted)
        }
    }
}

fun isAlarmStreamActive(interruptionFilter: Int, priorityCategories: List<Int>, notificationAccessGranted: Boolean): Boolean {
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

fun isVoiceCallStreamActive(interruptionFilter: Int): Boolean {
    return interruptionFilter != INTERRUPTION_FILTER_NONE
}

fun isMediaStreamActive(interruptionFilter: Int, priorityCategories: List<Int>, notificationAccessGranted: Boolean): Boolean {
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