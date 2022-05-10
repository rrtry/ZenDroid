package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.core.PreferencesManager
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
        if (intent?.action == ACTION_PHONE_STATE_CHANGED) {

            preferencesManager.getEnabledProfile()?.let { profile ->

                val includesCallsPriority: Boolean = profile.interruptionFilter == INTERRUPTION_FILTER_PRIORITY &&
                        (profile.priorityCategories and PRIORITY_CATEGORY_REPEAT_CALLERS) != 0 ||
                        (profile.priorityCategories and PRIORITY_CATEGORY_CALLS) != 0

                if (profile.streamsUnlinked && (includesCallsPriority || profile.interruptionFilter == INTERRUPTION_FILTER_ALL)) {

                    val phoneState: String? = intent.extras?.getString(EXTRA_STATE)

                    if (phoneState == EXTRA_STATE_RINGING) {
                        profileManager.setRingerMode(
                            STREAM_RING, profile.ringVolume, profile.ringerMode, FLAG_ALLOW_RINGER_MODES
                        )
                    } else if (phoneState == EXTRA_STATE_OFFHOOK || phoneState == EXTRA_STATE_IDLE) {
                        profileManager.setRingerMode(
                            STREAM_NOTIFICATION, profile.notificationVolume, profile.notificationMode, FLAG_ALLOW_RINGER_MODES
                        )
                    }
                }
            }
        }
    }
}