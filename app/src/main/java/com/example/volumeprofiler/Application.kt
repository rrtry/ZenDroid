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

    companion object {

        private const val LOG_TAG: String = "Application"

        const val ACTION_GEOFENCE_TRANSITION: String = "com.example.volumeprofiler.ACTION_GEOFENCE_TRANSITION"
        const val ACTION_ALARM_ALERT: String = "com.example.volumeprofiler.ACTION_ALARM_TRIGGER"
        const val ACTION_UPDATE_ALARM_STATE: String = "com.example.volumeprofiler.ACTION_UPDATE_ALARM_STATE"
    }
}