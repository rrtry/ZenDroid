package com.example.volumeprofiler.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.volumeprofiler.services.GeofenceRegistrationService
import com.example.volumeprofiler.services.SchedulerService

class BootCompletedReceiver: BroadcastReceiver() {

    private fun startService(context: Context, service: Class<*>): Unit {
        val intent: Intent = Intent(context, service)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?): Unit {
        if (intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            startService(context!!, SchedulerService::class.java)
            startService(context, GeofenceRegistrationService::class.java)
        }
        else if (intent?.action == Intent.ACTION_BOOT_COMPLETED && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            startService(context!!, SchedulerService::class.java)
            startService(context, GeofenceRegistrationService::class.java)
        }
    }

    companion object {

        private const val LOG_TAG: String = "BootCompletedReceiver"
    }
}