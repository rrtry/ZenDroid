package com.example.volumeprofiler

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Application: Application(), LifecycleObserver {

    override fun onCreate(): Unit {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop(): Unit {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume(): Unit {

    }

    /*
    private fun stopStatsService(): Unit {
        val intent: Intent = Intent(this, StatsService::class.java)
        stopService(intent)
    }

    private fun startStatsService(): Unit {
        val intent: Intent = Intent(this, StatsService::class.java)
        if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
     */

    companion object {

        private const val LOG_TAG: String = "Application"

        const val ACTION_GEOFENCE_TRANSITION: String = "com.example.volumeprofiler.ACTION_GEOFENCE_TRANSITION"
        const val ACTION_ALARM_TRIGGER: String = "com.example.volumeprofiler.ACTION_ALARM_TRIGGER"
        const val ACTION_UPDATE_ALARM_STATE: String = "com.example.volumeprofiler.ACTION_UPDATE_ALARM_STATE"
        const val ACTION_WIDGET_PROFILE_SELECTED: String = "com.example.volumeprofiler.WIDGET_PROFILE_SELECTED"
    }
}