package com.example.volumeprofiler.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import androidx.room.ForeignKey
import java.time.LocalDateTime

@Entity(foreignKeys = [ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["profileUUID"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE)])
data class Event(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "eventId") var id: Long = 0L,
                 var profileUUID: UUID,
                 var localDateTime: LocalDateTime = LocalDateTime.now(),
                 var isScheduled: Int = 0,
                 var workingDays: String = "")