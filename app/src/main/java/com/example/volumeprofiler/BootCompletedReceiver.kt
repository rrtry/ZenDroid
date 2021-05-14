package com.example.volumeprofiler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*

class BootCompletedReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootCompletedReceiver", "onReceive()")
            val reScheduleRequest: WorkRequest = OneTimeWorkRequestBuilder<AlarmRescheduleWorker>().build()
            if (context != null) {
                WorkManager.getInstance(context).enqueue(reScheduleRequest)
            }
        }
    }
}