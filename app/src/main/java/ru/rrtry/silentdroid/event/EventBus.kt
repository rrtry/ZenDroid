package ru.rrtry.silentdroid.event

import ru.rrtry.silentdroid.entities.Alarm
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventBus @Inject constructor() {

    sealed class Event {

        data class OnProfileChanged(val id: UUID): Event()
        data class OnUpdateAlarmState(val alarm: Alarm): Event()
        object OnAlterStreamVolumeFailure: Event()
    }

    private val eventsFlow: MutableSharedFlow<Event> = MutableSharedFlow(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val eventBus: SharedFlow<Event> = eventsFlow
    
    fun onAlterStreamVolumeFailure() {
        eventsFlow.tryEmit(Event.OnAlterStreamVolumeFailure)
    }
    
    fun onProfileChanged(id: UUID) {
        eventsFlow.tryEmit(Event.OnProfileChanged(id))
    }

    suspend fun updateAlarmState(alarm: Alarm): Unit {
        eventsFlow.emit(Event.OnUpdateAlarmState(alarm))
    }
}