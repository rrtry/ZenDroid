package com.example.volumeprofiler.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.*
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
    var rrule: String?,
    var instanceBeginTime: Long,
    var instanceEndTime: Long,
    var timezoneId: String,
    var scheduled: Boolean = false,

    @ColumnInfo(index = true)
    var eventStartsProfileId: UUID,

    @ColumnInfo(index = true)
    var eventEndsProfileId: UUID,

    ): Parcelable {

    private fun isRepeating(): Boolean {
        return endTime == 0L
    }

    fun isInstanceObsolete(millis: Long): Boolean {
        val zoneId: ZoneId = ZoneId.of(timezoneId)
        val now: LocalDateTime = LocalDateTime.now()
        val millisDate: LocalDateTime = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDateTime()
        return now.isAfter(millisDate)
    }

    fun isObsolete(): Boolean {
        return if (isRepeating()) {
            false
        } else {
            return isInstanceObsolete(endTime)
        }
    }

    override fun toString(): String {
        return "currentInstanceStartTime: $instanceBeginTime\n" +
                "currentInstanceEndTime: $instanceEndTime\n"
    }

    enum class State {
        START, END
    }

    companion object {

        fun sortEvents(events: List<EventRelation>): List<EventRelation> {
            return events.sortedWith { previous, next ->
                val previousEvent: Event = previous.event
                val nextEvent: Event = next.event
                when {
                    previousEvent.instanceBeginTime > nextEvent.instanceBeginTime -> 1
                    previousEvent.instanceBeginTime == nextEvent.instanceBeginTime -> 0
                    else -> -1
                }
            }
        }
    }
}