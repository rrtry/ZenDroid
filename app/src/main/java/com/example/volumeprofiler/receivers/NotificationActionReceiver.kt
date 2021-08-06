package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.services.NotificationWidgetService
import com.example.volumeprofiler.util.ProfileUtil

class NotificationActionReceiver: BroadcastReceiver() {

    @SuppressWarnings("unchecked")
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("NotificationReceiver",  "onReceive()")
        if (intent?.action == Application.ACTION_WIDGET_PROFILE_SELECTED && intent.extras != null) {
            val profileUtil: ProfileUtil = ProfileUtil.getInstance()
            val profile: Profile = intent.extras!!.getParcelable(NotificationWidgetService.EXTRA_PROFILE)!!
            profileUtil.applyAudioSettings(profile)
            profileUtil.sendLocalBroadcast(profile.id)
            startService(context!!)
        }
    }

    private fun startService(context: Context): Unit {
        val intent: Intent = Intent(context, NotificationWidgetService::class.java).apply {
            this.putExtra(NotificationWidgetService.EXTRA_UPDATE_NOTIFICATION, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        }
        else {
            context.startService(intent)
        }
    }
}