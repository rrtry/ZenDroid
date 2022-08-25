package ru.rrtry.silentdroid.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import android.util.Log
import ru.rrtry.silentdroid.Application.Companion.ACTION_GEOFENCE_TRANSITION
import ru.rrtry.silentdroid.core.GeofenceManager
import ru.rrtry.silentdroid.core.AppNotificationManager
import ru.rrtry.silentdroid.core.PreferencesManager
import ru.rrtry.silentdroid.core.ProfileManager
import ru.rrtry.silentdroid.db.repositories.LocationRepository
import ru.rrtry.silentdroid.entities.Location
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.receivers.AlarmReceiver.Companion.goAsync
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import ru.rrtry.silentdroid.util.ParcelableUtil.Companion.getExtra
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceReceiver: BroadcastReceiver() {

    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var repository: LocationRepository
    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var appNotificationManager: AppNotificationManager
    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {

            ACTION_GEOFENCE_TRANSITION -> {

                val geofencingEvent: GeofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
                val geofence: Location = getExtra(intent, EXTRA_GEOFENCE)

                if (geofencingEvent.hasError()) {
                    val errorMessage: String = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                    Log.e("GeofenceReceiver", errorMessage)
                    return
                }
                profileManager.onGeofenceTrigger(
                    geofence,
                    geofencingEvent.geofenceTransition,
                    getExtra<Profile>(intent, EXTRA_ENTER_PROFILE),
                    getExtra<Profile>(intent, EXTRA_EXIT_PROFILE)
                )
            }
            ACTION_LOCKED_BOOT_COMPLETED -> {
                goAsync(context!!, GlobalScope, Dispatchers.IO) {
                    geofenceManager.registerGeofences()
                }
            }
            ACTION_BOOT_COMPLETED -> {
                if (Build.VERSION_CODES.N > Build.VERSION.SDK_INT) {
                    goAsync(context!!, GlobalScope, Dispatchers.IO) {
                        geofenceManager.registerGeofences()
                    }
                }
            }
        }
    }

    companion object {

        const val EXTRA_GEOFENCE: String = "extra_geofence"
        const val EXTRA_ENTER_PROFILE: String = "extra_enter_profile"
        const val EXTRA_EXIT_PROFILE: String = "extra_exit_profile"
    }
}