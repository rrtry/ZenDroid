package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volumeprofiler.database.repositories.LocationRepository
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.LocationRelation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationsListViewModel @Inject constructor(
        private val locationRepository: LocationRepository
): ViewModel() {

    val locationsFlow: Flow<List<LocationRelation>> = locationRepository.observeLocations()

    fun addLocation(location: Location): Unit {
        viewModelScope.launch {
            locationRepository.addLocation(location)
        }
    }

    private fun updateLocation(location: Location): Unit {
        viewModelScope.launch {
            locationRepository.updateLocation(location)
        }
    }

    fun removeLocation(location: Location): Unit {
        viewModelScope.launch {
            locationRepository.removeLocation(location)
        }
    }

    fun enableGeofence(location: Location): Unit {
        location.enabled = 1
        updateLocation(location)
    }

    fun disableGeofence(location: Location): Unit {
        location.enabled = 0
        updateLocation(location)
    }
}