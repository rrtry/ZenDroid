package com.example.volumeprofiler.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.volumeprofiler.util.ProfileUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver: BroadcastReceiver() {

    @Inject lateinit var profileUtil: ProfileUtil

    override fun onReceive(context: Context?, intent: Intent?) {
        /*
        if (intent?.action == Application.ACTION_WIDGET_PROFILE_SELECTED && intent.extras != null) {
            val profile: Profile = intent.extras!!.getParcelable(StatsService.EXTRA_PROFILE)!!
            profileUtil.setProfile(profile)
            sendServiceCommand(context!!)
        }
         */
    }
}