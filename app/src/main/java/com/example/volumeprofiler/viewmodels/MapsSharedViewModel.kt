package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.Profile
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.*

class MapsSharedViewModel: ViewModel() {

    val repository: Repository = Repository.get()

    val latLng: MutableLiveData<EventWrapper<LatLng>> = MutableLiveData()
    val addressLine: MutableLiveData<EventWrapper<String>> = MutableLiveData()
    private val bottomSheetState: MutableLiveData<EventWrapper<Int>> = MutableLiveData(EventWrapper(BottomSheetBehavior.STATE_COLLAPSED))
    val radius: MutableLiveData<EventWrapper<Float>> = MutableLiveData(EventWrapper(100f))


    var animateCameraMovement: Boolean = false

    fun getLatLng(): LatLng? = latLng.value?.peekContent()

    fun getAddressLine(): String? = this.addressLine.value?.peekContent()

    fun getBottomSheetState(): Int? = this.bottomSheetState.value?.peekContent()

    fun getRadius(): Float? = this.radius.value?.peekContent()

    fun setLatLng(latLng: LatLng): Unit {
        this.latLng.value = EventWrapper(latLng)
    }

    fun setAddressLine(addressLine: String): Unit {
        this.addressLine.value = EventWrapper(addressLine, 1)
    }

    fun setBottomSheetState(state: Int): Unit {
        this.bottomSheetState.value = EventWrapper(state)
    }

    fun setRadius(radius: Float): Unit {
        this.radius.value = EventWrapper(radius)
    }

    companion object {
        private const val LOG_TAG: String = "MapsSharedViewModel"
    }
}