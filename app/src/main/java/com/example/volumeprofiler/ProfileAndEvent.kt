package com.example.volumeprofiler

import androidx.room.Embedded

data class ProfileAndEvent (

        @Embedded var profile: Profile,

        @Embedded
        var event: Event
)