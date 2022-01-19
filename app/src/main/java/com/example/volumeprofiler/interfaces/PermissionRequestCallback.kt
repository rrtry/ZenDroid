package com.example.volumeprofiler.interfaces

import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.entities.Profile

interface PermissionRequestCallback {

    fun requestProfilePermissions(profile: Profile): Unit

    fun requestLocationPermissions(locationRelation: LocationRelation): Unit
}