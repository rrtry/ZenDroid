package com.example.volumeprofiler

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import androidx.room.ForeignKey
import java.time.LocalDateTime

@Entity(foreignKeys = [ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["profileUUID"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE)])
data class Event(@PrimaryKey(autoGenerate = true) var eventId: Long = 0L,
                 var profileUUID: UUID,
                 var localDateTime: LocalDateTime = LocalDateTime.now(),
                 var isScheduled: Int = 0,
                 var workingDays: String = "")