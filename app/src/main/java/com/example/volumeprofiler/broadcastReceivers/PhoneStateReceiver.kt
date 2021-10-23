package com.example.volumeprofiler.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import javax.inject.Inject
import android.media.AudioManager.*
import dagger.hilt.android.AndroidEntryPoint
import android.telephony.TelephonyManager.*

@AndroidEntryPoint
class PhoneStateReceiver: BroadcastReceiver() {

    @Inject
    lateinit var sharedPreferencesUtil: SharedPreferencesUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_PHONE_STATE_CHANGED
            && intent.extras != null) {

            val phoneState: String? = intent.extras?.getString(EXTRA_STATE)

            val ringVolume: Int = sharedPreferencesUtil.getRingStreamVolume()
            val notificationVolume: Int = sharedPreferencesUtil.getNotificationStreamVolume()
            val ringerMode: Int = sharedPreferencesUtil.getRingerMode()
            val notificationMode: Int = sharedPreferencesUtil.getNotificationMode()

            if (phoneState == EXTRA_STATE_RINGING && ringVolume >= 0) {
                val streamType: Int = STREAM_RING
                profileUtil.setRingerMode(streamType, ringVolume, ringerMode)
            }
            else if ((phoneState == EXTRA_STATE_OFFHOOK || phoneState == EXTRA_STATE_IDLE) && notificationVolume >= 0) {
                val streamType: Int = STREAM_NOTIFICATION
                profileUtil.setRingerMode(streamType, notificationVolume, notificationMode)
            }
        }
    }
}