package ru.rrtry.silentdroid.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import javax.inject.Inject
import android.media.AudioManager.*
import dagger.hilt.android.AndroidEntryPoint
import android.telephony.TelephonyManager.*
import ru.rrtry.silentdroid.core.AppAudioManager
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.PREFS_STREAM_TYPE_INDEPENDENT
import ru.rrtry.silentdroid.core.ProfileManager
import ru.rrtry.silentdroid.entities.Profile

@AndroidEntryPoint
class PhoneStateReceiver: BroadcastReceiver() {

    @Inject lateinit var audioManager: AppAudioManager
    @Inject lateinit var profileManager: ProfileManager

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_PHONE_STATE_CHANGED) {

            val profile: Profile = profileManager.getProfile() ?: return
            val phoneState: String? = intent.extras?.getString(EXTRA_STATE)

            if (!audioManager.isRingerAudible(profile)) return
            if (audioManager.getNotificationStreamType() == PREFS_STREAM_TYPE_INDEPENDENT) return

            if (phoneState == EXTRA_STATE_RINGING) {
                audioManager.setRingerMode(
                    STREAM_RING,
                    profile.ringVolume,
                    profile.ringerMode,
                    FLAG_ALLOW_RINGER_MODES
                )
            } else if (phoneState == EXTRA_STATE_OFFHOOK || phoneState == EXTRA_STATE_IDLE) {
                audioManager.setRingerMode(
                    STREAM_NOTIFICATION,
                    profile.notificationVolume,
                    profile.notificationMode,
                    FLAG_ALLOW_RINGER_MODES
                )
            }
        }
    }
}