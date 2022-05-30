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

    sealed class ViewEvent {

        data class ProfileSetViewEvent(val profile: Profile, val alarms: List<AlarmRelation>): ViewEvent()
        data class ProfileRemoveViewEvent(val profile: Profile, val alarms: List<AlarmRelation>): ViewEvent()
        data class CancelAlarmsViewEvent(val alarms: List<AlarmRelation>?): ViewEvent()
        data class RemoveGeofencesViewEvent(val geofences: List<LocationRelation>): ViewEvent()
    }

    private val viewEventChannel: Channel<ViewEvent> = Channel(Channel.BUFFERED)
    val viewEventFlow: Flow<ViewEvent> = viewEventChannel.receiveAsFlow()

    val profilesFlow: Flow<List<Profile>> = profileRepository.observeProfiles()

    var lastSelected: UUID? = null

    fun setProfile(profile: Profile) {
        viewModelScope.launch {
            viewEventChannel.send(ViewEvent.ProfileSetViewEvent(
                profile, alarmRepository.getEnabledAlarms() ?: listOf()
            ))
        }
    }

    fun addProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.addProfile(profile)
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.updateProfile(profile)
        }
    }

    fun removeProfile(profile: Profile) {
        viewModelScope.launch {
            val enabledAlarms: List<AlarmRelation> = alarmRepository.getEnabledAlarms() ?: listOf()
            launch {
                viewEventChannel.send(ViewEvent.ProfileRemoveViewEvent(profile, enabledAlarms))
            }
            launch {
                viewEventChannel.send(ViewEvent.CancelAlarmsViewEvent(alarmRepository.getScheduledAlarmsByProfileId(profile.id)))
            }
            launch {
                viewEventChannel.send(ViewEvent.RemoveGeofencesViewEvent(locationRepository.getLocationsByProfileId(profile.id)))
            }
            profileRepository.removeProfile(profile)
        }
    }

    override fun onCleared(): Unit {
        super.onCleared()
        viewEventChannel.cancel()
    }
}