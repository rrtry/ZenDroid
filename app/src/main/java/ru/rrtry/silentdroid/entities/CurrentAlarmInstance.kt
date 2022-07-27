package ru.rrtry.silentdroid.entities

import java.time.LocalDateTime

data class CurrentAlarmInstance(
    val profile: Profile?,
    val until: LocalDateTime?,
    val from: LocalDateTime?,
    val relation: AlarmRelation
)
