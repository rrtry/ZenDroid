package com.example.volumeprofiler.util

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import com.example.volumeprofiler.VolumeProfilerApplication
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
            val storageContext: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }
            val sharedPreferences: SharedPreferences = storageContext.getSharedPreferences(VolumeProfilerApplication.SHARED_PREFERENCES, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val primarySettings = volumeSettingsMapPair.first
            val additionalSettings = volumeSettingsMapPair.second
            for ((key, value) in primarySettings) {
                when (key) {
                    AudioManager.STREAM_MUSIC -> {
                        editor.putInt(AlarmReceiver.PREFS_PROFILE_STREAM_MUSIC, value)
                    }
                    AudioManager.STREAM_VOICE_CALL -> {
                        editor.putInt(AlarmReceiver.PREFS_PROFILE_STREAM_VOICE_CALL, value)
                    }
                    AudioManager.STREAM_RING -> {
                        editor.putInt(AlarmReceiver.PREFS_PROFILE_STREAM_RING, value)
                    }
                    AudioManager.STREAM_NOTIFICATION -> {
                        editor.putInt(AlarmReceiver.PREFS_PROFILE_STREAM_NOTIFICATION, value)
                    }
                    AudioManager.STREAM_ALARM -> {
                        editor.putInt(AlarmReceiver.PREFS_PROFILE_STREAM_ALARM, value)
                    }
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