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
import android.app.NotificationManager.*
import android.util.Log

@AndroidEntryPoint
class PhoneStateReceiver: BroadcastReceiver() {

    @Inject
    lateinit var sharedPreferencesUtil: SharedPreferencesUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_PHONE_STATE_CHANGED && intent.extras != null) {

            val streamsUnlinked: Boolean = sharedPreferencesUtil.getStreamsUnlinked()
            val interruptionFilter: Int = sharedPreferencesUtil.getInterruptionFilter()
            val priorityCategories: List<Int> = sharedPreferencesUtil.getPriorityCategories()

            val isPriorityFilter: Boolean = interruptionFilter == INTERRUPTION_FILTER_PRIORITY
            val includesCallsPriority: Boolean = isPriorityFilter && priorityCategories.contains(
                Policy.PRIORITY_CATEGORY_REPEAT_CALLERS) || priorityCategories.contains(Policy.PRIORITY_CATEGORY_CALLS)

            if (streamsUnlinked && (includesCallsPriority || interruptionFilter == INTERRUPTION_FILTER_ALL)) {
                val phoneState: String? = intent.extras?.getString(EXTRA_STATE)

                val ringVolume: Int = sharedPreferencesUtil.getRingStreamVolume()
                val notificationVolume: Int = sharedPreferencesUtil.getNotificationStreamVolume()
                val ringerMode: Int = sharedPreferencesUtil.getRingerMode()
                val notificationMode: Int = sharedPreferencesUtil.getNotificationMode()

                if (phoneState == EXTRA_STATE_RINGING && ringVolume >= 0) {
                    profileUtil.setRingerMode(
                        STREAM_RING, ringVolume, ringerMode, FLAG_ALLOW_RINGER_MODES
                    )
                }
                else if ((phoneState == EXTRA_STATE_OFFHOOK || phoneState == EXTRA_STATE_IDLE) && notificationVolume >= 0) {
                    profileUtil.setRingerMode(
                        STREAM_NOTIFICATION,
                        notificationVolume, notificationMode, FLAG_ALLOW_RINGER_MODES)
                }
            }
        }
    }
}