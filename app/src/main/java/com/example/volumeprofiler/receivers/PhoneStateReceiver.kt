package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.volumeprofiler.util.ProfileManager
import com.example.volumeprofiler.util.PreferencesManager
import javax.inject.Inject
import android.media.AudioManager.*
import dagger.hilt.android.AndroidEntryPoint
import android.telephony.TelephonyManager.*
import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*

@AndroidEntryPoint
class PhoneStateReceiver: BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var profileManager: ProfileManager

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent?.action == ACTION_PHONE_STATE_CHANGED && intent.extras != null) {

            val streamsUnlinked: Boolean = preferencesManager.getStreamsUnlinked()
            val interruptionFilter: Int = preferencesManager.getInterruptionFilter()
            val priorityCategories: Int = preferencesManager.getPriorityCategories()

            val isPriorityFilter: Boolean = interruptionFilter == INTERRUPTION_FILTER_PRIORITY
            val includesCallsPriority: Boolean = isPriorityFilter && (priorityCategories and PRIORITY_CATEGORY_REPEAT_CALLERS) != 0
                    || (priorityCategories and PRIORITY_CATEGORY_CALLS) != 0

            if (streamsUnlinked && (includesCallsPriority || interruptionFilter == INTERRUPTION_FILTER_ALL)) {

                val phoneState: String? = intent.extras?.getString(EXTRA_STATE)

                val ringVolume: Int = preferencesManager.getRingStreamVolume()
                val notificationVolume: Int = preferencesManager.getNotificationStreamVolume()
                val ringerMode: Int = preferencesManager.getRingerMode()
                val notificationMode: Int = preferencesManager.getNotificationMode()

                if (phoneState == EXTRA_STATE_RINGING && ringVolume >= 0) {
                    profileManager.setRingerMode(
                        STREAM_RING, ringVolume, ringerMode, FLAG_ALLOW_RINGER_MODES
                    )
                }
                else if ((phoneState == EXTRA_STATE_OFFHOOK || phoneState == EXTRA_STATE_IDLE) && notificationVolume >= 0) {
                    profileManager.setRingerMode(
                        STREAM_NOTIFICATION,
                        notificationVolume, notificationMode, FLAG_ALLOW_RINGER_MODES)
                }
            }
        }
    }
}