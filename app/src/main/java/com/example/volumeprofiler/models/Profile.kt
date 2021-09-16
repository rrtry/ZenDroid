package com.example.volumeprofiler.models

import android.net.Uri
import androidx.room.PrimaryKey
import java.util.UUID
import android.app.NotificationManager.Policy.*
import android.media.AudioManager
import android.app.NotificationManager.*
import android.os.Parcelable
import kotlinx.parcelize.*

@Parcelize
@androidx.room.Entity
data class Profile(var title: String,

                   @PrimaryKey val id: UUID = UUID.randomUUID(),

                   var mediaVolume: Int = 0,
                   var callVolume: Int = 1,
                   var notificationVolume: Int = 3,
                   var ringVolume: Int = 3,
                   var alarmVolume: Int = 1,

                   var phoneRingtoneUri: Uri = Uri.EMPTY,
                   var notificationSoundUri: Uri = Uri.EMPTY,
                   var alarmSoundUri: Uri = Uri.EMPTY,

                   var interruptionFilter: Int = INTERRUPTION_FILTER_PRIORITY,
                   var ringerMode: Int = AudioManager.RINGER_MODE_NORMAL,
                   var notificationMode: Int = AudioManager.RINGER_MODE_NORMAL,
                   var isVibrateForCallsActive: Int = 0,

                   var priorityCategories: ArrayList<Int> = arrayListOf(PRIORITY_CATEGORY_CALLS, PRIORITY_CATEGORY_MESSAGES),
                   var priorityCallSenders: Int = PRIORITY_SENDERS_ANY,
                   var priorityMessageSenders: Int = PRIORITY_SENDERS_ANY,
                   var screenOnVisualEffects: ArrayList<Int> = arrayListOf(),
                   var screenOffVisualEffects: ArrayList<Int> = arrayListOf(),
                   var primaryConversationSenders: Int = CONVERSATION_SENDERS_ANYONE): Parcelable