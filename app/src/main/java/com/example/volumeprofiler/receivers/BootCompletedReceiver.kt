package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.volumeprofiler.services.AlarmRescheduleService
import com.example.volumeprofiler.services.NotificationWidgetService


class BootCompletedReceiver: BroadcastReceiver() {

    private fun startService(context: Context, service: Class<*>): Unit {
        val intent: Intent = Intent(context, service)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        }
        else {
            context.startService(intent)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?): Unit {

        val context: Context = context as Context

        if (intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            startService(context, AlarmRescheduleService::class.java)
            startService(context, NotificationWidgetService::class.java)
        }
        else if (intent?.action == Intent.ACTION_BOOT_COMPLETED &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            startService(context, AlarmRescheduleService::class.java)
            startService(context, NotificationWidgetService::class.java)
        }
    }

    companion object {

        private const val LOG_TAG: String = "BootCompletedReceiver"
    }
}