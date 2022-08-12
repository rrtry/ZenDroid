package ru.rrtry.silentdroid.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.rrtry.silentdroid.core.ProfileManager
import ru.rrtry.silentdroid.core.PreferencesManager
import javax.inject.Inject
import android.media.AudioManager.*
import dagger.hilt.android.AndroidEntryPoint
import android.telephony.TelephonyManager.*
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.PREFS_STREAM_TYPE_INDEPENDENT
import ru.rrtry.silentdroid.entities.Profile

@AndroidEntryPoint
class PhoneStateReceiver: BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var profileManager: ProfileManager

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_PHONE_STATE_CHANGED) {

            val profile: Profile = preferencesManager.getProfile() ?: return
            val phoneState: String? = intent.extras?.getString(EXTRA_STATE)

            if (!profileManager.isRingerAudible(profile)) return
            if (preferencesManager.getNotificationStreamType() == PREFS_STREAM_TYPE_INDEPENDENT) return

            if (phoneState == EXTRA_STATE_RINGING) {
                profileManager.setRingerMode(
                    STREAM_RING,
                    profile.ringVolume,
                    profile.ringerMode,
                    FLAG_ALLOW_RINGER_MODES
                )
            } else if (phoneState == EXTRA_STATE_OFFHOOK || phoneState == EXTRA_STATE_IDLE) {
                profileManager.setRingerMode(
                    STREAM_NOTIFICATION,
                    profile.notificationVolume,
                    profile.notificationMode,
                    FLAG_ALLOW_RINGER_MODES
                )
            }
        }
    }
}