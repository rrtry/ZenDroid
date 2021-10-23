package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.models.Profile
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MapsSharedViewModel @Inject constructor(
        private val repository: ProfileRepository
): ViewModel() {

    val latLng: MutableStateFlow<LatLng?> = MutableStateFlow(null)
    val address: MutableStateFlow<String?> = MutableStateFlow(null)
    val radius: MutableStateFlow<Float> = MutableStateFlow(100f)

    val toRestoreProfileId: MutableStateFlow<UUID?> = MutableStateFlow(null)
    val toApplyProfileId: MutableStateFlow<UUID?> = MutableStateFlow(null)
    val profilesFlow: StateFlow<List<Profile>> = repository.observeProfiles().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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