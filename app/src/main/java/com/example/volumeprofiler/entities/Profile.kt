package com.example.volumeprofiler.entities

import android.net.Uri
import androidx.room.PrimaryKey
import java.util.UUID
import android.app.NotificationManager.Policy.*
import android.media.AudioManager
import android.app.NotificationManager.*
import android.os.Parcelable
import kotlinx.parcelize.*
import android.media.AudioManager.*
@Parcelize
@androidx.room.Entity
data class Profile(var title: String,

                   @PrimaryKey val id: UUID = UUID.randomUUID(),

                   var mediaVolume: Int = STREAM_MUSIC_DEFAULT_VOLUME,
                   var callVolume: Int = STREAM_VOICE_CALL_DEFAULT_VOLUME,
                   var notificationVolume: Int = STREAM_NOTIFICATION_DEFAULT_VOLUME,
                   var ringVolume: Int = STREAM_RING_DEFAULT_VOLUME,
                   var alarmVolume: Int = STREAM_ALARM_DEFAULT_VOLUME,

                   var phoneRingtoneUri: Uri = Uri.EMPTY,
                   var notificationSoundUri: Uri = Uri.EMPTY,
                   var alarmSoundUri: Uri = Uri.EMPTY,

                   var streamsUnlinked: Boolean = false,

                   var interruptionFilter: Int = INTERRUPTION_FILTER_PRIORITY,
                   var ringerMode: Int = RINGER_MODE_NORMAL,
                   var notificationMode: Int = RINGER_MODE_NORMAL,
                   var isVibrateForCallsActive: Int = 0,

                   var priorityCategories: List<Int> = listOf(PRIORITY_CATEGORY_CALLS, PRIORITY_CATEGORY_MESSAGES, PRIORITY_CATEGORY_REPEAT_CALLERS),
                   var priorityCallSenders: Int = PRIORITY_SENDERS_ANY,
                   var priorityMessageSenders: Int = PRIORITY_SENDERS_ANY,
                   var screenOnVisualEffects: List<Int> = listOf(),
                   var screenOffVisualEffects: List<Int> = listOf(),
                   var primaryConversationSenders: Int = CONVERSATION_SENDERS_ANYONE): Parcelable {

    override fun toString(): String {
        return title
    }

    companion object {

        const val STREAM_MUSIC_DEFAULT_VOLUME: Int = 5
        const val STREAM_VOICE_CALL_DEFAULT_VOLUME: Int = 4
        const val STREAM_NOTIFICATION_DEFAULT_VOLUME: Int = 5
        const val STREAM_RING_DEFAULT_VOLUME: Int = 5
        const val STREAM_ALARM_DEFAULT_VOLUME: Int = 3
    }
}