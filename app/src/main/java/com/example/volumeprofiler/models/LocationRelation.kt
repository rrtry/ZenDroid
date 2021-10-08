package com.example.volumeprofiler.models

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationRelation (

        @Embedded
        var location: Location,

        @Relation(parentColumn = "onEnterProfileId", entityColumn = "id")
        var onEnterProfile: Profile,

        @Relation(parentColumn = "onExitProfileId", entityColumn = "id")
        var onExitProfile: Profile

): Parcelable