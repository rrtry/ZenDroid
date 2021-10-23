package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import com.example.volumeprofiler.util.Metrics
import com.example.volumeprofiler.util.TextUtil
import kotlinx.coroutines.flow.MutableStateFlow

class MapsCoordinatesViewModel: ViewModel() {

    val longitudeEditStatus: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val latitudeEditStatus: MutableStateFlow<Boolean> = MutableStateFlow(true)

    val metrics: MutableStateFlow<Metrics> = MutableStateFlow(Metrics.METERS)

    fun validateLongitudeInput(source: CharSequence?) {
        longitudeEditStatus.value = TextUtil.validateCoordinatesInput(source)
    }

    fun validateLatitudeInput(source: CharSequence?) {
        latitudeEditStatus.value = TextUtil.validateCoordinatesInput(source)
    }

    fun setMetrics(m: Metrics) {
        metrics.value = m
    }
}