package com.example.volumeprofiler.eventBus

import android.util.Log
import com.example.volumeprofiler.entities.Alarm
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
        data class UpdateAlarmState(val alarm: Alarm): Event()
    }

    private val mutableEventsFlow: MutableSharedFlow<Event> = MutableSharedFlow(replay = 1, extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val sharedFlow: SharedFlow<Event> = mutableEventsFlow

    fun updateProfilesFragment(id: UUID): Unit {
        Log.i("EventBus", "updateProfilesFragment")
        mutableEventsFlow.tryEmit(Event.ProfileApplied(id))
    }

    fun updateAlarmState(alarm: Alarm): Unit {
        mutableEventsFlow.tryEmit(Event.UpdateAlarmState(alarm))
    }
}