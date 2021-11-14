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
                result
            } catch (e: IOException) {
                Log.e("GeocoderUtil", "getAddressFromLocationName", e)
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
                    LatLng(address.latitude, address.longitude)
                } else {
                    null
                }
            } catch (e: IOException) {
                Log.e("GeocoderUtil", "getLatLngFromAddress", e)
                null
            }
        }
    }

    suspend fun getAddressFromLatLng(latLng: LatLng): Address? {
        return withContext(Dispatchers.IO) {
            try {
                val addressList: MutableList<Address>? = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 30)
                if (addressList != null && addressList.isNotEmpty()) {
                    addressList[0]
                } else {
                    null
                }
            } catch (e: IOException) {
                Log.e("GeocoderUtil", "getAddressFromLatLng", e)
                null
            }
        }
    }
}