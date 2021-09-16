package com.example.volumeprofiler.util

import android.app.NotificationManager
import android.app.NotificationManager.*
import android.content.Context
import android.media.AudioManager
import com.example.volumeprofiler.models.Profile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import android.media.AudioManager.*
import android.os.Build

@Singleton
class ProfileUtil @Inject constructor (
        @ApplicationContext private val context: Context
        ) {

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun createNotificationPolicy(profile: Profile): Policy? {
        if (Build.VERSION_CODES.N > Build.VERSION.SDK_INT) {
            return Policy (
                bitmaskOfListContents(profile.priorityCategories),
                profile.priorityCallSenders,
                profile.priorityMessageSenders
            )
        }
        else if (Build.VERSION_CODES.N <= Build.VERSION.SDK_INT && Build.VERSION_CODES.R > Build.VERSION.SDK_INT) {
             return Policy (
                bitmaskOfListContents(profile.priorityCategories),
                profile.priorityCallSenders,
                profile.priorityMessageSenders,
                bitmaskOfListContents(profile.screenOnVisualEffects + profile.screenOffVisualEffects)
            )
        }
        else if (Build.VERSION_CODES.R <= Build.VERSION.SDK_INT) {
            return Policy (
                bitmaskOfListContents(profile.priorityCategories),
                profile.priorityCallSenders,
                profile.priorityMessageSenders,
                bitmaskOfListContents(profile.screenOnVisualEffects + profile.screenOffVisualEffects),
                profile.primaryConversationSenders
            )
        }
        return null
    }

    private fun setInterruptionFilter(profile: Profile): Unit {
        if (profile.interruptionFilter == INTERRUPTION_FILTER_PRIORITY) {
            notificationManager.notificationPolicy = createNotificationPolicy(profile)
        }
        notificationManager.setInterruptionFilter(profile.interruptionFilter)
    }

    fun applyProfile(profile: Profile) {
        audioManager.setStreamVolume(STREAM_MUSIC, profile.mediaVolume, 0)
        audioManager.setStreamVolume(STREAM_VOICE_CALL, profile.callVolume, 0)
        when (profile.notificationMode) {
            RINGER_MODE_VIBRATE -> {
                toggleVibrateMode(STREAM_NOTIFICATION)
            }
            RINGER_MODE_SILENT -> {
                toggleSilentMode(STREAM_NOTIFICATION)
            }
            RINGER_MODE_NORMAL -> {
                audioManager.setStreamVolume(STREAM_NOTIFICATION, profile.notificationVolume, FLAG_ALLOW_RINGER_MODES)
            }
        }
        audioManager.setStreamVolume(STREAM_ALARM, profile.alarmVolume, 0)
        setInterruptionFilter(profile)
    }

    fun toggleSilentMode(streamType: Int): Unit {
        if (audioManager.isStreamMute(streamType)) {
            audioManager.adjustStreamVolume(streamType, ADJUST_RAISE, 0)
        }
        audioManager.adjustStreamVolume(streamType, ADJUST_MUTE, AudioManager.FLAG_ALLOW_RINGER_MODES)
    }

    fun toggleVibrateMode(streamType: Int): Unit {
        if (audioManager.isStreamMute(streamType)) {
            audioManager.adjustStreamVolume(streamType, ADJUST_RAISE, 0)
        }
        audioManager.setStreamVolume(streamType, 0, FLAG_ALLOW_RINGER_MODES)
    }

    fun setStreamVolume(streamType: Int, index: Int, flags: Int): Unit {
        audioManager.setStreamVolume(streamType, index, flags)
    }

    // TODO implement application-wide event bus
    fun sendLocalBroadcast(profileId: UUID): Unit {
        /*
        val localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
        val intent: Intent = Intent(Application.ACTION_UPDATE_UI).apply {
            this.putExtra(AlarmReceiver.EXTRA_PROFILE_ID, profileId)
        }
        localBroadcastManager.sendBroadcast(intent)
         */
    }

    companion object {

        private fun bitmaskOfListContents(list: List<Int>): Int {
            var temp: Int = 0
            for (i in list) {
                temp = temp or i
            }
            return temp
        }
    }
}