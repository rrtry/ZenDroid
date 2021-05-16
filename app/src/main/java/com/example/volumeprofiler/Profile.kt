package com.example.volumeprofiler

import androidx.room.Entity
import androidx.room.Ignore
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
                   var dialTones: Int = 0,
                   var screenLockingSounds: Int = 0,
                   var chargingSoundsAndVibration: Int = 0,
                   var touchSounds: Int = 0,
                   var touchVibration: Int = 0,
                   var shutterSound: Int = 0) {
    @Ignore var isActive: Boolean = false
}