package com.example.volumeprofiler.util.interruptionPolicy

import android.annotation.TargetApi
import android.os.Build
import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*

const val MODE_SCREEN_OFF: Int = 0
const val MODE_SCREEN_ON: Int = 1

private fun maskContainsBit(mask: Int, bit: Int): Boolean {
    return (mask and bit) != 0
}

fun notificationsVisible(mask: Int, screenMode: Int): Boolean {
    return when (screenMode) {
        MODE_SCREEN_ON -> maskContainsBit(mask, SUPPRESSED_EFFECT_SCREEN_ON)
        MODE_SCREEN_OFF -> maskContainsBit(mask, SUPPRESSED_EFFECT_SCREEN_OFF)
        else -> false
    }
}

@TargetApi(Build.VERSION_CODES.R)
fun extractConversationSenders(mask: Int): Int {
    return when {
        maskContainsBit(mask, CONVERSATION_SENDERS_ANYONE) -> {
            CONVERSATION_SENDERS_ANYONE
        }
        maskContainsBit(mask, CONVERSATION_SENDERS_IMPORTANT) -> {
            CONVERSATION_SENDERS_IMPORTANT
        }
        maskContainsBit(mask, CONVERSATION_SENDERS_NONE) -> {
            CONVERSATION_SENDERS_NONE
        }
        else -> -1
    }
}

@TargetApi(Build.VERSION_CODES.P)
fun extractSuppressedEffects(mask: Int, mode: Int): List<Int> {
    val visualEffects: List<Int> = if (mode == MODE_SCREEN_ON) {
        listOf(
            SUPPRESSED_EFFECT_BADGE,
            SUPPRESSED_EFFECT_STATUS_BAR,
            SUPPRESSED_EFFECT_PEEK,
            SUPPRESSED_EFFECT_NOTIFICATION_LIST,
            SUPPRESSED_EFFECT_SCREEN_ON
        )
    } else {
         listOf(
            SUPPRESSED_EFFECT_LIGHTS,
            SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
            SUPPRESSED_EFFECT_AMBIENT
        )
    }
    return visualEffects.filter { effect -> maskContainsBit(mask, effect) }
}

fun extractPrioritySenders(mask: Int): Int {
    return when {
        maskContainsBit(mask, PRIORITY_SENDERS_ANY) -> {
            PRIORITY_SENDERS_ANY
        }
        maskContainsBit(mask, PRIORITY_SENDERS_CONTACTS) -> {
            PRIORITY_SENDERS_CONTACTS
        }
        maskContainsBit(mask, PRIORITY_SENDERS_STARRED) -> {
            PRIORITY_SENDERS_STARRED
        }
        else -> -1
    }
}

fun extractPriorityCategories(mask: Int): List<Int> {
    val categories: MutableList<Int> = mutableListOf(
        PRIORITY_CATEGORY_CALLS,
        PRIORITY_CATEGORY_EVENTS,
        PRIORITY_CATEGORY_MESSAGES,
        PRIORITY_CATEGORY_REMINDERS,
        PRIORITY_CATEGORY_REPEAT_CALLERS,
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

fun interruptionFilterAllowsNotifications(notificationInterruptionFilter: Int, notificationPriorityCategories: List<Int>): Boolean {
    return when (notificationInterruptionFilter) {
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

fun interruptionPolicyAllowsNotificationStream(
    notificationInterruptionFilter: Int,
    notificationPriorityCategories: List<Int>,
    notificationAccessGranted: Boolean): Boolean {
    return if (!notificationAccessGranted) {
        true
    } else {
        return interruptionFilterAllowsNotifications(notificationInterruptionFilter, notificationPriorityCategories)
    }
}

fun interruptionPolicyAllowsNotificationStream(
    notificationInterruptionFilter: Int,
    notificationPriorityCategories: List<Int>,
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