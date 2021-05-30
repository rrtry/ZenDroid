package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.telephony.TelephonyManager
import com.example.volumeprofiler.fragments.ProfilesListFragment
import android.util.Log
import android.os.Build
import com.example.volumeprofiler.VolumeProfilerApplication

class IncomingCallReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("IncomingCallReceiver", "onReceive()")
        if (intent != null && intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED
                && intent.extras != null && context != null) {
            val storageContext: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }
            val extraState: String? = intent.extras!!.getString(TelephonyManager.EXTRA_STATE)
            val sharedPreferences: SharedPreferences = storageContext.getSharedPreferences(VolumeProfilerApplication.SHARED_PREFERENCES, Context.MODE_PRIVATE)
            val ringVolume: Int = sharedPreferences.getInt(AlarmReceiver.PREFS_PROFILE_STREAM_RING, -1)
            val notificationVolume: Int = sharedPreferences.getInt(AlarmReceiver.PREFS_PROFILE_STREAM_NOTIFICATION, -1)
            if (extraState == TelephonyManager.EXTRA_STATE_RINGING) {
                applyAudioSettings(context, AudioManager.STREAM_RING, ringVolume) // Phone is in state of receiving incoming call, adjusting ring stream to desired value
            }
            else if (extraState == TelephonyManager.EXTRA_STATE_IDLE) {
                applyAudioSettings(context, AudioManager.STREAM_NOTIFICATION, notificationVolume) // Phone is in state of ending a call, reverting settings
            }
        }
    }

    private fun applyAudioSettings(context: Context, audioStream: Int, index: Int): Unit {
        val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(audioStream, index, AudioManager.FLAG_SHOW_UI)
    }
}