package com.example.volumeprofiler.util.interruptionPolicy

import android.os.Build
import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*

const val ALL_SCREEN_OFF_EFFECTS: Int = 0x8c
const val ALL_SCREEN_ON_EFFECTS: Int = 0xAA
const val ALL_VISUAL_EFFECTS_SUPPRESSED: Int = 0xAE

fun isBitSet(mask: Int, bit: Int): Boolean {
    return (mask and bit) != 0
}

fun createMask(list: List<Int>): Int {
    var mask: Int = 0
    for (i in list) {
        mask = mask or i
    }
    return mask
}

fun extractPriorityCategories(mask: Int): List<Int> {
    var categories: Array<Int> = arrayOf(
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
    return categories.filter { category -> isBitSet(mask, category) }
}

fun interruptionFilterAllowsNotifications(notificationInterruptionFilter: Int, notificationPriorityCategories: Int): Boolean {
    return when (notificationInterruptionFilter) {
        INTERRUPTION_FILTER_PRIORITY -> {
            if (Build.VERSION_CODES.R >= Build.VERSION.SDK_INT) {
                isBitSet(notificationPriorityCategories, PRIORITY_CATEGORY_MESSAGES) ||
                        isBitSet(notificationPriorityCategories, PRIORITY_CATEGORY_REMINDERS) ||
                        isBitSet(notificationPriorityCategories, PRIORITY_CATEGORY_EVENTS)
            } else {
                isBitSet(notificationPriorityCategories, PRIORITY_CATEGORY_MESSAGES) ||
                        isBitSet(notificationPriorityCategories, PRIORITY_CATEGORY_REMINDERS) ||
                        isBitSet(notificationPriorityCategories, PRIORITY_CATEGORY_EVENTS) ||
                        isBitSet(notificationPriorityCategories, PRIORITY_CATEGORY_CONVERSATIONS)
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
            INTERRUPTION_FILTER_PRIORITY -> isBitSet(priorityCategories, PRIORITY_CATEGORY_CALLS) || isBitSet(
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
                isBitSet(priorityCategories, PRIORITY_CATEGORY_ALARMS)
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
                isBitSet(priorityCategories, PRIORITY_CATEGORY_MEDIA)
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