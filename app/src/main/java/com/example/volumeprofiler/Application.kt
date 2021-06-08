package com.example.volumeprofiler

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.fragments.ProfilesListFragment

class Application: Application(), LifecycleObserver {

    override fun onCreate(): Unit {
        super.onCreate()
        val storageContext: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.createDeviceProtectedStorageContext()
        }
        else {
            this
        }
        Repository.initialize(storageContext)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onApplicationInBackground(): Unit {
        Log.i(LOG_TAG, "onApplicationInBackground")
        sendBroadcast(ACTION_GONE_BACKGROUND)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onApplicationInForeground(): Unit {
        Log.i(LOG_TAG, "onApplicationForeground")
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                sendBroadcast(ACTION_GONE_FOREGROUND)
            }
        }, 100)
    }

    private fun sendBroadcast(action: String): Unit {
        val intent: Intent = Intent(this, ProfilesListFragment::class.java).apply {
            this.action = action
        }
        val localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.sendBroadcast(intent)
    }

    companion object {

        private const val LOG_TAG: String = "Application"
        const val SHARED_PREFERENCES: String = "volumeprofiler_shared_prefs"
        const val ACTION_ALARM_TRIGGER: String = "com.example.volumeprofiler.ACTION_TRIGGER_ALARM"
        const val ACTION_UPDATE_UI: String = "com.example.volumeprofiler.ACTION_UPDATE_UI"
        const val ACTION_GONE_FOREGROUND: String = "com.example.volumeprofiler.ACTION_GONE_FOREGROUND"
        const val ACTION_GONE_BACKGROUND: String = "com.example.volumeprofiler.ACTION_GONE_BACKGROUND"
        const val ACTION_WIDGET_PROFILE_SELECTED: String = "com.example.volumeprofiler.WIDGET_PROFILE_SELECTED"
    }
}