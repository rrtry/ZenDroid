package com.example.volumeprofiler.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AlarmCancellationService: Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}