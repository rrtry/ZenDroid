package com.example.volumeprofiler.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.receivers.AlarmReceiver
import java.util.*

/*
   * Utility class which simplifies work with audio-related values
 */
class ProfileUtil constructor (val context: Context) {

    private val storageContext: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        context.createDeviceProtectedStorageContext() else context
    private val sharedPreferences: SharedPreferences = storageContext.getSharedPreferences(Application.SHARED_PREFERENCES, Context.MODE_PRIVATE)

    private fun saveProfileToSharedPrefs(id: UUID, primarySettings: Map<Int, Int>, optionalSettings: Map<String, Int>, title: String): Unit {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
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
        }
        editor.putString(AlarmReceiver.PREFS_PROFILE_TITLE, title)
        editor.putString(AlarmReceiver.PREFS_PROFILE_ID, id.toString())
        editor.apply()
    }

    fun applyAudioSettings(primarySettings: Map<Int, Int>, optionalSettings: Map<String, Int>, id: UUID, title: String) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        for ((key, value) in primarySettings) {
            audioManager.setStreamVolume(key, value, AudioManager.FLAG_SHOW_UI)
        }
        /*
        for ((key, value) in additionalSettings) {
            Settings.System.putInt(context.contentResolver, key, value)
        }
         */
        saveProfileToSharedPrefs(id, primarySettings, optionalSettings, title)
    }

    fun sendBroadcastToUpdateUI(profileId: UUID): Unit {
        val localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
        val intent: Intent = Intent(Application.ACTION_UPDATE_SELECTED_VIEW).apply {
            this.putExtra(AlarmReceiver.EXTRA_PROFILE_ID, profileId)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

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
    }
}