package ru.rrtry.silentdroid.core

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.rrtry.silentdroid.util.AddressWrapper
import java.io.IOException
import java.util.*
import javax.inject.Inject

@ActivityScoped
class AppGeocoder @Inject constructor(@ActivityContext private val context: Context) {

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    suspend fun queryAddresses(query: String?): List<AddressWrapper>? {
        if (query.isNullOrEmpty() || query.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                geocoder.getFromLocationName(query, 30)?.map { address ->
                    AddressWrapper(
                        address.latitude,
                        address.longitude,
                        address.getAddressLine(0),
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