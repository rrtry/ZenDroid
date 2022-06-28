package com.example.volumeprofiler.util

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import kotlin.math.abs
import kotlin.math.sqrt

object MapsUtil {

    fun isLatitude(string: String): Boolean {
        string.toDoubleOrNull()?.let {
            return it.isFinite() && abs(it) <= 90
        }
        return false
    }

    fun isLongitude(string: String): Boolean {
        string.toDoubleOrNull()?.let {
            return it.isFinite() && abs(it) <= 180
        }
        return false
    }

    fun getLatLngBoundsFromCircle(latLng: LatLng, radius: Float): LatLngBounds {

        val targetNorthEast: LatLng = SphericalUtil.computeOffset(latLng, radius * sqrt(2.0), 45.0)
        val targetSouthWest: LatLng = SphericalUtil.computeOffset(latLng,radius * sqrt(2.0), 225.0)

        return LatLngBounds.Builder()
            .include(targetNorthEast)
            .include(targetSouthWest)
            .build()
    }

    fun getLatLngBoundsFromCircle(circle: Circle): LatLngBounds {

        val targetNorthEast: LatLng = SphericalUtil.computeOffset(circle.center, circle.radius * sqrt(2.0), 45.0)
        val targetSouthWest: LatLng = SphericalUtil.computeOffset(circle.center, circle.radius * sqrt(2.0), 225.0)

        return LatLngBounds.Builder()
            .include(targetNorthEast)
            .include(targetSouthWest)
            .build()
    }

    fun isTargetWithinVisibleRegion(map: GoogleMap, target: LatLng): Boolean {
        return map.projection.visibleRegion.latLngBounds.contains(target)
    }
}