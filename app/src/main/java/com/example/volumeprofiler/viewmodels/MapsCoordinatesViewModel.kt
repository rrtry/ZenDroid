package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.volumeprofiler.util.Metrics
import com.example.volumeprofiler.util.TextUtil

class MapsCoordinatesViewModel: ViewModel() {

    private val longitudeEditStatus: MutableLiveData<Boolean> = MutableLiveData(true)
    private val latitudeEditStatus: MutableLiveData<Boolean> = MutableLiveData(true)
    private val addressEditState: MutableLiveData<Boolean> = MutableLiveData(true)
    val observableLongitudeEditStatus: LiveData<Boolean> get() = longitudeEditStatus
    val observableLatitudeEditStatus: LiveData<Boolean> get() = latitudeEditStatus
    val observableAddressEditState: LiveData<Boolean> get() = addressEditState

    private val metrics: MutableLiveData<Metrics> = MutableLiveData(Metrics.METERS)
    val observableMetrics: LiveData<Metrics> get() = metrics

    fun validateLongitudeInput(source: CharSequence?) {
        longitudeEditStatus.value = TextUtil.validateCoordinatesInput(source)
    }

    fun validateLatitudeInput(source: CharSequence?) {
        latitudeEditStatus.value = TextUtil.validateCoordinatesInput(source)
    }

    fun validateAddressInput(source: CharSequence?) {
        addressEditState.value = TextUtil.validateAddressInput(source)
    }

    fun setMetrics(m: Metrics) {
        metrics.value = m
    }
}