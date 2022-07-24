package ru.rrtry.silentdroid.util

enum class Metrics(private val shortName: String, val min: Float, val max: Float) {

    KILOMETERS("KM", 0.1f,100f), METERS("M", 100f,10000f);

    override fun toString(): String {
        return shortName
    }
}