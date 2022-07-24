package ru.rrtry.silentdroid.entities

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import ru.rrtry.silentdroid.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlarmRelation(

    @Embedded var alarm: Alarm,

    @Relation(parentColumn = "startProfileUUID", entityColumn = "id")
        var startProfile: Profile,

    @Relation(parentColumn = "endProfileUUID", entityColumn = "id")
        var endProfile: Profile,

    @Ignore
        @IgnoredOnParcel
        override val id: Int,

    @Ignore
        @IgnoredOnParcel
        override val viewType: Int = R.layout.alarm_item_view

): Parcelable, ListItem<Int> {

        constructor(alarm: Alarm, startProfile: Profile, endProfile: Profile): this(
                alarm, startProfile, endProfile, alarm.id, R.layout.alarm_item_view
        )
}