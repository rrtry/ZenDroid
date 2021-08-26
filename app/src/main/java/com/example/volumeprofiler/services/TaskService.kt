package com.example.volumeprofiler.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class TaskService: Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("TaskService", "onCreate()")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i("TaskService", "onTaskRemoved()")
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }
}