package com.example.volumeprofiler.entities

import java.time.ZonedDateTime

data class OngoingAlarm(
    val profile: Profile,
    val until: ZonedDateTime,
    val from: ZonedDateTime,
    val alarm: Alarm
)
