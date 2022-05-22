package com.example.volumeprofiler.entities

import android.net.Uri
import androidx.room.PrimaryKey
import java.util.UUID
import android.os.Parcelable
import kotlinx.parcelize.*
import android.media.AudioManager.*
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import com.example.volumeprofiler.R
import com.google.gson.annotations.Expose

@Parcelize
@androidx.room.Entity
data class Profile(

    @PrimaryKey
    @Expose
    var id: UUID = UUID.randomUUID(),

    @Expose var title: String,
    @Expose var iconRes: Int,

    @Expose var mediaVolume: Int = 5,
    @Expose var callVolume: Int = 4,
    @Expose var notificationVolume: Int = 5,
    @Expose var ringVolume: Int = 5,
    @Expose var alarmVolume: Int = 3,

    var phoneRingtoneUri: Uri = Uri.EMPTY,
    var notificationSoundUri: Uri = Uri.EMPTY,
    var alarmSoundUri: Uri = Uri.EMPTY,

    @Expose var streamsUnlinked: Boolean = false,

    @Expose var interruptionFilter: Int = 0,
    @Expose var ringerMode: Int = RINGER_MODE_NORMAL,
    @Expose var notificationMode: Int = RINGER_MODE_NORMAL,
    @Expose var isVibrateForCallsActive: Int = 0,

    @Expose var priorityCategories: Int = 0,
    @Expose var priorityCallSenders: Int = 0,
    @Expose var priorityMessageSenders: Int = 0,
    @Expose var suppressedVisualEffects: Int = 0,
    @Expose var primaryConversationSenders: Int = 0): Parcelable {

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