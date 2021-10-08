package com.example.volumeprofiler.util

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.volumeprofiler.Application.Companion.ACTION_GEOFENCE_TRANSITION
import com.example.volumeprofiler.broadcastReceivers.GeofenceReceiver
import com.example.volumeprofiler.models.Location
import com.example.volumeprofiler.models.LocationRelation
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceUtil @Inject constructor(
        @ApplicationContext private val context: Context
) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun addGeofence(locationRelation: LocationRelation): Unit {
        val location: Location = locationRelation.location
        geofencingClient.addGeofences(getGeofencingRequest(listOf(location)), getGeofencingPendingIntent(locationRelation)!!)
                .addOnSuccessListener {
                    Log.i("GeofenceUtil", "successfully added geofence")
                }
                .addOnFailureListener {
                    Log.e("GeofenceUtil", "an error happened while adding geofence", it)
                }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun removeGeofence(locationRelation: LocationRelation): Unit {
        val pendingIntent: PendingIntent? = getGeofencingPendingIntent(locationRelation, false)
        if (pendingIntent != null) {
            geofencingClient.removeGeofences(pendingIntent)
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

    private fun getGeofencingPendingIntent(locationRelation: LocationRelation, create: Boolean = true): PendingIntent? {
        val intent: Intent = Intent(context, GeofenceReceiver::class.java).apply {
            action = ACTION_GEOFENCE_TRANSITION
            putExtra(GeofenceReceiver.EXTRA_LOCATION_TRIGGER, locationRelation)
        }
        return PendingIntent.getBroadcast(context, locationRelation.location.id, intent, if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
    }
}