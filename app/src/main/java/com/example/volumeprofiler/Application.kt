package com.example.volumeprofiler

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.fragments.ProfilesListFragment
import com.example.volumeprofiler.services.NotificationWidgetService
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil

class Application: Application(), LifecycleObserver {

    override fun onCreate(): Unit {
        super.onCreate()
        initializeSingletons()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop(): Unit {
        sendGoneBackgroundBroadcast()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume(): Unit {
        stopNotificationService()
    }

    private fun initializeSingletons(): Unit {
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
    }

    private fun sendGoneBackgroundBroadcast(): Unit {
        val intent: Intent = Intent(this, ProfilesListFragment::class.java).apply {
            this.action = ACTION_GONE_BACKGROUND
        }
        val localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun stopNotificationService(): Unit {
        val intent: Intent = Intent(this, NotificationWidgetService::class.java)
        this.stopService(intent)
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