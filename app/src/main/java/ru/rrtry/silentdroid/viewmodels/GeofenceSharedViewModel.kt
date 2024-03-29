package ru.rrtry.silentdroid.viewmodels

import androidx.lifecycle.*
import ru.rrtry.silentdroid.db.repositories.LocationRepository
import ru.rrtry.silentdroid.db.repositories.ProfileRepository
import ru.rrtry.silentdroid.entities.Location
import ru.rrtry.silentdroid.entities.LocationRelation
import ru.rrtry.silentdroid.entities.LocationSuggestion
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.util.AddressWrapper
import ru.rrtry.silentdroid.util.MapsUtil
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
        object OnRequestBackgroundLocationPermission: ViewEvent()
    }

    private var isEntitySet: Boolean = false
    var locationId: Int? = null
    var isRegistered: Boolean = false
    var backgroundLocationAccessGranted: Boolean = false

    val title: MutableStateFlow<String> = MutableStateFlow("My geofence")
    val latLng: MutableStateFlow<Pair<LatLng, Boolean>> = MutableStateFlow(Pair(LatLng(-33.865143, 151.209900), false))
    val radius: MutableStateFlow<Float> = MutableStateFlow(100f)
    val enterProfile: MutableStateFlow<Profile?> = MutableStateFlow(null)
    val exitProfile: MutableStateFlow<Profile?> = MutableStateFlow(null)
    val address: MutableStateFlow<String> = MutableStateFlow("6/10 O'Connell St, The Rocks NSW 2000")

    val latitudeTextInputError: MutableStateFlow<String?> = MutableStateFlow(null)
    val longitudeTextInputError: MutableStateFlow<String?> = MutableStateFlow(null)

    val profilesStateFlow: StateFlow<List<Profile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), listOf())

    fun onLatitudeTextInputChanged(latitudeSeq: CharSequence?) {
        if (MapsUtil.isLatitude(latitudeSeq?.toString())) {
            latitudeTextInputError.value = null
        } else {
            latitudeTextInputError.value = "Enter value between -90 and 90"
        }
    }

    fun onLongitudeTextInputChanged(longitudeSeq: CharSequence?) {
        if (MapsUtil.isLongitude(longitudeSeq?.toString())) {
            longitudeTextInputError.value = null
        } else {
            longitudeTextInputError.value = "Enter value between -180 and 180"
        }
    }

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
            if (!backgroundLocationAccessGranted && isRegistered) {
                eventChannel.send(ViewEvent.OnRequestBackgroundLocationPermission)
            } else {
                eventChannel.send(if (locationId != null) {
                    ViewEvent.OnUpdateGeofenceEvent(getLocation())
                } else {
                    ViewEvent.OnInsertGeofenceEvent(getLocation())
                })
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

    fun setProfiles(profiles: List<Profile>) {
        if (locationId == null && !isEntitySet) {
            enterProfile.value = profiles.random()
            exitProfile.value = profiles.random()
            isEntitySet = true
        }
    }

    fun setEntity(locationRelation: LocationRelation) {
        if (isEntitySet) return

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
        isRegistered = locationRelation.location.enabled
        isEntitySet = true
    }

    fun getLocation(): Location {
        return Location(
            id = if (locationId != null) locationId!! else 0,
            title = title.value.trim().ifBlank { "No title" },
            latitude = latLng.value.first.latitude,
            longitude = latLng.value.first.longitude,
            address = address.value,
            radius = radius.value,
            onEnterProfileId = enterProfile.value!!.id,
            onExitProfileId = exitProfile.value!!.id,
            enabled = isRegistered
        )
    }

    fun getRadius(): Float = radius.value

    fun setLatLng(latLng: LatLng, queryAddress: Boolean = true) {
        this.latLng.value = Pair(latLng, queryAddress)
    }

    fun setRadius(radius: Float) {
        this.radius.value = radius
    }
}