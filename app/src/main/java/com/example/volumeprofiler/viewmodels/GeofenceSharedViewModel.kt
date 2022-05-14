package com.example.volumeprofiler.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.example.volumeprofiler.database.repositories.LocationRepository
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.entities.LocationSuggestion
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.AddressWrapper
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeofenceSharedViewModel @Inject constructor(
        private val profileRepository: ProfileRepository,
        private val locationRepository: LocationRepository
): ViewModel() {

    sealed class ViewEvent {

        data class OnMapTypeChanged(val style: Int): ViewEvent()
        data class OnMapStyleChanged(val style: Int): ViewEvent()
        data class OnUpdateGeofenceEvent(val location: Location): ViewEvent()
        data class OnInsertGeofenceEvent(val location: Location): ViewEvent()
        data class OnRemoveGeofenceEvent(val location: Location): ViewEvent()

        object OnWrongInputEvent: ViewEvent()
        object ToggleFloatingActionMenu: ViewEvent()
        object ObtainCurrentLocation: ViewEvent()
        object ShowMapStylesDialog: ViewEvent()
    }

    private data class PositionPair(var first: Int, var second: Int)

    private var entitySet: Boolean = false

    val title: MutableStateFlow<String?> = MutableStateFlow("My geofence")
    val latLng: MutableStateFlow<Pair<LatLng, Boolean>?> = MutableStateFlow(null)
    val radius: MutableStateFlow<Float> = MutableStateFlow(100f)
    val enterProfilePosition: MutableStateFlow<Int> = MutableStateFlow(0)
    val exitProfilePosition: MutableStateFlow<Int> = MutableStateFlow(0)
    private val locality: MutableStateFlow<String> = MutableStateFlow("")
    val address: MutableStateFlow<String> = MutableStateFlow("")

    val profilesStateFlow: StateFlow<List<Profile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), listOf())

    private val eventChannel: Channel<ViewEvent> = Channel(Channel.BUFFERED)
    val events: Flow<ViewEvent> = eventChannel.receiveAsFlow()

    private var locationId: Int? = null
    private var isEnabled: Boolean = false

    fun onMapStylesFabClick() {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.ShowMapStylesDialog)
        }
    }

    fun onCurrentLocationFabClick() {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.ObtainCurrentLocation)
        }
    }

    fun onExpandableFabClick() {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.ToggleFloatingActionMenu)
        }
    }

    fun onMapStyleChanged(style: Int) {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.OnMapStyleChanged(style))
        }
    }

    fun onMapTypeChanged(type: Int) {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.OnMapTypeChanged(type))
        }
    }

    fun onApplyChangesButtonClick() {
        viewModelScope.launch {
            getLocation(profilesStateFlow.value)?.let {
                if (locationId != null) {
                    eventChannel.send(ViewEvent.OnUpdateGeofenceEvent(it))
                } else {
                    eventChannel.send(ViewEvent.OnInsertGeofenceEvent(it))
                }
            }
        }
    }

    fun removeSuggestion(suggestion: LocationSuggestion): Job {
        return viewModelScope.launch {
            locationRepository.removeSuggestion(suggestion)
        }
    }

    fun addSuggestion(suggestion: LocationSuggestion) {
        viewModelScope.launch {
            locationRepository.addSuggestion(suggestion)
        }
    }

    suspend fun getSuggestions(query: String?): List<AddressWrapper> {
        val suggestions: List<LocationSuggestion> = if (query.isNullOrEmpty()) {
            locationRepository.getAllRecentSuggestions()
        } else {
            locationRepository.getRecentLocationsByAddress(query)
        }
        return suggestions.map {
            AddressWrapper(
                it.latitude,
                it.longitude,
                it.address,
                true
            )
        }
    }

    fun updateLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.updateLocation(location)
        }
    }

    fun addLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.addLocation(location)
        }
    }

    fun setEntity(locationRelation: LocationRelation) {
        if (!entitySet) {

            getPositions(locationRelation).let { positions ->
                enterProfilePosition.value = positions.first
                exitProfilePosition.value = positions.second
            }

            latLng.value = Pair(
                LatLng(
                    locationRelation.location.latitude,
                    locationRelation.location.longitude
                ), false
            )

            title.value = locationRelation.location.title
            address.value = locationRelation.location.address
            radius.value = locationRelation.location.radius

            locationId = locationRelation.location.id
            isEnabled = locationRelation.location.enabled
        }
    }

    private fun getPositions(locationRelation: LocationRelation): PositionPair {
        return PositionPair(0, 0).apply {
            for ((index, i) in profilesStateFlow.value.withIndex()) {
                when (i.id) {
                    locationRelation.onEnterProfile.id -> first = index
                    locationRelation.onExitProfile.id -> second = index
                }
            }
        }
    }

    private fun getEnterProfile(profiles: List<Profile>): Profile {
        return profiles[enterProfilePosition.value]
    }

    private fun getExitProfile(profiles: List<Profile>): Profile {
        return profiles[exitProfilePosition.value]
    }

    private fun getLocation(profiles: List<Profile>): Location? {
        return if (title.value == null || latLng.value == null) {
            null
        } else {
            Location(
                id = if (locationId != null) locationId!! else 0,
                title = title.value!!,
                latitude = latLng.value!!.first.latitude,
                longitude = latLng.value!!.first.longitude,
                address = address.value,
                locality = locality.value,
                radius = radius.value,
                onEnterProfileId = getEnterProfile(profiles).id,
                onExitProfileId = getExitProfile(profiles).id,
                enabled = isEnabled
            )
        }
    }

    fun getRadius(): Float {
        return radius.value
    }

    fun setLatLng(latLng: LatLng, queryAddress: Boolean = true) {
        this.latLng.value = Pair(latLng, queryAddress)
    }

    fun setTitle(title: String?) {
        this.title.value = title
    }

    fun setAddress(address: String) {
        this.address.value = address
    }

    fun setRadius(radius: Float) {
        this.radius.value = radius
    }
}