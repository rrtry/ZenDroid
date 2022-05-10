package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volumeprofiler.database.repositories.LocationRepository
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.LocationRelation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationsListViewModel @Inject constructor(
        private val locationRepository: LocationRepository
): ViewModel() {

    val locationsFlow: Flow<List<LocationRelation>> = locationRepository.observeLocations()

    private val channel: Channel<ViewEvent> = Channel(Channel.BUFFERED)
    val viewEvents: Flow<ViewEvent> = channel.receiveAsFlow()

    sealed class ViewEvent {

        data class OnGeofenceEnabled(val relation: LocationRelation): ViewEvent()
        data class OnGeofenceDisabled(val relation: LocationRelation): ViewEvent()
        data class OnGeofenceRemoved(val relation: LocationRelation): ViewEvent()
    }

    fun removeGeofence(location: LocationRelation) {
        viewModelScope.launch {
            channel.send(ViewEvent.OnGeofenceRemoved(location))
        }
    }

    fun disableGeofence(location: LocationRelation) {
        viewModelScope.launch {
            channel.send(ViewEvent.OnGeofenceDisabled(location))
        }
    }

    fun enableGeofence(location: LocationRelation) {
        viewModelScope.launch {
            channel.send(ViewEvent.OnGeofenceEnabled(location))
        }
    }

    fun addLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.addLocation(location)
        }
    }

    private fun updateLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.updateLocation(location)
        }
    }

    fun removeLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.removeLocation(location)
        }
    }

    fun enableGeofence(location: Location) {
        location.enabled = true
        updateLocation(location)
    }

    fun disableGeofence(location: Location) {
        location.enabled = false
        updateLocation(location)
    }
}