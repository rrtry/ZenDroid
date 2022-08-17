package ru.rrtry.silentdroid.entities

import java.time.LocalDateTime

data class PreviousAndNextTrigger(
    val profile: Profile?,
    val until: LocalDateTime?,
    val from: LocalDateTime?,
    val relation: AlarmRelation
)
