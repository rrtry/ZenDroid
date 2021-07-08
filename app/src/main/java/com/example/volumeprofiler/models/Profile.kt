package com.example.volumeprofiler.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import android.app.NotificationManager.Policy.*
import android.media.AudioManager
import android.app.NotificationManager.Policy.*
import android.app.NotificationManager.*

@Entity
data class Profile(var title: String,
                   @PrimaryKey val id: UUID = UUID.randomUUID(),

                   var mediaVolume: Int = 0,
                   var callVolume: Int = 1,
                   var notificationVolume: Int = 0,
                   var ringVolume: Int = 0,
                   var alarmVolume: Int = 1,

                   var phoneRingtoneUri: Uri = Uri.EMPTY,
                   var notificationSoundUri: Uri = Uri.EMPTY,
                   var alarmSoundUri: Uri = Uri.EMPTY,

                   var interruptionFilter: Int = INTERRUPTION_FILTER_PRIORITY,
                   var isInterruptionFilterActive: Int = 0,
                   var ringerMode: Int = AudioManager.RINGER_MODE_NORMAL,
                   var isVibrateForCallsActive: Int = 0,


                   var dialTones: Int = 0,
                   var screenLockingSounds: Int = 0,
                   var chargingSoundsAndVibration: Int = 0,
                   var touchSounds: Int = 0,
                   var touchVibration: Int = 0,
                   var shutterSound: Int = 0,

                   var priorityCategories: String = "",
                   var priorityCallSenders: Int = PRIORITY_SENDERS_ANY,
                   var priorityMessageSenders: Int = PRIORITY_SENDERS_ANY,
                   var screenOnVisualEffects: String = "",
                   var screenOffVisualEffects: String = "",
                   var primaryConversationSenders: Int = CONVERSATION_SENDERS_ANYONE)