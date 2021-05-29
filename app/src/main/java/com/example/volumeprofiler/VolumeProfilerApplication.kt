package com.example.volumeprofiler

import android.app.Application
import com.example.volumeprofiler.database.Repository

class VolumeProfilerApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Repository.initialize(this)
    }

    companion object {
        
        const val ACTION_TRIGGER_ALARM: String = "com.example.volumeprofiler.ACTION_TRIGGER_ALARM"
        const val ACTION_UPDATE_UI: String = "com.example.volumeprofiler.ACTION_UPDATE_UI"
    }
}