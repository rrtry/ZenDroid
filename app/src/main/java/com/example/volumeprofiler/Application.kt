package com.example.volumeprofiler

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.volumeprofiler.services.StatsService

class Application: Application(), LifecycleObserver {

    override fun onCreate(): Unit {
        super.onCreate()
        initializeSingletons()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop(): Unit {
        //startStatsService()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume(): Unit {
        stopStatsService()
    }

    private fun initializeSingletons(): Unit {
        /*
        val storageContext: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.createDeviceProtectedStorageContext()
        }
        else {
            this
        }
        SharedPreferencesUtil.initialize(storageContext)
        Repository.initialize(storageContext)
        AlarmUtil.initialize(this)
        ProfileUtil.initialize(this)
         */
    }

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

    companion object {


        private const val LOG_TAG: String = "Application"
        const val SHARED_PREFERENCES: String = "volumeprofiler_shared_prefs"
        const val ACTION_ALARM_TRIGGER: String = "com.example.volumeprofiler.ACTION_ALARM_TRIGGER"
        const val ACTION_UPDATE_UI: String = "com.example.volumeprofiler.ACTION_UPDATE_UI"
        const val ACTION_GONE_BACKGROUND: String = "com.example.volumeprofiler.ACTION_GONE_BACKGROUND"
        const val ACTION_WIDGET_PROFILE_SELECTED: String = "com.example.volumeprofiler.WIDGET_PROFILE_SELECTED"
    }
}