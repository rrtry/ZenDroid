package com.example.volumeprofiler.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.volumeprofiler.services.GeofenceRegistrationService

class PackageDataClearedReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_PACKAGE_DATA_CLEARED
                && intent.getIntExtra(Intent.EXTRA_UID, -1) == G_SERVICES_UID) {
            startService(context!!)
        }
    }

    private fun startService(context: Context): Unit {
        val intent: Intent = Intent(context, GeofenceRegistrationService::class.java)
        if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    companion object {

        private const val G_SERVICES_UID: Int = 10014
    }
}