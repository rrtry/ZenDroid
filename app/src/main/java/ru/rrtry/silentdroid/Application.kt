package ru.rrtry.silentdroid

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Application: Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    companion object {

        const val ACTION_GEOFENCE_TRANSITION: String = "ru.rrtry.silentdroid.ACTION_GEOFENCE_TRANSITION"
        const val ACTION_ALARM: String = "ru.rrtry.silentdroid.ACTION_ALARM"
    }
}