package com.example.volumeprofiler.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import androidx.core.content.contentValuesOf
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.inject.Inject

@ActivityScoped
class GeocoderUtil @Inject constructor(
        @ActivityContext private val context: Context
) {
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    suspend fun getAddressFromLocationName(name: String): List<Address>? {
        return withContext(Dispatchers.IO) {
            try {
                val result = geocoder.getFromLocationName(name, 15)
                Log.i("GeocoderUtil", "result array size: ${result.size}")
                result
            } catch (e: IOException) {
                null
            }
        }
    }

    suspend fun getLatLngFromAddress(address: String): LatLng? {
        return withContext(Dispatchers.IO) {
            try {
                val addressList: List<Address>? = geocoder.getFromLocationName(address, 15)
                if (addressList != null && addressList.isNotEmpty()) {
                    val address: Address = addressList[0]
                    Log.i("GeocoderUtil", "${address.latitude}, ${address.longitude}")
                    Log.i("GeocoderUtil", "addressList size: ${addressList.size}")
                    Log.i("GeocoderUtil", address.getAddressLine(0))
                    LatLng(address.latitude, address.longitude)
                } else {
                    null
                }
            } catch (e: IOException) {
                null
            }
        }
    }

    suspend fun getAddressFromLatLng(latLng: LatLng): String? {
        return withContext(Dispatchers.IO) {
            try {
                val addressList: MutableList<Address>? = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 30)
                if (addressList != null && addressList.isNotEmpty()) {
                    val address: Address = addressList[0]
                    val addressLine: String = address.getAddressLine(0)
                    addressLine
                } else {
                    null
                }
            } catch (e: IOException) {
                null
            }
        }
    }
}