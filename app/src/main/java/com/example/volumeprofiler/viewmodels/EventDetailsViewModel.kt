package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.entities.Event
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.ContentUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val contentUtil: ContentUtil
): ViewModel() {

    val profilesStateFlow: StateFlow<List<Profile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), listOf())

    val eventStartsSelectedProfile: MutableStateFlow<Int> = MutableStateFlow(0)
    val eventEndsSelectedProfile: MutableStateFlow<Int> = MutableStateFlow(0)

    val eventStartsProfile: StateFlow<Profile?> = combine(
        profilesStateFlow,
        eventStartsSelectedProfile,
    ) {
        profiles, position -> if (profiles.isNotEmpty()) profiles[position] else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val eventEndsProfile: StateFlow<Profile?> = combine(
        profilesStateFlow,
        eventEndsSelectedProfile
    ) {
        profiles, position ->
        if (profiles.isNotEmpty()) profiles[position] else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val eventId: MutableStateFlow<Int> = MutableStateFlow(-1)

    private var scheduled: Boolean = false

    var calendarId: Int = -1
    var calendarTitle: String = ""

    var eventTitle: String = ""
    var startTime: Long = 0L
    var endTime: Long = 0L
    var instanceStartTime: Long = -1L
    var instanceEndTime: Long = -1L

    private var eventSet: Boolean = false

    fun isEventSet(): Boolean {
        return eventSet
    }

    fun setCalendarID(id: Int): Unit {
        calendarId = id
    }

    fun setEventId(id: Int): Unit {
        eventId.value = id
    }

    fun getEventId(): Int {
        return eventId.value
    }

    fun getEvent(): Event {
        return Event(
            id = eventId.value,
            title = eventTitle,
            calendarId = calendarId,
            calendarTitle = calendarTitle,
            startTime = startTime,
            endTime = endTime,
            eventStartsProfileId = eventStartsProfile.value!!.id,
            eventEndsProfileId = eventEndsProfile.value!!.id,
            scheduled = true,
            currentInstanceStartTime = instanceStartTime,
            currentInstanceEndTime = instanceEndTime,
        )
    }

    fun setEvent(event: Event, profiles: List<Profile>): Unit {
        eventId.value = event.id
        eventStartsSelectedProfile.value = getProfilePosition(event.eventStartsProfileId, profiles)
        eventEndsSelectedProfile.value = getProfilePosition(event.eventEndsProfileId, profiles)
        calendarId = event.calendarId
        startTime = event.startTime
        endTime = event.endTime
        scheduled = event.scheduled
        instanceStartTime = event.currentInstanceStartTime
        instanceEndTime = event.currentInstanceEndTime

        eventSet = true
    }

    private fun getProfilePosition(uuid: UUID?, profiles: List<Profile>): Int {
        for ((index, i) in profiles.withIndex()) {
            if (i.id == uuid) {
                return index
            }
        }
        return 0
    }
}