package ru.rrtry.silentdroid.ui

import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*
import android.content.Context
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.core.containsCategory
import ru.rrtry.silentdroid.core.getPriorityCategoriesList

object BindingConverters {

    private fun priorityCategoryToString(context: Context, category: Int): String {
        return when (category) {
            PRIORITY_CATEGORY_CALLS -> context.resources.getString(R.string.priority_category_calls)
            PRIORITY_CATEGORY_MESSAGES -> context.resources.getString(R.string.priority_category_messages)
            PRIORITY_CATEGORY_CONVERSATIONS -> context.resources.getString(R.string.priority_category_conversations)
            else -> throw IllegalArgumentException("Invalid priority category")
        }
    }

    @JvmStatic
    fun interruptionFilterToString(context: Context, interruptionFilter: Int): String {
        return when (interruptionFilter) {
            INTERRUPTION_FILTER_ALL -> context.resources.getString(R.string.dnd_allow_all_description)
            INTERRUPTION_FILTER_PRIORITY -> context.resources.getString(R.string.dnd_priority_only_description)
            INTERRUPTION_FILTER_ALARMS -> context.resources.getString(R.string.dnd_alarms_only_description)
            INTERRUPTION_FILTER_NONE -> context.resources.getString(R.string.dnd_total_silence_description)
            else -> throw IllegalArgumentException("Unknown interruption filter")
        }
    }

    @JvmStatic
    fun conversationSendersToString(context: Context, senders: Int): String {
        return when (senders) {
            CONVERSATION_SENDERS_ANYONE -> context.resources.getString(R.string.conversation_senders_anyone)
            CONVERSATION_SENDERS_IMPORTANT -> context.resources.getString(R.string.conversation_senders_important)
            CONVERSATION_SENDERS_NONE -> context.resources.getString(R.string.conversation_senders_none)
            else -> throw IllegalArgumentException("Invalid sender type")
        }
    }

    @JvmStatic
    fun prioritySendersToString(context: Context, prioritySenders: Int, priorityCategories: Int, categoryType: Int): String {
        return if (!containsCategory(priorityCategories, categoryType)) {
            context.resources.getString(R.string.disallow) + " " + priorityCategoryToString(context, categoryType)
        } else {
            when (prioritySenders) {
                PRIORITY_SENDERS_ANY -> context.resources.getString(R.string.message_senders_anyone)
                PRIORITY_SENDERS_STARRED -> context.resources.getString(R.string.message_senders_starred)
                PRIORITY_SENDERS_CONTACTS -> context.resources.getString(R.string.message_senders_contacts)
                else -> throw IllegalArgumentException("Invalid sender type")
            }
        }
    }

    @JvmStatic
    fun priorityCategoriesToString(context: Context, categories: Int): String {
        val categoriesList: List<Int> = getPriorityCategoriesList(categories).sorted()
        if (categoriesList.isEmpty()) return context.resources.getString(R.string.no_exceptions)
        return context.resources.getString(R.string.allow) + " " + categoriesList.joinToString(separator = ", ", transform = {
            when (it) {
                PRIORITY_CATEGORY_REMINDERS -> context.resources.getString(R.string.priority_category_reminders)
                PRIORITY_CATEGORY_EVENTS -> context.resources.getString(R.string.priority_category_events)
                PRIORITY_CATEGORY_SYSTEM -> context.resources.getString(R.string.priority_category_system)
                PRIORITY_CATEGORY_ALARMS -> context.resources.getString(R.string.priority_category_alarms)
                PRIORITY_CATEGORY_MEDIA -> context.resources.getString(R.string.priority_category_media)
                else -> throw IllegalArgumentException("Invalid priority category")
            }
        })
    }

    @JvmStatic
    fun interruptionRulesToString(context: Context, notificationAccessGranted: Boolean): String {
        return if (!notificationAccessGranted) {
            context.resources.getString(R.string.notification_policy_access_revoked)
        } else {
            context.resources.getString(R.string.notification_policy_access_granted)
        }
    }

    @JvmStatic
    fun interruptionFilterToString(context: Context, interruptionFilterMode: Int, policyAccess: Boolean): String {
        return if (policyAccess) {
            when (interruptionFilterMode) {
                INTERRUPTION_FILTER_PRIORITY -> context.resources.getString(R.string.dnd_priority_ony)
                INTERRUPTION_FILTER_ALARMS -> context.resources.getString(R.string.dnd_alarms_only)
                INTERRUPTION_FILTER_NONE -> context.resources.getString(R.string.dnd_total_silence)
                INTERRUPTION_FILTER_ALL -> context.resources.getString(R.string.dnd_allow_all)
                else -> throw IllegalArgumentException("Invalid interruption filter")
            }
        } else {
            context.resources.getString(R.string.notification_policy_access_revoked)
        }
    }
}
