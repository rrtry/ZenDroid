package com.example.volumeprofiler.entities

import androidx.room.Entity

@Entity(primaryKeys = ["latitude", "longitude"])
data class LocationSuggestion(
    val address: String,
    val latitude: Double,
    val longitude: Double)