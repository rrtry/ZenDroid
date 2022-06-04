package com.example.volumeprofiler.entities

import java.time.LocalDateTime

data class OngoingAlarm(
    val profile: Profile?,
    val until: LocalDateTime?,
    val from: LocalDateTime?,
    val alarm: Alarm
)
