package com.example.volumeprofiler.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Profile(var title: String,
                   @PrimaryKey val id: UUID = UUID.randomUUID(),
                   var mediaVolume: Int = 0,
                   var callVolume: Int = 0,
                   var notificationVolume: Int = 0,
                   var ringVolume: Int = 0,
                   var alarmVolume: Int = 0,
                   var phoneRingtoneUri: Uri = Uri.EMPTY,
                   var notificationSoundUri: Uri = Uri.EMPTY,
                   var alarmSoundUri: Uri = Uri.EMPTY,
                   var dialTones: Int = 0,
                   var screenLockingSounds: Int = 0,
                   var chargingSoundsAndVibration: Int = 0,
                   var touchSounds: Int = 0,
                   var touchVibration: Int = 0,
                   var shutterSound: Int = 0,
                   var vibrateForCalls: Int = 0)