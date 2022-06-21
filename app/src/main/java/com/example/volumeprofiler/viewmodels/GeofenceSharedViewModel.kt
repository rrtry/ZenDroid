package com.example.volumeprofiler.viewmodels

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

    private val eventChannel: Channel<ViewEvent> = Channel(Channel.BUFFERED)
    val events: Flow<ViewEvent> = eventChannel.receiveAsFlow()

    sealed class ViewEvent {

        data class OnMapTypeChanged(val style: Int): ViewEvent()
        data class OnMapStyleChanged(val style: Int): ViewEvent()
        data class OnUpdateGeofenceEvent(val location: Location): ViewEvent()
        data class OnInsertGeofenceEvent(val location: Location): ViewEvent()

        object ToggleFloatingActionMenu: ViewEvent()
        object ObtainCurrentLocation: ViewEvent()
        object ShowMapStylesDialog: ViewEvent()
    }

    private var entitySet: Boolean = false
    private var locationId: Int? = null
    private var isEnabled: Boolean = false

    val title: MutableStateFlow<String> = MutableStateFlow("My geofence")
    val latLng: MutableStateFlow<Pair<LatLng, Boolean>> = MutableStateFlow(Pair(LatLng(-33.865143, 151.209900), false))
    val radius: MutableStateFlow<Float> = MutableStateFlow(100f)
    val enterProfile: MutableStateFlow<Profile?> = MutableStateFlow(null)
    val exitProfile: MutableStateFlow<Profile?> = MutableStateFlow(null)
    val address: MutableStateFlow<String> = MutableStateFlow("")
    val locality: MutableStateFlow<String> = MutableStateFlow("")

    val profilesStateFlow: StateFlow<List<Profile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), listOf())

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

    fun onApplyChangesButtonClick() {
        viewModelScope.launch {
            getLocation()?.let {
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

            enterProfile.value = locationRelation.onEnterProfile
            exitProfile.value = locationRelation.onExitProfile

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
            entitySet = true
        }
    }

    private fun getLocation(): Location {
        return Location(
            id = if (locationId != null) locationId!! else 0,
            title = title.value.ifEmpty { "No title" },
            latitude = latLng.value.first.latitude,
            longitude = latLng.value.first.longitude,
            address = address.value,
            locality = locality.value,
            radius = radius.value,
            onEnterProfileId = enterProfile.value!!.id,
            onExitProfileId = exitProfile.value!!.id,
            enabled = isEnabled
        )
    }

    fun getRadius(): Float = radius.value

    fun setLatLng(latLng: LatLng, queryAddress: Boolean = true) {
        this.latLng.value = Pair(latLng, queryAddress)
    }

    fun setTitle(title: String) {
        this.title.value = title
    }

    fun setAddress(address: String) {
        this.address.value = address
    }

    fun setRadius(radius: Float) {
        this.radius.value = radius
    }
}