package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.services.NotificationWidgetService
import com.example.volumeprofiler.util.ProfileUtil
import java.util.*

class NotificationActionReceiver: BroadcastReceiver() {

    private lateinit var context: Context

    @SuppressWarnings("unchecked")
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("NotificationReceiver",  "onReceive()")
        if (intent?.action == Application.ACTION_WIDGET_PROFILE_SELECTED && intent.extras != null) {
            val title: String? = intent.extras!!.getString(NotificationWidgetService.EXTRA_PROFILE_TITLE)
            val settings: Pair<Map<Int, Int>, Map<String, Int>> = intent.extras!!.getSerializable(NotificationWidgetService.EXTRA_PROFILE_SETTINGS)
                    as Pair<Map<Int, Int>, Map<String, Int>>
            val id = intent.extras!!.get(NotificationWidgetService.EXTRA_PROFILE_ID) as UUID
            val profileUtil = ProfileUtil(context!!)

            profileUtil.applyAudioSettings(settings.first, settings.second, id, title!!)
            startService(context)
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