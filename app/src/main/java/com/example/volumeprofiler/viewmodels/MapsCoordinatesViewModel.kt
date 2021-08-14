package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.ViewModel
import com.example.volumeprofiler.util.Metrics

class MapsCoordinatesViewModel: ViewModel() {

    var metrics: Metrics = Metrics.METERS
    var radius: String = "100"
    var address: String? = null
    var latitude: String? = null
    var longitude: String? = null

}