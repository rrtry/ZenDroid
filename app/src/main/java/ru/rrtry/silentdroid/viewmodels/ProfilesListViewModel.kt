package ru.rrtry.silentdroid.viewmodels

import androidx.lifecycle.*
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.db.repositories.AlarmRepository
import ru.rrtry.silentdroid.db.repositories.LocationRepository
import ru.rrtry.silentdroid.db.repositories.ProfileRepository
import ru.rrtry.silentdroid.entities.LocationRelation
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
            viewEventChannel.send(
                ViewEvent.ProfileSetViewEvent(
                    profile, alarmRepository.getEnabledAlarms() ?: listOf()
                )
            )
        }
    }

    fun removeProfile(profile: Profile) {
        viewModelScope.launch {
            viewEventChannel.send(
                ViewEvent.CancelAlarmsViewEvent(
                    alarmRepository.getScheduledAlarmsByProfileId(
                        profile.id
                    )
                )
            )
            viewEventChannel.send(
                ViewEvent.RemoveGeofencesViewEvent(
                    locationRepository.getLocationsByProfileId(
                        profile.id
                    )
                )
            )
            profileRepository.removeProfile(profile)
            viewEventChannel.send(
                ViewEvent.ProfileRemoveViewEvent(
                    profile,
                    alarmRepository.getEnabledAlarms() ?: listOf()
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewEventChannel.cancel()
    }
}