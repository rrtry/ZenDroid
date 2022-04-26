package com.example.volumeprofiler.entities

import android.os.Parcelable
import androidx.room.*
import java.util.UUID
import com.example.volumeprofiler.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

@Parcelize
@Entity(foreignKeys = [ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["profileUUID"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE)])
data class Alarm(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "eventId") override val id: Long,

                 @ColumnInfo(index = true)
                 var profileUUID: UUID,
                 var instanceStartTime: Instant,
                 var localStartTime: LocalTime,
                 var zoneId: ZoneId,
                 var isScheduled: Int,
                 var scheduledDays: Int) : Parcelable, ListItem() {

                     @IgnoredOnParcel
                     @Ignore
                     override val itemViewType: Int = R.layout.alarm_item_view

                 }