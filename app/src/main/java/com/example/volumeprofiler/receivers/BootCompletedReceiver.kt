package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.volumeprofiler.services.AlarmRescheduleService


class BootCompletedReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val intent: Intent = Intent(context, AlarmRescheduleService::class.java)
            context?.startService(intent)
        }
        else if (intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val intent: Intent = Intent(context, AlarmRescheduleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context?.startForegroundService(intent)
            }
            else {
                context?.startService(intent)
            }
        }
    }

    companion object {

        private const val LOG_TAG: String = "BootCompletedReceiver"
    }
}