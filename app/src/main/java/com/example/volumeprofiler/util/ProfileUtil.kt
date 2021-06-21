package com.example.volumeprofiler.util

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.receivers.AlarmReceiver
import java.util.*

/*
   * Utility class which simplifies work with audio-related values
 */
class ProfileUtil private constructor (private val context: Context) {

    fun applyAudioSettings(primarySettings: Map<Int, Int>, optionalSettings: Map<String, Int>, id: UUID, title: String) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        for ((key, value) in primarySettings) {
            if (key != VIBRATE_FOR_CALLS) {
                audioManager.setStreamVolume(key, value, AudioManager.FLAG_SHOW_UI)
            }
            else {
                if (value == AudioManager.RINGER_MODE_VIBRATE) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
                else {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            }
        }
        /*
        for ((key, value) in optionalSettings) {
            Settings.System.putInt(context.contentResolver, key, value)
        }
         */
        SharedPreferencesUtil.getInstance().saveProfileToSharedPrefs(id, primarySettings, optionalSettings, title)
    }

    fun sendBroadcastToUpdateUI(profileId: UUID): Unit {
        val localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
        val intent: Intent = Intent(Application.ACTION_UPDATE_UI).apply {
            this.putExtra(AlarmReceiver.EXTRA_PROFILE_ID, profileId)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    companion object {

        const val VIBRATE_FOR_CALLS: Int = 1
        private var INSTANCE: ProfileUtil? = null

        fun getInstance(): ProfileUtil {

            if (INSTANCE != null) {
                return INSTANCE!!
            }
            else {
                throw IllegalStateException("Singleton must be initialized")
            }
        }

        fun initialize(context: Context) {

            if (INSTANCE == null) {
                INSTANCE = ProfileUtil(context)
            }
        }

        fun getVolumeSettingsMapPair(profile: Profile): Pair<Map<Int, Int>, Map<String, Int>> {

            val streamVolumeMap: Map<Int, Int> = hashMapOf(
                    Pair(AudioManager.STREAM_MUSIC, profile.mediaVolume),
                    Pair(AudioManager.STREAM_VOICE_CALL, profile.callVolume),
                    Pair(AudioManager.STREAM_NOTIFICATION, profile.notificationVolume),
                    Pair(AudioManager.STREAM_RING, profile.ringVolume),
                    Pair(AudioManager.STREAM_ALARM, profile.alarmVolume),
                    Pair(VIBRATE_FOR_CALLS, profile.vibrateForCalls))

            val additionalSoundsMap: Map<String, Int> = hashMapOf(
                    Pair(Settings.System.DTMF_TONE_WHEN_DIALING, profile.dialTones),
                    Pair(Settings.System.SOUND_EFFECTS_ENABLED, profile.touchSounds),
                    Pair(Settings.System.HAPTIC_FEEDBACK_ENABLED, profile.touchVibration))

            return Pair(streamVolumeMap, additionalSoundsMap)
        }
    }
}