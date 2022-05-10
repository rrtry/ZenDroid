package com.example.volumeprofiler.util.ui

import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*
import androidx.databinding.BindingConversion
import com.example.volumeprofiler.util.interruptionPolicy.*

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
            PRIORITY_SENDERS_ANY -> if (containsCategory(priorityCategories, categoryType)) "From anyone" else "Don't allow any ${priorityCategoryToString(categoryType)}"
            PRIORITY_SENDERS_STARRED -> if (containsCategory(priorityCategories, categoryType)) "From starred contacts only" else "Don't allow any ${priorityCategoryToString(categoryType)}"
            PRIORITY_SENDERS_CONTACTS -> if (containsCategory(priorityCategories, categoryType)) "From contacts only" else "Don't allow any ${priorityCategoryToString(categoryType)}"
            else -> throw IllegalArgumentException("Invalid sender type")
        }
    }

    @BindingConversion
    @JvmStatic
    fun priorityCategoriesToString(categories: Int): String {
        val categoriesList: List<Int> = extractPriorityCategories(categories)
        if (categoriesList.isEmpty()) {
            return "No exceptions"
        }
        return "Allow " + categoriesList.joinToString(separator = ", ", transform = {
            when (it) {
                PRIORITY_CATEGORY_REMINDERS -> "reminders"
                PRIORITY_CATEGORY_EVENTS -> "events"
                PRIORITY_CATEGORY_SYSTEM -> "touch sounds"
                PRIORITY_CATEGORY_ALARMS -> "alarms"
                PRIORITY_CATEGORY_MEDIA -> "media"
                else -> throw IllegalArgumentException("Invalid priority category")
            }
        })
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
                else -> throw IllegalArgumentException("Invalid interruption filter")
            }
        } else "Notification policy access required"
    }
}
