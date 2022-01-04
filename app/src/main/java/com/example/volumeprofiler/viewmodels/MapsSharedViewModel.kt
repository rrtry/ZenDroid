package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.*
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.entities.Profile
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MapsSharedViewModel @Inject constructor(
        private val repository: ProfileRepository
): ViewModel() {

    private data class Pair(var first: Int, var second: Int)

    private var areArgsSet: Boolean = false

    val title: MutableStateFlow<String?> = MutableStateFlow(null)
    val latLng: MutableStateFlow<LatLng?> = MutableStateFlow(null)
    val address: MutableStateFlow<String?> = MutableStateFlow(null)
    val radius: MutableStateFlow<Float> = MutableStateFlow(100f)
    private val locality: MutableStateFlow<String> = MutableStateFlow("")

    val toRestoreProfilePosition: MutableStateFlow<Int> = MutableStateFlow(0)
    val toApplyProfilePosition: MutableStateFlow<Int> = MutableStateFlow(0)

    val profilesStateFlow: StateFlow<List<Profile>> = repository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), listOf())

    private var locationId: Int? = null
    private var isGeofenceEnabled: Boolean = false

    fun setLocation(locationRelation: LocationRelation): Unit {
        if (!areArgsSet) {
            val positionsPair: Pair = getPositions(locationRelation)
            toApplyProfilePosition.value = positionsPair.first
            toRestoreProfilePosition.value = positionsPair.second

            latLng.value = LatLng(locationRelation.location.latitude, locationRelation.location.longitude)
            title.value = locationRelation.location.title
            address.value = locationRelation.location.address
            radius.value = locationRelation.location.radius

            locationId = locationRelation.location.id
            isGeofenceEnabled = locationRelation.location.enabled == 1.toByte()
        }
    }

    private fun getPositions(locationRelation: LocationRelation): Pair {
        val pair: Pair= Pair(0, 0)
        for ((index, i) in profilesStateFlow.value.withIndex()) {
            when (i.id) {
                locationRelation.onEnterProfile.id -> {
                    pair.first = index
                }
                locationRelation.onExitProfile.id -> {
                    pair.second = index
                }
            }
        }
        return pair
    }

    private fun getProfileToRestore(profiles: List<Profile>): Profile {
        return profiles[toRestoreProfilePosition.value]
    }

    private fun getProfileToApply(profiles: List<Profile>): Profile {
        return profiles[toApplyProfilePosition.value]
    }

    fun getLocationRelation(profiles: List<Profile>): LocationRelation {
        return LocationRelation (
            getLocation(profiles),
            getProfileToApply(profiles),
            getProfileToRestore(profiles)
        )
    }

    fun getLocation(profiles: List<Profile>): Location {
        val location: Location = Location(
            title = title.value!!,
            latitude = latLng.value!!.latitude,
            longitude = latLng.value!!.longitude,
            address = address.value!!,
            locality = locality.value,
            radius = radius.value,
            onEnterProfileId = getProfileToApply(profiles).id,
            onExitProfileId = getProfileToRestore(profiles).id,
            enabled = if (isGeofenceEnabled) 1 else 0)
        if (locationId != null) {
            location.id = locationId!!
        }
        return location
    }

    fun getLatLng(): LatLng? {
        return latLng.value
    }

    fun getAddress(): String? {
        return address.value
    }

    fun getRadius(): Float {
        return radius.value
    }

    fun setLatLng(latLng: LatLng): Unit {
        this.latLng.value = latLng
    }

    fun setTitle(title: String?): Unit {
        this.title.value = title
    }

    fun setAddress(addressLine: String): Unit {
        address.value = addressLine
    }

    fun setRadius(radius: Float): Unit {
        this.radius.value = radius
    }

    companion object {

        private const val LOG_TAG: String = "MapsSharedViewModel"
    }
}