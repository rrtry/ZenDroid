package com.example.volumeprofiler.entities

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlarmRelation (

        @Embedded var profile: Profile,

        @Embedded var alarm: Alarm,

): Parcelable