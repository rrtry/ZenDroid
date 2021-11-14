package com.example.volumeprofiler.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.Application.Companion.ACTION_GEOFENCE_TRANSITION
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_GEOFENCE_TRANSITION) {

            val geofencingEvent: GeofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent.hasError()) {
                Log.i("GeofenceReceiver", "hasError")
                val errorMessage: String = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                return
            }

            val address: String = intent.getStringExtra(EXTRA_ADDRESS)!!
            when (geofencingEvent.geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    val profile: Profile = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_ENTER_PROFILE)!!, ParcelableUtil.getParcelableCreator())
                    postNotification(context!!, createGeofenceEnterNotification(context!!, profile.title, address), ID_GEOFENCE)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    val profile: Profile = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_ENTER_PROFILE)!!, ParcelableUtil.getParcelableCreator())
                    postNotification(context!!, createGeofenceExitNotification(context, profile.title, address), ID_GEOFENCE)
                }
                Geofence.GEOFENCE_TRANSITION_DWELL -> {

                }
            }
        }
    }

    companion object {

        private const val TAG: String = "GeofenceReceiver"
        const val EXTRA_ADDRESS: String = "extra_address"
        const val EXTRA_ENTER_PROFILE: String = "extra_enter_profile"
        const val EXTRA_EXIT_PROFILE: String = "extra_exit_profile"
    }
}