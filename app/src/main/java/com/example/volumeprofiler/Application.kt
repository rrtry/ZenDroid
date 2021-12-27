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

        internal const val ACTION_UPDATE_CALENDAR_EVENT: String = "com.example.volumeprofiler.ACTION_UPDATE_CALENDAR_EVENT"
        internal const val ACTION_GEOFENCE_TRANSITION: String = "com.example.volumeprofiler.ACTION_GEOFENCE_TRANSITION"
        internal const val ACTION_ALARM_TRIGGER: String = "com.example.volumeprofiler.ACTION_ALARM_TRIGGER"
        internal const val ACTION_CALENDAR_EVENT_TRIGGER: String = "com.example.volumeprofiler.ACTION_CALENDAR_EVENT_TRIGGER"
        internal const val ACTION_EVENT_END: String = "com.example.volumeprofiler.ACTION_EVENT_END"
    }
}