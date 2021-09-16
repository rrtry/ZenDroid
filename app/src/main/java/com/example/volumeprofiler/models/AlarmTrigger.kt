package com.example.volumeprofiler.models

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlarmTrigger (

        @Embedded var profile: Profile,

        @Embedded var alarm: Alarm,

): Parcelable