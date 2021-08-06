package com.example.volumeprofiler.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import androidx.room.ForeignKey
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Parcelize
@Entity(foreignKeys = [ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["profileUUID"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE)])
data class Alarm(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "eventId") var id: Long = 0L,
                 @ColumnInfo(index = true) var profileUUID: UUID,
                 var localDateTime: LocalDateTime = LocalDateTime.now(),
                 var isScheduled: Int = 0,
                 var workingsDays: ArrayList<Int> = arrayListOf()) : Parcelable