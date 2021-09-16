package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import javax.inject.Inject
import android.media.AudioManager.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IncomingCallReceiver: BroadcastReceiver() {

    @Inject lateinit var sharedPreferencesUtil: SharedPreferencesUtil
    @Inject lateinit var profileUtil: ProfileUtil

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED
            && intent.extras != null) {
            val phoneState: String? = intent.extras?.getString(TelephonyManager.EXTRA_STATE)

            val ringVolume: Int = sharedPreferencesUtil.getRingStreamVolume()
            val notificationVolume: Int = sharedPreferencesUtil.getNotificationStreamVolume()
            val ringerMode: Int = sharedPreferencesUtil.getRingerMode()
            val notificationMode: Int = sharedPreferencesUtil.getNotificationMode()
            if (phoneState == TelephonyManager.EXTRA_STATE_RINGING && ringVolume >= 0) {
                val streamType: Int = STREAM_RING
                when (ringerMode) {
                    RINGER_MODE_NORMAL -> {
                        profileUtil.setStreamVolume(streamType, ringVolume, 0)
                    }
                    RINGER_MODE_SILENT -> {
                        profileUtil.toggleSilentMode(streamType)
                    }
                    RINGER_MODE_VIBRATE -> {
                        profileUtil.toggleVibrateMode(streamType)
                    }
                }
            }
            else if (phoneState == TelephonyManager.EXTRA_STATE_OFFHOOK && notificationVolume >= 0) {
                val streamType: Int = STREAM_NOTIFICATION
                when (notificationMode) {
                    RINGER_MODE_NORMAL -> {
                        profileUtil.setStreamVolume(streamType, ringVolume, 0)
                    }
                    RINGER_MODE_SILENT -> {
                        profileUtil.toggleSilentMode(streamType)
                    }
                    RINGER_MODE_VIBRATE -> {
                        profileUtil.toggleVibrateMode(streamType)
                    }
                }
            }
        }
    }
}