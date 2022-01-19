package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.*
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.database.repositories.LocationRepository
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.entities.LocationRelation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ProfilesListViewModel @Inject constructor(
        private val profileRepository: ProfileRepository,
        private val alarmRepository: AlarmRepository,
        private val locationRepository: LocationRepository
): ViewModel() {

    sealed class Event {

        data class ProfileSetEvent(val profile: Profile): Event()
        data class ProfileRemoveEvent(val profile: Profile): Event()
        data class CancelAlarmsEvent(val alarms: List<AlarmRelation>?): Event()
        data class RemoveGeofencesEvent(val geofences: List<LocationRelation>): Event()
    }

    private val eventChannel: Channel<Event> = Channel(Channel.BUFFERED)
    val eventFlow: Flow<Event> = eventChannel.receiveAsFlow()

    val profilesFlow: Flow<List<Profile>> = profileRepository.observeProfiles()

    var lastSelected: UUID? = null

    fun setProfile(profile: Profile): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ProfileSetEvent(profile))
        }
    }

    fun addProfile(profile: Profile): Unit {
        viewModelScope.launch {
            profileRepository.addProfile(profile)
        }
    }

    fun updateProfile(profile: Profile): Unit {
        viewModelScope.launch {
            profileRepository.updateProfile(profile)
        }
    }

    fun removeProfile(profile: Profile): Unit {
        viewModelScope.launch {
            launch {
                eventChannel.send(Event.ProfileRemoveEvent(profile))
            }
            launch {
                eventChannel.send(Event.CancelAlarmsEvent(alarmRepository.getScheduledAlarmsByProfileId(profile.id)))
            }
            launch {
                eventChannel.send(Event.RemoveGeofencesEvent(locationRepository.getLocationsByProfileId(profile.id)))
            }
            profileRepository.removeProfile(profile)
        }
    }

    override fun onCleared(): Unit {
        super.onCleared()
        eventChannel.cancel()
    }
}