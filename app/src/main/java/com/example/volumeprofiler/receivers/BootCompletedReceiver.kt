package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.volumeprofiler.services.AlarmRescheduleWorker

class BootCompletedReceiver: BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
                intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootCompletedReceiver", "onReceive()")
            val reScheduleRequest: WorkRequest = OneTimeWorkRequestBuilder<AlarmRescheduleWorker>().build()
            WorkManager.getInstance(context!!).enqueue(reScheduleRequest)
        }
    }
}