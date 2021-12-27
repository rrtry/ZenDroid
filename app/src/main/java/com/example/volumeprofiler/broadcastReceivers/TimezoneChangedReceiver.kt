package com.example.volumeprofiler.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.volumeprofiler.services.SchedulerService
import com.example.volumeprofiler.util.startService

class TimezoneChangedReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_TIMEZONE_CHANGED) {
            startService(context!!, SchedulerService::class.java)
        }
    }
}