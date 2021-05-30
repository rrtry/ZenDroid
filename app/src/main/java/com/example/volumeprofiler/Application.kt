package com.example.volumeprofiler

import android.app.Application
import android.content.Context
import android.os.Build
import com.example.volumeprofiler.database.Repository

class Application: Application() {

    override fun onCreate() {
        super.onCreate()
        val context: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.createDeviceProtectedStorageContext()
        }
        else {
            this
        }
        Repository.initialize(context)
    }

    companion object {

        const val SHARED_PREFERENCES = "volumeprofiler_shared_prefs"
        private const val LOG_TAG: String = "Application"
        const val ACTION_TRIGGER_ALARM: String = "com.example.volumeprofiler.ACTION_TRIGGER_ALARM"
        const val ACTION_UPDATE_UI: String = "com.example.volumeprofiler.ACTION_UPDATE_UI"
    }
}