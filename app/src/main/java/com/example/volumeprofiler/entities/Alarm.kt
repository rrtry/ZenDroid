package com.example.volumeprofiler.entities

import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.Expose
import java.util.UUID
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Parcelize
@Entity(foreignKeys = [
    ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["startProfileUUID"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["endProfileUUID"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE)
])
data class Alarm(@PrimaryKey(autoGenerate = true)
                 @ColumnInfo(name = "eventId")
                 var id: Int,
                 @Expose var title: String = "No title",

                 @ColumnInfo(index = true)
                 var startProfileUUID: UUID,
                 var endProfileUUID: UUID,
                 var startDateTime: LocalDateTime? = null,
                 var endDateTime: LocalDateTime? = null,
                 var startTime: LocalTime,
                 var endTime: LocalTime,
                 var zoneId: ZoneId,
                 var isScheduled: Boolean,
                 var scheduledDays: Int): Parcelable