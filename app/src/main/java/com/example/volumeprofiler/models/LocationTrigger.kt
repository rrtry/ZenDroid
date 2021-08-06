package com.example.volumeprofiler.models

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationTrigger (

        @Embedded var location: Location,
        @Embedded var profile: Profile
): Parcelable