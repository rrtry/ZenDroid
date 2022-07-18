package com.example.volumeprofiler.entities

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import com.example.volumeprofiler.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationRelation(

        @Embedded
        var location: Location,

        @Relation(parentColumn = "onEnterProfileId", entityColumn = "id")
        var onEnterProfile: Profile,

        @Relation(parentColumn = "onExitProfileId", entityColumn = "id")
        var onExitProfile: Profile,

        @Ignore
        @IgnoredOnParcel
        override val id: Int,

        @Ignore
        @IgnoredOnParcel
        override val viewType: Int

): Parcelable, ListItem<Int> {

        constructor(location: Location, onEnterProfile: Profile, onExitProfile: Profile): this(
                location, onEnterProfile, onExitProfile, location.id, R.layout.location_item_view
        )
}