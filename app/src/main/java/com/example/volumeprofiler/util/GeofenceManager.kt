package com.example.volumeprofiler.util

import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.volumeprofiler.Application.Companion.ACTION_GEOFENCE_TRANSITION
import com.example.volumeprofiler.receivers.GeofenceReceiver
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.Profile
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.Manifest.permission.*
import android.annotation.SuppressLint

@Singleton
class GeofenceManager @Inject constructor(
        @ApplicationContext private val context: Context
) {

    interface LocationRequestListener {

        fun onSuccess()

        fun onFailure()

    }

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    @TargetApi(Build.VERSION_CODES.Q)
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
    fun addGeofence(location: Location, enterProfile: Profile, exitProfile: Profile): Unit {
        geofencingClient.addGeofences(getGeofencingRequest(listOf(location)), createGeofencingPendingIntent(location, enterProfile, exitProfile))
                .addOnSuccessListener {
                    Log.i("GeofenceUtil", "successfully added geofence")
                }
                .addOnFailureListener {
                    Log.e("GeofenceUtil", "an error happened while adding geofence", it)
                }
    }

    fun removeGeofence(location: Location, enterProfile: Profile, exitProfile: Profile): Unit {
        val pendingIntent: PendingIntent? = getGeofencingPendingIntent(location, enterProfile, exitProfile)
        if (pendingIntent != null) {
            geofencingClient.removeGeofences(pendingIntent)
                .addOnSuccessListener {
                    Log.i("GeofenceUtil", "removeGeofence: success")
                }
                .addOnFailureListener {
                    Log.i("GeofenceUtil", "removeGeofence: failure")
                }
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

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createGeofencingPendingIntent(
        location: Location,
        enterProfile: Profile,
        exitProfile: Profile
    ): PendingIntent {
        val intent: Intent = Intent(context, GeofenceReceiver::class.java).apply {
            action = ACTION_GEOFENCE_TRANSITION
            putExtra(GeofenceReceiver.EXTRA_TITLE, location.title)
            putExtra(GeofenceReceiver.EXTRA_ENTER_PROFILE, ParcelableUtil.toByteArray(enterProfile))
            putExtra(GeofenceReceiver.EXTRA_EXIT_PROFILE, ParcelableUtil.toByteArray(exitProfile))
        }
        return PendingIntent.getBroadcast(context, location.id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getGeofencingPendingIntent(location: Location,enterProfile: Profile, exitProfile: Profile): PendingIntent? {
        val intent: Intent = Intent(context, GeofenceReceiver::class.java).apply {
            action = ACTION_GEOFENCE_TRANSITION
            putExtra(GeofenceReceiver.EXTRA_TITLE, location.title)
            putExtra(GeofenceReceiver.EXTRA_ENTER_PROFILE, ParcelableUtil.toByteArray(enterProfile))
            putExtra(GeofenceReceiver.EXTRA_EXIT_PROFILE, ParcelableUtil.toByteArray(exitProfile))
        }
        return PendingIntent.getBroadcast(context, location.id, intent, PendingIntent.FLAG_NO_CREATE)
    }

    fun checkLocationServicesAvailability(activity: Activity) {
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
                    Log.e("GeofenceManager", "Error geting location settings resolution: " + sendEx.message)
                }
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            (activity as LocationRequestListener).also { listener ->
                if (it.isSuccessful) {
                    listener.onSuccess()
                } else {
                    listener.onFailure()
                }
            }
        }
    }

    companion object {

        const val REQUEST_ENABLE_LOCATION_SERVICES: Int = 182

    }
}