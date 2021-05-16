package com.example.volumeprofiler.models

import androidx.room.Embedded
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.models.Profile

data class ProfileAndEvent (

        @Embedded var profile: Profile,

        @Embedded
        var event: Event
)