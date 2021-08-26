package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.services.StatsService
import com.example.volumeprofiler.util.ProfileUtil

class NotificationActionReceiver: BroadcastReceiver() {

    @SuppressWarnings("unchecked")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Application.ACTION_WIDGET_PROFILE_SELECTED && intent.extras != null) {
            val profileUtil: ProfileUtil = ProfileUtil.getInstance()
            val profile: Profile = intent.extras!!.getParcelable(StatsService.EXTRA_PROFILE)!!
            profileUtil.applyAudioSettings(profile)
            sendServiceCommand(context!!)
        }
    }

    private fun sendServiceCommand(context: Context): Unit {
        val intent: Intent = Intent(context, StatsService::class.java).apply {
            this.putExtra(StatsService.EXTRA_UPDATE_NOTIFICATION, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        }
        else {
            context.startService(intent)
        }
    }
}