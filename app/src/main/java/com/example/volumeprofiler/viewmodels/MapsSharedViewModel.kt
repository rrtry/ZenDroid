package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.example.volumeprofiler.database.Repository

class MapsSharedViewModel: ViewModel() {

    val repository: Repository = Repository.get()
    val latLng: SingleLiveEvent<LatLng> = SingleLiveEvent()
    val addressLine: MutableLiveData<String> = SingleLiveEvent()
    val bottomSheetState: MutableLiveData<Int> = SingleLiveEvent()
    val radius: MutableLiveData<Float> = SingleLiveEvent()

    var animateCameraMovement: Boolean = false

    companion object {
        private const val LOG_TAG: String = "MapsSharedViewModel"
    }
}