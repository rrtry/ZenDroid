package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.telephony.TelephonyManager
import android.os.Build
import com.example.volumeprofiler.Application

class IncomingCallReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED
                && intent.extras != null) {
            val storageContext: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context!!.createDeviceProtectedStorageContext()
            } else {
                context!!
            }
            val extraState: String? = intent.extras!!.getString(TelephonyManager.EXTRA_STATE)
            val sharedPreferences: SharedPreferences = storageContext.getSharedPreferences(Application.SHARED_PREFERENCES, Context.MODE_PRIVATE)
            val ringVolume: Int = sharedPreferences.getInt(AlarmReceiver.PREFS_PROFILE_STREAM_RING, -1)
            val notificationVolume: Int = sharedPreferences.getInt(AlarmReceiver.PREFS_PROFILE_STREAM_NOTIFICATION, -1)
            if (extraState == TelephonyManager.EXTRA_STATE_RINGING && ringVolume >= 0) {
                applyAudioSettings(context, AudioManager.STREAM_RING, ringVolume)
            }
            else if ((extraState == TelephonyManager.EXTRA_STATE_IDLE
                            || extraState == TelephonyManager.EXTRA_STATE_OFFHOOK)
                    && notificationVolume >= 0) {
                applyAudioSettings(context, AudioManager.STREAM_NOTIFICATION, notificationVolume)
            }
        }
    }

    private fun applyAudioSettings(context: Context, audioStream: Int, index: Int): Unit {
        val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(audioStream, index, AudioManager.FLAG_SHOW_UI)
    }
}