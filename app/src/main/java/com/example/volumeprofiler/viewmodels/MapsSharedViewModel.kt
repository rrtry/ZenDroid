package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@HiltViewModel
class MapsSharedViewModel @Inject constructor(): ViewModel() {

    val latLng: MutableStateFlow<LatLng?> = MutableStateFlow(null)
    val address: MutableStateFlow<String?> = MutableStateFlow(null)
    val radius: MutableStateFlow<Float> = MutableStateFlow(100f)

    var animateMovement: Boolean = false

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

    fun setBottomSheetState(state: Int): Unit {

    }

    fun setRadius(radius: Float): Unit {
        this.radius.value = radius
    }

    companion object {
        private const val LOG_TAG: String = "MapsSharedViewModel"
    }
}