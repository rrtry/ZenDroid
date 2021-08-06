package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.volumeprofiler.models.Profile
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.volumeprofiler.database.Repository

class MapsSharedViewModel: ViewModel() {

    val repository: Repository = Repository.get()
    val latLng: MutableLiveData<LatLng> = MutableLiveData()
    var addressLine: MutableLiveData<String> = MutableLiveData()

    val profileListLiveData: LiveData<List<Profile>> by lazy { repository.observeProfiles() }
    var onExitProfile: Profile? = null
    var onEnterProfile: Profile? = null

    var animateCameraMovement: Boolean = false
    var bottomSheetState: MutableLiveData<Int> = MutableLiveData(BottomSheetBehavior.STATE_COLLAPSED)

    override fun onCleared() {
        Log.i(LOG_TAG, "clearing $LOG_TAG")
        super.onCleared()
    }

    companion object {
        private const val LOG_TAG: String = "MapsSharedViewModel"
    }
}