package com.example.volumeprofiler.util.ui

import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.databinding.BindingConversion

object BindingConverters {

    @JvmStatic
    fun queryStarredContacts(context: Context): String {
        val projection: Array<String> = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            "${ContactsContract.Data.STARRED} = 1",
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )
        val stringBuilder: StringBuilder = StringBuilder()
        cursor?.use {
            if (it.count > 0) {
                while (it.moveToNext() && it.position <= 2) {
                    stringBuilder.append(it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)) + if (it.isLast) "and ${it.count} others" else ", ")
                }
            } else {
                stringBuilder.append("None")
            }
        }
        return stringBuilder.toString()
    }

    @JvmStatic
    fun getRingtoneTitle(context: Context, uri: Uri, type: Int): String {
        val actualUri: Uri = if (uri == Uri.EMPTY) RingtoneManager.getActualDefaultRingtoneUri(
            context,
            type
        ) else uri
        val contentResolver: ContentResolver = context.contentResolver
        val projection: Array<String> = arrayOf(MediaStore.MediaColumns.TITLE)
        var title: String = ""
        val cursor: Cursor? = contentResolver.query(actualUri, projection, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                title = cursor.getString(0)
                cursor.close()
            }
        }
        return title
    }

    @JvmStatic
    fun prioritySendersToString(prioritySenders: Int, priorityCategories: List<Int>, categoryType: Int): String {
        return if (categoryType == PRIORITY_CATEGORY_CALLS) {
            when (prioritySenders) {
                PRIORITY_SENDERS_ANY -> if (priorityCategories.contains(PRIORITY_CATEGORY_CALLS)) "From anyone" else "Don't allow any calls"
                PRIORITY_SENDERS_STARRED -> if (priorityCategories.contains(PRIORITY_CATEGORY_CALLS)) "From starred contacts only" else "Don't allow any calls"
                PRIORITY_SENDERS_CONTACTS -> if (priorityCategories.contains(PRIORITY_CATEGORY_CALLS)) "From contacts only" else "Don't allow any calls"
                else -> "Unknown"
            }
        } else {
            when (prioritySenders) {
                PRIORITY_SENDERS_ANY -> if (priorityCategories.contains(PRIORITY_CATEGORY_MESSAGES)) "From anyone" else "Don't allow any messages"
                PRIORITY_SENDERS_STARRED -> if (priorityCategories.contains(PRIORITY_CATEGORY_MESSAGES)) "From starred contacts only" else "Don't allow any messages"
                PRIORITY_SENDERS_CONTACTS -> if (priorityCategories.contains(PRIORITY_CATEGORY_MESSAGES)) "From contacts only" else "Don't allow any messages"
                else -> "Unknown"
            }
        }
    }

    @BindingConversion
    @JvmStatic
    fun suppressedEffectsToString(visualInterruptions: List<Int>): String {
        if (visualInterruptions.isEmpty()) {
            return "None of visual effects are suppressed"
        } else {
            val stringBuilder: StringBuilder = java.lang.StringBuilder()
            for ((index, i) in visualInterruptions.withIndex()) {
                when (i) {
                    SUPPRESSED_EFFECT_LIGHTS -> stringBuilder.append(if (index < visualInterruptions.size - 1) "Don't blink notification light, " else "Don't blink notification light")
                    SUPPRESSED_EFFECT_FULL_SCREEN_INTENT -> stringBuilder.append(if (index < visualInterruptions.size - 1) "Don't turn on the screen, " else "Don't turn on the screen")
                    SUPPRESSED_EFFECT_AMBIENT -> stringBuilder.append(if (index < visualInterruptions.size - 1) "Don't wake for notification, " else "Don't wake for notifications")
                    SUPPRESSED_EFFECT_BADGE -> stringBuilder.append(if (index < visualInterruptions.size - 1) "Hide notification dots on app icons, " else "Hide notifications dots on app icons")
                    SUPPRESSED_EFFECT_STATUS_BAR -> stringBuilder.append(if (index < visualInterruptions.size - 1) "Hide status bar icons at top of the screen, " else "Hide status bar icons at top of the screen")
                    SUPPRESSED_EFFECT_PEEK, SUPPRESSED_EFFECT_SCREEN_ON -> stringBuilder.append(if (index < visualInterruptions.size - 1) "Don't pop notifications on screen, " else "Don't pop notifications on screen")
                    SUPPRESSED_EFFECT_NOTIFICATION_LIST -> stringBuilder.append(if (index < visualInterruptions.size - 1) "Hide from notification list, " else "Hide from notification list")
                }
            }
            return stringBuilder.toString()
        }
    }

    @JvmStatic
    fun priorityCategoriesToString(categories: List<Int>): String {
        val otherInterruptions: List<Int> = listOf(
                PRIORITY_CATEGORY_REMINDERS,
                PRIORITY_CATEGORY_EVENTS,
                PRIORITY_CATEGORY_SYSTEM,
                PRIORITY_CATEGORY_ALARMS,
                PRIORITY_CATEGORY_MEDIA,
                PRIORITY_CATEGORY_CONVERSATIONS
        )
        val filteredCategories: List<Int> = categories.filter { otherInterruptions.contains(it) }
        if (filteredCategories.isEmpty()) {
            return "No interruptions are allowed"
        } else {
            val stringBuilder: StringBuilder = java.lang.StringBuilder()
            stringBuilder.append("Allow ")
            for ((index, i) in categories.withIndex()) {
                when (i) {
                    PRIORITY_CATEGORY_REMINDERS -> stringBuilder.append(if (index < categories.size - 1) "reminders, " else "reminders")
                    PRIORITY_CATEGORY_EVENTS -> stringBuilder.append(if (index < categories.size - 1) "events, " else "events")
                    PRIORITY_CATEGORY_SYSTEM -> stringBuilder.append(if (index < categories.size - 1) "touch sounds, " else "touch sounds")
                    PRIORITY_CATEGORY_ALARMS -> stringBuilder.append(if (index < categories.size - 1) "alarms, " else "alarms")
                    PRIORITY_CATEGORY_MEDIA -> stringBuilder.append(if (index < categories.size - 1) "media, " else "media")
                    PRIORITY_CATEGORY_CONVERSATIONS -> stringBuilder.append(if (index < categories.size - 1) "conversations, " else "conversations")
                }
            }
            return stringBuilder.toString()
        }
    }

    @JvmStatic
    fun interruptionRulesToString(notificationAccessGranted: Boolean): String {
        return if (!notificationAccessGranted) {
            "Notification policy access required"
        } else {
            "Priority interruption rules"
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
