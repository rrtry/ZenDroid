package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.volumeprofiler.services.AlarmRescheduleService


class BootCompletedReceiver: BroadcastReceiver() {

    private fun startService(context: Context): Unit {
        val intent: Intent = Intent(context, AlarmRescheduleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i(LOG_TAG, "Starting foreground service ( ... >= Api 26)")
            context.startForegroundService(intent)
        }
        else {
            Log.i(LOG_TAG, "Starting foreground service (Api 26 < ...")
            context.startService(intent)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?): Unit {

        if (intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Log.i(LOG_TAG, "onReceive(), action: ACTION_LOCKED_BOOT_COMPLETED")
            startService(context!!)
        }
        else if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(LOG_TAG, "onReceive(), action: ACTION_BOOT_COMPLETED")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                startService(context!!)
            }
        }
    }

    companion object {

        private const val LOG_TAG: String = "BootCompletedReceiver"
    }
}