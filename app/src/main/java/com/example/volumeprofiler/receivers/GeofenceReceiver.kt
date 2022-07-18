package com.example.volumeprofiler.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import android.util.Log
import com.example.volumeprofiler.Application.Companion.ACTION_GEOFENCE_TRANSITION
import com.example.volumeprofiler.core.GeofenceManager
import com.example.volumeprofiler.core.NotificationHelper
import com.example.volumeprofiler.core.PreferencesManager
import com.example.volumeprofiler.core.PreferencesManager.Companion.TRIGGER_TYPE_GEOFENCE_ENTER
import com.example.volumeprofiler.core.PreferencesManager.Companion.TRIGGER_TYPE_GEOFENCE_EXIT
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.db.repositories.LocationRepository
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.receivers.AlarmReceiver.Companion.goAsync
import com.example.volumeprofiler.util.*
import com.google.android.gms.location.Geofence.*
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
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {

            ACTION_GEOFENCE_TRANSITION -> {

                val geofencingEvent: GeofencingEvent = GeofencingEvent.fromIntent(intent)
                if (geofencingEvent.hasError()) {
                    val errorMessage: String = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                    Log.e("GeofenceReceiver", errorMessage)
                    return
                }

                val title: String = intent.getStringExtra(EXTRA_TITLE)!!
                val geofence: Location = getExtra(intent, EXTRA_GEOFENCE)

                when (geofencingEvent.geofenceTransition) {
                    GEOFENCE_TRANSITION_ENTER, GEOFENCE_TRANSITION_DWELL -> {
                        getExtra<Profile>(intent, EXTRA_ENTER_PROFILE).also {
                            profileManager.setProfile(it, TRIGGER_TYPE_GEOFENCE_ENTER, geofence)
                            notificationHelper.postGeofenceEnterNotification(
                                it.title, title
                            )
                        }
                    }
                    GEOFENCE_TRANSITION_EXIT -> {
                        getExtra<Profile>(intent, EXTRA_EXIT_PROFILE).also {
                            profileManager.setProfile(it, TRIGGER_TYPE_GEOFENCE_EXIT, geofence)
                            notificationHelper.postGeofenceExitNotification(
                                it.title, title
                            )
                        }
                    }
                }
            }
            ACTION_LOCKED_BOOT_COMPLETED -> {
                goAsync(context!!, GlobalScope, Dispatchers.IO) {
                    registerGeofences()
                }
            }
            ACTION_BOOT_COMPLETED -> {
                if (Build.VERSION_CODES.N > Build.VERSION.SDK_INT) {
                    goAsync(context!!, GlobalScope, Dispatchers.IO) {
                        registerGeofences()
                    }
                }
            }
            ACTION_PACKAGE_DATA_CLEARED -> {
                if (intent.dataString == "package:com.google.android.gms") {
                    goAsync(context!!, GlobalScope, Dispatchers.IO) {
                        registerGeofences()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun registerGeofences() {
        repository.getLocations().forEach {
            if (it.location.enabled) {
                geofenceManager.addGeofence(
                    it.location,
                    it.onEnterProfile,
                    it.onExitProfile
                )
            }
        }
    }

    companion object {

        fun <T> getExtra(intent: Intent, name: String): T {
            return ParcelableUtil.toParcelable(
                intent.getByteArrayExtra(name)!!,
                ParcelableUtil.getParcelableCreator())
        }

        const val EXTRA_TITLE: String = "extra_title"
        const val EXTRA_GEOFENCE: String = "extra_geofence"
        const val EXTRA_ENTER_PROFILE: String = "extra_enter_profile"
        const val EXTRA_EXIT_PROFILE: String = "extra_exit_profile"
    }
}