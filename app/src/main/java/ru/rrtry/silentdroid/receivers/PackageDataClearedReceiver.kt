package ru.rrtry.silentdroid.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PACKAGE_DATA_CLEARED
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import ru.rrtry.silentdroid.core.GeofenceManager
import ru.rrtry.silentdroid.receivers.AlarmReceiver.Companion.goAsync
import javax.inject.Inject

@AndroidEntryPoint
class PackageDataClearedReceiver: BroadcastReceiver() {

    @Inject lateinit var geofenceManager: GeofenceManager

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_PACKAGE_DATA_CLEARED &&
            intent.dataString == "package:com.google.android.gms"
        ) {
            goAsync(context!!, GlobalScope, Dispatchers.IO) {
                geofenceManager.registerGeofences()
            }
        }
    }
}