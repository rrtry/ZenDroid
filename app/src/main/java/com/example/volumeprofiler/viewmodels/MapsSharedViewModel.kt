package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.example.volumeprofiler.database.Repository
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MapsSharedViewModel: ViewModel() {

    val repository: Repository = Repository.get()
    val latLng: MutableLiveData<Event<LatLng>> = MutableLiveData()
    val addressLine: MutableLiveData<Event<String>> = MutableLiveData()
    val bottomSheetState: MutableLiveData<Event<Int>> = MutableLiveData(Event(BottomSheetBehavior.STATE_COLLAPSED))
    val radius: MutableLiveData<Event<Float>> = MutableLiveData(Event(100f))
    var animateCameraMovement: Boolean = false

    fun getLatLng(): LatLng? = latLng.value?.peekContent()

    fun getAddressLine(): String? = this.addressLine.value?.peekContent()

    fun getBottomSheetState(): Int? = this.bottomSheetState.value?.peekContent()

    fun getRadius(): Float? = this.radius.value?.peekContent()

    fun setLatLng(latLng: LatLng): Unit {
        this.latLng.value = Event(latLng)
    }

    fun setAddressLine(addressLine: String): Unit {
        this.addressLine.value = Event(addressLine)
    }

    fun setBottomSheetState(state: Int): Unit {
        this.bottomSheetState.value = Event(state)
    }

    fun setRadius(radius: Float): Unit {
        this.radius.value = Event(radius)
    }

    companion object {
        private const val LOG_TAG: String = "MapsSharedViewModel"
    }
}