package com.example.volumeprofiler.util

import kotlin.math.abs

object GeoUtil {

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
}