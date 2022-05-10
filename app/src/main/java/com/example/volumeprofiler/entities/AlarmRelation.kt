package com.example.volumeprofiler.entities

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlarmRelation(

        @Embedded var alarm: Alarm,

        @Relation(parentColumn = "startProfileUUID", entityColumn = "id")
        var startProfile: Profile,

        @Relation(parentColumn = "endProfileUUID", entityColumn = "id")
        var endProfile: Profile

): Parcelable