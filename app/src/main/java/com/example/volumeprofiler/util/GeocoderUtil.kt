package com.example.volumeprofiler.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocoderUtil @Inject constructor(
        @ApplicationContext private val context: Context
) {
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    suspend fun getAddressFromLocationName(name: String): Address? {
        return withContext(Dispatchers.IO) {
            try {
                geocoder.getFromLocationName(name, 30)?.let {
                    it[0]
                }
            } catch (e: IOException) {
                Log.e("GeocoderUtil", "getAddressFromLocationName", e)
                null
            }
        }
    }

    suspend fun getAddressListFromLocationName(name: String): List<AddressWrapper>? {
        return withContext(Dispatchers.IO) {
            Log.i("GeocoderUtil", "getAddressListFromLocationName: $name")
            geocoder.getFromLocationName(name, 30)?.map {
                Log.i("GeocoderUtil", "getAddressListFromLocationName: $it")
                AddressWrapper(
                    it.latitude,
                    it.longitude,
                    it.getAddressLine(0)
                )
            }
        }
    }

    suspend fun getAddressFromLatLng(latLng: LatLng): Address? {
        return withContext(Dispatchers.IO) {
            try {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 30)?.let {
                    if (it.isNotEmpty()) {
                        it[0]
                    }
                }
                null
            } catch (e: IOException) {
                Log.e("GeocoderUtil", "getAddressFromLatLng", e)
                null
            }
        }
    }

    companion object {

        fun isPresent(): Boolean {
            return Geocoder.isPresent()
        }
    }
}