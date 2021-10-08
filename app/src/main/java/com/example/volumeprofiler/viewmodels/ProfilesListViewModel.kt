package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmRelation
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.database.repositories.ProfileRepository
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
): ViewModel() {

    sealed class Event {

        data class CancelAlarmsEvent(val alarms: List<AlarmRelation>?): Event()
        object RemoveGeofencesEvent: Event()
    }

    private val eventChannel: Channel<Event> = Channel(Channel.BUFFERED)
    val eventFlow: Flow<Event> = eventChannel.receiveAsFlow()

    val profilesFlow: Flow<List<Profile>> = profileRepository.observeProfiles()

    var lastSelected: UUID? = null

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
            eventChannel.send(Event.CancelAlarmsEvent(alarmRepository.getScheduledAlarmsByProfileId(profile.id)))
            eventChannel.send(Event.RemoveGeofencesEvent)
            profileRepository.removeProfile(profile)
        }
    }

    override fun onCleared(): Unit {
        super.onCleared()
        eventChannel.cancel()
    }
}