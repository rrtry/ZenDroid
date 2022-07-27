package ru.rrtry.silentdroid

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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {

    }

    companion object {

        internal const val ACTION_GEOFENCE_TRANSITION: String = "com.example.volumeprofiler.ACTION_GEOFENCE_TRANSITION"
        internal const val ACTION_ALARM: String = "com.example.volumeprofiler.ACTION_ALARM"
    }
}