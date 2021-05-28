package com.example.volumeprofiler.util

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.provider.Settings
import com.example.volumeprofiler.fragments.ProfilesListFragment
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.receivers.AlarmReceiver

/*
   * Utility class which simplifies work with audio-related values
 */
class AudioUtil {

    companion object {

        fun getVolumeSettingsMapPair(profile: Profile): Pair<Map<Int, Int>, Map<String, Int>> {

            val streamVolumeMap: Map<Int, Int> = hashMapOf(
                    Pair(AudioManager.STREAM_MUSIC, profile.mediaVolume),
                    Pair(AudioManager.STREAM_VOICE_CALL, profile.callVolume),
                    Pair(AudioManager.STREAM_NOTIFICATION, profile.notificationVolume),
                    Pair(AudioManager.STREAM_RING, profile.ringVolume),
                    Pair(AudioManager.STREAM_ALARM, profile.alarmVolume))

            val additionalSoundsMap: Map<String, Int> = hashMapOf(
                    Pair(Settings.System.DTMF_TONE_WHEN_DIALING, profile.dialTones),
                    Pair(Settings.System.SOUND_EFFECTS_ENABLED, profile.touchSounds),
                    Pair(Settings.System.HAPTIC_FEEDBACK_ENABLED, profile.touchVibration))

            return Pair(streamVolumeMap, additionalSoundsMap)
        }

        fun applyAudioSettings(context: Context, volumeSettingsMapPair: Pair<Map<Int, Int>, Map<String, Int>>) {
            val sharedPreferences: SharedPreferences = context.getSharedPreferences(ProfilesListFragment.SHARED_PREFERENCES, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val primarySettings = volumeSettingsMapPair.first
            val additionalSettings = volumeSettingsMapPair.second
            for ((key, value) in primarySettings) {
                if (key == AudioManager.STREAM_NOTIFICATION) {
                    editor.putInt(AlarmReceiver.PREFS_PROFILE_NOTIFICATION_VOLUME, value)
                }
                else if (key == AudioManager.STREAM_RING) {
                    editor.putInt(AlarmReceiver.PREFS_PROFILE_RING_VOLUME, value)
                }
                audioManager.setStreamVolume(key, value, AudioManager.FLAG_SHOW_UI)
            }
            /*
            for ((key, value) in additionalSettings) {
                Settings.System.putInt(context.contentResolver, key, value)
            }
             */
            editor.apply()
        }
    }
}