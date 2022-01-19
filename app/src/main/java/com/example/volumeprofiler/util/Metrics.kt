package com.example.volumeprofiler.util

enum class Metrics(private val metricShortName: String, val sliderMinValue: Float, val sliderMaxValue: Float) {

    KILOMETERS("KM", 0.1f,100f), METERS("M", 100f,10000f);

    override fun toString(): String {
        return metricShortName
    }
}