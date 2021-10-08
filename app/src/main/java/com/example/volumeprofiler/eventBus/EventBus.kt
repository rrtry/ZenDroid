package com.example.volumeprofiler.eventBus

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventBus @Inject constructor() {

    sealed class Event {

        data class ProfileApplied(val id: UUID): Event()
        data class UpdateAlarmState(val id: Long): Event()
    }

    private val mutableEventsFlow: MutableSharedFlow<Event> = MutableSharedFlow(replay = 1, extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val sharedFlow: SharedFlow<Event> = mutableEventsFlow

    fun updateProfilesFragment(id: UUID): Unit {
        mutableEventsFlow.tryEmit(Event.ProfileApplied(id))
    }

    fun updateAlarmState(id: Long): Unit {
        mutableEventsFlow.tryEmit(Event.UpdateAlarmState(id))
    }
}