package ru.rrtry.silentdroid.core

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import ru.rrtry.silentdroid.Application.Companion.ACTION_GEOFENCE_TRANSITION
import ru.rrtry.silentdroid.receivers.GeofenceReceiver
import ru.rrtry.silentdroid.entities.Location
import ru.rrtry.silentdroid.entities.Profile
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.PendingIntent.*
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.tasks.RuntimeExecutionException
import ru.rrtry.silentdroid.db.repositories.LocationRepository
import ru.rrtry.silentdroid.entities.LocationRelation
import ru.rrtry.silentdroid.receivers.GeofenceReceiver.Companion.EXTRA_ENTER_PROFILE
import ru.rrtry.silentdroid.receivers.GeofenceReceiver.Companion.EXTRA_EXIT_PROFILE
import ru.rrtry.silentdroid.receivers.GeofenceReceiver.Companion.EXTRA_GEOFENCE
import ru.rrtry.silentdroid.util.ParcelableUtil
import ru.rrtry.silentdroid.util.checkPermission

@Singleton
class GeofenceManager @Inject constructor(
        @ApplicationContext private val context: Context,
        private val repository: LocationRepository
) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    interface LocationRequestListener {

        fun onLocationRequestSuccess()

        fun onLocationRequestFailure()

    }

    suspend fun registerGeofences() {
        repository.getLocations().filter {
            it.location.enabled
        }.forEach {
            addGeofence(
                it.location,
                it.onEnterProfile,
                it.onExitProfile
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun updateGeofenceProfile(registeredGeofences: List<LocationRelation>?, profile: Profile) {
        registeredGeofences?.forEach { locationRelation ->
            if (locationRelation.onEnterProfile.id == profile.id) {
                addGeofence(
                    locationRelation.location,
                    profile,
                    locationRelation.onExitProfile
                )
            } else {
                addGeofence(
                    locationRelation.location,
                    locationRelation.onEnterProfile,
                    profile
                )
            }
        }
    }

    fun locationAccessGranted(): Boolean {
        val foregroundLocationApproved: Boolean = context.checkPermission(ACCESS_FINE_LOCATION)
        val backgroundLocationApproved: Boolean = if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            context.checkPermission(ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        }
        return foregroundLocationApproved && backgroundLocationApproved
    }

    @RequiresPermission(ACCESS_FINE_LOCATION)
    fun addGeofence(location: Location, enterProfile: Profile, exitProfile: Profile) {
        geofencingClient.addGeofences(getGeofencingRequest(listOf(location)), createGeofencingPendingIntent(location, enterProfile, exitProfile))
                .addOnSuccessListener {
                    Log.i("GeofenceManager", "successfully added geofence")
                }
                .addOnFailureListener {
                    Log.e("GeofenceManager", "an error happened while adding geofence", it)
                }
    }

    fun removeGeofence(location: Location, enterProfile: Profile, exitProfile: Profile) {
        val pendingIntent: PendingIntent = getGeofencePendingIntent(location, enterProfile, exitProfile) ?: return
        geofencingClient.removeGeofences(pendingIntent)
            .addOnSuccessListener {
                Log.i("GeofenceManager", "removeGeofence: success")
            }
            .addOnFailureListener {
                Log.i("GeofenceManager", "removeGeofence: failure")
            }
    }

    private fun buildGeofence(location: Location): Geofence {
        return Geofence.Builder()
                .setRequestId(location.id.toString())
                .setCircularRegion(
                        location.latitude,
                        location.longitude,
                        location.radius
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
    }

    private fun getGeofencingRequest(geofenceList: List<Location>): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList.map { buildGeofence(it) })
        }.build()
    }

    private fun createGeofencingPendingIntent(
        location: Location,
        enterProfile: Profile,
        exitProfile: Profile
    ): PendingIntent {
        Intent(context, GeofenceReceiver::class.java).apply {

            action = ACTION_GEOFENCE_TRANSITION
            putExtra(EXTRA_GEOFENCE, ParcelableUtil.toByteArray(location))
            putExtra(EXTRA_ENTER_PROFILE, ParcelableUtil.toByteArray(enterProfile))
            putExtra(EXTRA_EXIT_PROFILE, ParcelableUtil.toByteArray(exitProfile))

            return getBroadcast(context, location.id, this, FLAG_UPDATE_CURRENT or FLAG_MUTABLE)
        }
    }

    private fun getGeofencePendingIntent(location: Location, enterProfile: Profile, exitProfile: Profile): PendingIntent? {
        Intent(context, GeofenceReceiver::class.java).apply {

            action = ACTION_GEOFENCE_TRANSITION
            putExtra(EXTRA_GEOFENCE, ParcelableUtil.toByteArray(location))
            putExtra(EXTRA_ENTER_PROFILE, ParcelableUtil.toByteArray(enterProfile))
            putExtra(EXTRA_EXIT_PROFILE, ParcelableUtil.toByteArray(exitProfile))

            return getBroadcast(context, location.id, this, FLAG_NO_CREATE or FLAG_MUTABLE)
        }
    }

    fun requestLocationPermission(launcher: ActivityResultLauncher<Array<String>>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            launcher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION))
        } else {
            launcher.launch(arrayOf(ACCESS_FINE_LOCATION))
        }
    }

    fun checkLocationServicesAvailability(activity: Activity) {

        val listener = activity as LocationRequestListener
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(activity as Activity)
        val locationSettingsResponseTask: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(
                        activity,
                        REQUEST_ENABLE_LOCATION_SERVICES
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e("GeofenceManager", "Error getting location settings resolution: " + sendEx.message)
                }
            }
        }
        locationSettingsResponseTask.addOnSuccessListener {
            listener.onLocationRequestSuccess()
        }
    }

    companion object {

        val ACCESS_LOCATION: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ACCESS_BACKGROUND_LOCATION
        } else {
            ACCESS_FINE_LOCATION
        }

        const val REQUEST_ENABLE_LOCATION_SERVICES: Int = 182
    }
}