package com.example.volumeprofiler.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import android.util.Log
import com.example.volumeprofiler.Application.Companion.ACTION_GEOFENCE_TRANSITION
import com.example.volumeprofiler.database.repositories.LocationRepository
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.receivers.AlarmReceiver.Companion.goAsync
import com.example.volumeprofiler.util.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceReceiver: BroadcastReceiver() {

    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var repository: LocationRepository
    @Inject lateinit var geofenceManager: GeofenceManager

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {

            val context: Context = context!!

            when (it.action) {

                ACTION_GEOFENCE_TRANSITION -> {
                    val geofencingEvent: GeofencingEvent = GeofencingEvent.fromIntent(intent)
                    if (geofencingEvent.hasError()) {
                        Log.i(toString().javaClass.simpleName, "hasError")
                        val errorMessage: String = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                        Log.e(toString(), errorMessage)
                        return
                    }

                    val title: String = intent.getStringExtra(EXTRA_TITLE)!!
                    lateinit var profile: Profile

                    when (geofencingEvent.geofenceTransition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER, Geofence.GEOFENCE_TRANSITION_DWELL -> {
                            profile = ParcelableUtil.toParcelable(
                                intent.getByteArrayExtra(EXTRA_ENTER_PROFILE)!!,
                                ParcelableUtil.getParcelableCreator())
                            postNotification(context, createGeofenceEnterNotification(context, profile.title, title), ID_GEOFENCE)
                        }
                        Geofence.GEOFENCE_TRANSITION_EXIT -> {
                            profile = ParcelableUtil.toParcelable(
                                intent.getByteArrayExtra(EXTRA_EXIT_PROFILE)!!,
                                ParcelableUtil.getParcelableCreator())
                            postNotification(context, createGeofenceExitNotification(context, profile.title, title), ID_GEOFENCE)
                        }
                    }
                    profileManager.setProfile(profile)
                }
                ACTION_LOCKED_BOOT_COMPLETED -> {
                    goAsync(context, GlobalScope, Dispatchers.IO) {
                        registerGeofences()
                    }
                }
                ACTION_BOOT_COMPLETED -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        goAsync(context, GlobalScope, Dispatchers.IO) {
                            registerGeofences()
                        }
                    }
                }
                ACTION_PACKAGE_DATA_CLEARED -> {
                    if (it.dataString == "package:com.google.android.gms") {
                        goAsync(context, GlobalScope, Dispatchers.IO) {
                            registerGeofences()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun registerGeofences() {
        val geofences: List<LocationRelation> = repository.getLocations()
        if (geofences.isNotEmpty()) {
            for (i in geofences) {
                if (i.location.enabled == 1.toByte()) {
                    geofenceManager.addGeofence(
                        i.location,
                        i.onEnterProfile,
                        i.onExitProfile
                    )
                }
            }
        }
    }

    companion object {

        const val EXTRA_TITLE: String = "extra_title"
        const val EXTRA_ENTER_PROFILE: String = "extra_enter_profile"
        const val EXTRA_EXIT_PROFILE: String = "extra_exit_profile"
    }
}