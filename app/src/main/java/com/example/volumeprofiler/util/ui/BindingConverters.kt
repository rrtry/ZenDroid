package com.example.volumeprofiler.util.ui

import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*
import android.util.Log
import androidx.databinding.BindingConversion
import com.example.volumeprofiler.util.interruptionPolicy.extractPriorityCategories
import com.example.volumeprofiler.util.interruptionPolicy.maskContainsBit
import kotlin.text.StringBuilder

object BindingConverters {

    private fun priorityCategoryToString(category: Int): String {
        return when (category) {
            PRIORITY_CATEGORY_CALLS -> "calls"
            PRIORITY_CATEGORY_MESSAGES -> "messages"
            PRIORITY_CATEGORY_CONVERSATIONS -> "conversations"
            else -> throw IllegalArgumentException("Invalid priority category")
        }
    }

    @JvmStatic
    fun conversationSendersToString(senders: Int): String {
        return when (senders) {
            CONVERSATION_SENDERS_ANYONE -> "All conversations"
            CONVERSATION_SENDERS_IMPORTANT -> "Priority conversations"
            CONVERSATION_SENDERS_NONE -> "None"
            else -> throw IllegalArgumentException("Invalid interruption rule")
        }
    }

    @JvmStatic
    fun prioritySendersToString(prioritySenders: Int, priorityCategories: Int, categoryType: Int): String {
        return when (prioritySenders) {
            PRIORITY_SENDERS_ANY -> if (maskContainsBit(priorityCategories, categoryType)) "From anyone" else "Don't allow any ${priorityCategoryToString(categoryType)}"
            PRIORITY_SENDERS_STARRED -> if (maskContainsBit(priorityCategories, categoryType)) "From starred contacts only" else "Don't allow any ${priorityCategoryToString(categoryType)}"
            PRIORITY_SENDERS_CONTACTS -> if (maskContainsBit(priorityCategories, categoryType)) "From contacts only" else "Don't allow any ${priorityCategoryToString(categoryType)}"
            else -> "Unknown"
        }
    }

    @BindingConversion
    @JvmStatic
    fun suppressedEffectsToString(visualInterruptions: Int): String {
        return if (visualInterruptions == 0) {
            "None of visual effects are suppressed"
        } else {
            "Partially visible"
        }
    }

    @BindingConversion
    @JvmStatic
    fun priorityCategoriesToString(categories: Int): String {
        val categoriesList: List<Int> = extractPriorityCategories(categories)
        if (categoriesList.isNotEmpty()) {
            val stringBuilder: StringBuilder = StringBuilder()
            stringBuilder.append("Allow ")
            for ((index, i) in categoriesList.withIndex()) {
                when (i) {
                    PRIORITY_CATEGORY_REMINDERS -> stringBuilder.append(if (index < categoriesList.size - 1) "reminders, " else "reminders")
                    PRIORITY_CATEGORY_EVENTS -> stringBuilder.append(if (index < categoriesList.size - 1) "events, " else "events")
                    PRIORITY_CATEGORY_SYSTEM -> stringBuilder.append(if (index < categoriesList.size - 1) "touch sounds, " else "touch sounds")
                    PRIORITY_CATEGORY_ALARMS -> stringBuilder.append(if (index < categoriesList.size - 1) "alarms, " else "alarms")
                    PRIORITY_CATEGORY_MEDIA -> stringBuilder.append(if (index < categoriesList.size - 1) "media, " else "media")
                }
            }
            return stringBuilder.toString()
        } else {
            return "No interruptions are allowed"
        }
    }

    @BindingConversion
    @JvmStatic
    fun interruptionRulesToString(notificationAccessGranted: Boolean): String {
        return if (!notificationAccessGranted) {
            "Notification policy access required"
        } else {
            "Define your own do not disturb rules"
        }
    }

    @JvmStatic
    fun interruptionFilterToString(interruptionFilterMode: Int, policyAccess: Boolean): String {
        return if (policyAccess) {
            when (interruptionFilterMode) {
                INTERRUPTION_FILTER_PRIORITY -> "Priority only"
                INTERRUPTION_FILTER_ALARMS -> "Alarms only"
                INTERRUPTION_FILTER_NONE -> "Total silence"
                INTERRUPTION_FILTER_ALL -> "Allow everything"
                else -> "You've specified unknown interruption filter"
            }
        } else {
            "Notification policy access required"
        }
    }
}
