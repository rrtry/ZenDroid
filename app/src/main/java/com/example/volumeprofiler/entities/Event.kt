package com.example.volumeprofiler.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.*

@Entity(foreignKeys = [ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["eventEndsProfileId"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE),
                        ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["eventStartsProfileId"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE)])
@Parcelize
data class Event(

    @PrimaryKey
    var id: Int,

    var title: String,
    var calendarId: Int,
    var calendarTitle: String,
    var startTime: Long,
    var endTime: Long,
    var currentInstanceStartTime: Long,
    var currentInstanceEndTime: Long,
    var scheduled: Boolean = false,

    @ColumnInfo(index = true)
    var eventStartsProfileId: UUID,

    @ColumnInfo(index = true)
    var eventEndsProfileId: UUID?,

): Parcelable {

    override fun toString(): String {
        return "currentInstanceStartTime: $currentInstanceStartTime\n" +
                "currentInstanceEndTime: $currentInstanceEndTime\n"
    }

    enum class State {
        STARTED, COMPLETED
    }
}