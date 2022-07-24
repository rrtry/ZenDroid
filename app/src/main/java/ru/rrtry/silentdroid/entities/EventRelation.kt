package ru.rrtry.silentdroid.entities

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class EventRelation(

    @Embedded
    var event: Event,

    @Relation(parentColumn = "eventStartsProfileId", entityColumn = "id")
    var eventStartsProfile: Profile,

    @Relation(parentColumn = "eventEndsProfileId", entityColumn = "id")
    var eventEndsProfile: Profile,

    ): Parcelable