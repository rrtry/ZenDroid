package com.example.volumeprofiler.viewmodels

import android.util.Log
import androidx.collection.ArrayMap
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.models.LocationTrigger
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ProfileListViewModel @Inject constructor(
        private val profileRepository: ProfileRepository,
        private val alarmRepository: AlarmRepository
): ViewModel() {

    sealed class Event {

        data class CancelAlarmsEvent(val alarms: List<AlarmTrigger>?): Event()

        object RemoveGeofencesEvent: Event()
    }

    private val eventChannel: Channel<Event> = Channel(Channel.BUFFERED)
    val eventFlow: Flow<Event> = eventChannel.receiveAsFlow()

    var lastActiveProfileIndex: Int = -1

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
            eventChannel.send(Event.CancelAlarmsEvent(alarmRepository.getActiveAlarmTriggersByProfileId(profile.id)))
            eventChannel.send(Event.RemoveGeofencesEvent)
            profileRepository.removeProfile(profile)
        }
    }

    override fun onCleared(): Unit {
        super.onCleared()
        eventChannel.cancel()
    }
}