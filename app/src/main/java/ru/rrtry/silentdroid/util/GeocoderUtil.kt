package ru.rrtry.silentdroid.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocoderUtil @Inject constructor(@ApplicationContext private val context: Context) {

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    suspend fun queryAddresses(query: String?): List<AddressWrapper>? {
        if (query.isNullOrEmpty() || query.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                geocoder.getFromLocationName(query, 30)?.map {
                    AddressWrapper(
                        it.latitude,
                        it.longitude,
                        it.getAddressLine(0),
                        false
                    )
                }
            } catch (e: IOException) {
                null
            }
        }
    }

    suspend fun getAddressFromLocation(latLng: LatLng?): String? {
        if (latLng == null) return null
        return withContext(Dispatchers.IO) {
            try {
                val results: List<Address>? = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (results.isNullOrEmpty()) null else results.first().getAddressLine(0)
            }
            catch (e: IOException) {
                null
            }
        }
    }
}