package ru.rrtry.silentdroid.eventBus

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

        data class ProfileChanged(val id: UUID): Event()
        data class UpdateAlarmState(val alarm: Alarm): Event()
    }

    private val mutableEventsFlow: MutableSharedFlow<Event> = MutableSharedFlow(replay = 1, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val sharedFlow: SharedFlow<Event> = mutableEventsFlow

    fun onProfileChanged(id: UUID) {
        mutableEventsFlow.tryEmit(Event.ProfileChanged(id))
    }

    suspend fun updateAlarmState(alarm: Alarm): Unit {
        mutableEventsFlow.emit(Event.UpdateAlarmState(alarm))
    }
}