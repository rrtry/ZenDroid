package com.example.volumeprofiler.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.Application.Companion.ACTION_GEOFENCE_TRANSITION
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_GEOFENCE_TRANSITION) {

            val geofencingEvent: GeofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent.hasError()) {
                val errorMessage: String = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                return
            }

            when (geofencingEvent.geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.i("GeofenceReceiver", "GEOFENCE_TRANSITION_ENTER")
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.i("GeofenceReceiver", "GEOFENCE_TRANSITION_EXIT")
                }
                else -> {
                    Log.i("GeofenceReceiver", "different transition type")
                }
            }
        }
    }

    companion object {

        private const val TAG: String = "GeofenceReceiver"
        const val EXTRA_LOCATION_TRIGGER: String = "extra_location_trigger"
    }
}