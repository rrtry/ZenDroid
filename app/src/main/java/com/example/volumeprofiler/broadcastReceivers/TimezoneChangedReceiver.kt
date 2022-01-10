package com.example.volumeprofiler.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.volumeprofiler.services.SchedulerService

class TimezoneChangedReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_TIMEZONE_CHANGED) {
            val intent: Intent = Intent(context!!, SchedulerService::class.java)
            if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}