package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import com.example.volumeprofiler.util.Metrics
import com.example.volumeprofiler.util.TextUtil

class MapsCoordinatesViewModel: ViewModel() {

    var metrics: Metrics = Metrics.METERS
    var radius: String = "100"
    var address: String? = null
    var latitude: String? = null
    var longitude: String? = null

    fun validateCoordinatesInput(source: CharSequence?): Boolean {
        return TextUtil.validateCoordinatesInput(source)
    }

    fun filterCoordinatesInput(source: CharSequence?): CharSequence {
        return TextUtil.filterCoordinatesInput(source)
    }

    fun filterStreetAddressInput(source: CharSequence?): CharSequence {
        return TextUtil.filterStreetAddressInput(source)
    }

    fun filterRadiusInput(source: CharSequence?): CharSequence {
        return TextUtil.filterRadiusInput(source)
    }

    fun validateRadiusInput(s: CharSequence?, currentMetric: Metrics): Boolean {
        return TextUtil.validateRadiusInput(s, currentMetric)
    }
}