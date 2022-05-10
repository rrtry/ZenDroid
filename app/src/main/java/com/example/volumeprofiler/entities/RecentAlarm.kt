package com.example.volumeprofiler.entities

import java.time.ZonedDateTime

data class RecentAlarm(
    val profile: Profile,
    val time: ZonedDateTime,
    val alarm: Alarm
)
