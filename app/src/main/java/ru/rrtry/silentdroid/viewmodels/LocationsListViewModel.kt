package ru.rrtry.silentdroid.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.rrtry.silentdroid.db.repositories.LocationRepository
import ru.rrtry.silentdroid.entities.Location
import ru.rrtry.silentdroid.entities.LocationRelation
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

        object RequestLocationPermission: ViewEvent()
    }

    fun removeGeofence(location: LocationRelation) {
        viewModelScope.launch {
            removeLocation(location.location)
            channel.send(ViewEvent.OnGeofenceRemoved(location))
        }
    }

    fun disableGeofence(relation: LocationRelation) {
        viewModelScope.launch {
            disableGeofence(relation.location)
            channel.send(ViewEvent.OnGeofenceDisabled(relation))
        }
    }

    fun enableGeofence(relation: LocationRelation) {
        viewModelScope.launch {
            enableGeofence(relation.location)
            channel.send(ViewEvent.OnGeofenceEnabled(relation))
        }
    }

    fun requestLocationPermission() {
        viewModelScope.launch {
            channel.send(ViewEvent.RequestLocationPermission)
        }
    }

    fun addLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.addLocation(location)
        }
    }

    fun removeLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.removeLocation(location)
        }
    }

    private suspend fun enableGeofence(location: Location) {
        location.enabled = true
        locationRepository.updateLocation(location)
    }

    private suspend fun disableGeofence(location: Location) {
        location.enabled = false
        locationRepository.updateLocation(location)
    }
}