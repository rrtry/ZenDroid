package com.example.volumeprofiler.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.app.NotificationManager.Policy.*
import android.app.NotificationManager.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.receivers.AlarmReceiver
import java.util.*
import android.os.Build
import android.util.Log

class ProfileUtil private constructor (private val context: Context) {

    fun applyAudioSettings(profile: Profile) {
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //Settings.System.putInt(context.contentResolver, Settings.System.VIBRATE_WHEN_RINGING, profile.isVibrateForCallsActive)
        if (profile.isInterruptionFilterActive == 1) {
            if (profile.interruptionFilter == INTERRUPTION_FILTER_PRIORITY) {
                lateinit var policy: Policy
                if (Build.VERSION_CODES.N > Build.VERSION.SDK_INT) {
                    policy = Policy (
                        bitmaskOfListContents(profile.priorityCategories),
                        profile.priorityCallSenders,
                        profile.priorityMessageSenders
                    )
                }
                else if (Build.VERSION_CODES.N <= Build.VERSION.SDK_INT && Build.VERSION_CODES.R > Build.VERSION.SDK_INT) {
                    policy = Policy (
                        bitmaskOfListContents(profile.priorityCategories),
                        profile.priorityCallSenders,
                        profile.priorityMessageSenders,
                        bitmaskOfListContents(profile.screenOnVisualEffects + profile.screenOffVisualEffects)
                    )
                }
                else if (Build.VERSION_CODES.R <= Build.VERSION.SDK_INT) {
                    policy = Policy (
                        bitmaskOfListContents(profile.priorityCategories),
                        profile.priorityCallSenders,
                        profile.priorityMessageSenders,
                        bitmaskOfListContents(profile.screenOnVisualEffects + profile.screenOffVisualEffects)
                    )
                }
                notificationManager.notificationPolicy = policy
            }
            notificationManager.setInterruptionFilter(profile.interruptionFilter)
        }
        else {
            notificationManager.setInterruptionFilter(INTERRUPTION_FILTER_ALL)
        }
        setStreamValues(profile)
        //audioManager.ringerMode = profile.ringerMode
        SharedPreferencesUtil.getInstance().saveCurrentProfile(profile)
    }

    private fun setStreamValues(profile: Profile) {
        val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (profile.isInterruptionFilterActive == 1) {
            when (profile.interruptionFilter) {
                INTERRUPTION_FILTER_PRIORITY -> {
                    if (profile.priorityCategories.contains(PRIORITY_CATEGORY_ALARMS)) {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, profile.alarmVolume, AudioManager.FLAG_SHOW_UI)
                    }
                    if (profile.priorityCategories.contains(PRIORITY_CATEGORY_MEDIA)) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, profile.mediaVolume, AudioManager.FLAG_SHOW_UI)
                    }
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, profile.callVolume, AudioManager.FLAG_SHOW_UI)
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, profile.notificationVolume, AudioManager.FLAG_SHOW_UI)
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, profile.ringVolume, AudioManager.FLAG_SHOW_UI)
                }
                INTERRUPTION_FILTER_ALARMS -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, profile.mediaVolume, AudioManager.FLAG_SHOW_UI)
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, profile.callVolume, AudioManager.FLAG_SHOW_UI)
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, profile.alarmVolume, AudioManager.FLAG_SHOW_UI)
                }
                INTERRUPTION_FILTER_NONE -> {
                    Log.i("ProfileUtil", "interruption filter none")
                }
            }
        }
        else {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, profile.mediaVolume, AudioManager.FLAG_SHOW_UI)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, profile.callVolume, AudioManager.FLAG_SHOW_UI)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, profile.alarmVolume, AudioManager.FLAG_SHOW_UI)
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, profile.notificationVolume, AudioManager.FLAG_SHOW_UI)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, profile.ringVolume, AudioManager.FLAG_SHOW_UI)
        }
    }

    fun sendLocalBroadcast(profileId: UUID): Unit {
        val localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
        val intent: Intent = Intent(Application.ACTION_UPDATE_UI).apply {
            this.putExtra(AlarmReceiver.EXTRA_PROFILE_ID, profileId)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    companion object {

        private var INSTANCE: ProfileUtil? = null

        private fun bitmaskOfListContents(list: List<Int>): Int {
            var temp: Int = 0
            for (i in list) {
                temp = temp or i
            }
            return temp
        }

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
    }
}