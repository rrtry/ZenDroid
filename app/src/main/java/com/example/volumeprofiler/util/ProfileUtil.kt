package com.example.volumeprofiler.util

import android.annotation.TargetApi
import android.app.NotificationManager
import android.app.NotificationManager.*
import android.content.Context
import android.media.AudioManager
import android.Manifest.permission.*
import android.media.AudioAttributes
import com.example.volumeprofiler.entities.Profile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.media.AudioManager.*
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Vibrator
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import com.example.volumeprofiler.entities.LocationRelation
import java.lang.IllegalArgumentException
import android.media.RingtoneManager.*

@Singleton
class ProfileUtil @Inject constructor (
        @ApplicationContext private val context: Context
        ) {

    @Inject
    lateinit var sharedPreferencesUtil: SharedPreferencesUtil

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val telephonyManager: TelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var ringtone: Ringtone? = null

    private val phoneStateListener: PhoneStateListener = object : PhoneStateListener() {

        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            phoneState = state
        }
    }

    init {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private var phoneState: Int = TelephonyManager.CALL_STATE_IDLE

    fun setProfile(profile: Profile) {
        if (notificationManager.currentInterruptionFilter != INTERRUPTION_FILTER_ALL) {
            setInterruptionFilter(INTERRUPTION_FILTER_ALL) // set non-blocking interruption filter to unmute all audio stream
        }
        setStreamVolume(STREAM_MUSIC, profile.mediaVolume, 0)
        setStreamVolume(STREAM_VOICE_CALL, profile.callVolume, 0)
        setStreamVolume(STREAM_ALARM, profile.alarmVolume, 0)
        if (profile.streamsUnlinked) {
            if (phoneState == TelephonyManager.CALL_STATE_RINGING) {
                setRingerMode(STREAM_RING, profile.ringVolume, profile.ringerMode)
            } else {
                setRingerMode(STREAM_NOTIFICATION, profile.notificationVolume, profile.notificationMode)
            }
        } else {
            setRingerMode(STREAM_RING, profile.ringVolume, profile.ringerMode)
        }
        setInterruptionFilter(profile)
        setRingtoneUri(profile.phoneRingtoneUri, TYPE_RINGTONE)
        setRingtoneUri(profile.notificationSoundUri, TYPE_NOTIFICATION)
        setRingtoneUri(profile.alarmSoundUri, TYPE_ALARM)
        setVibrateWhenRingingBehaviour(profile.isVibrateForCallsActive)
        sharedPreferencesUtil.writeCurrentProfileProperties(profile)
    }

    private fun setSilentMode(streamType: Int, flags: Int = 0): Unit {
        if (isVibrateHardwarePresent()) {
            adjustUnmuteStream(streamType)
        }
        audioManager.adjustStreamVolume(streamType, ADJUST_MUTE, flags)
    }

    private fun setVibrateMode(streamType: Int, flags: Int = 0): Unit {
        if (isVibrateHardwarePresent()) {
            adjustUnmuteStream(streamType)
            audioManager.setStreamVolume(streamType, 0, flags)
        } else {
            Log.w("ProfileUtil", "Vibration is not supported on this device")
        }
    }

    private fun setStreamVolume(streamType: Int, index: Int, flags: Int): Unit {
        adjustUnmuteStream(streamType)
        audioManager.setStreamVolume(streamType, index, flags)
    }

    fun getAlarmStreamMinVolume(): Int {
        return if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
            audioManager.getStreamMinVolume(STREAM_ALARM)
        } else {
            0
        }
    }

    fun getVoiceCallStreamMinVolume(): Int {
        return if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
            audioManager.getStreamMinVolume(STREAM_VOICE_CALL)
        } else {
            1
        }
    }

    private fun adjustUnmuteStream(streamType: Int): Unit {
        if (audioManager.isStreamMute(streamType)) {
            audioManager.adjustStreamVolume(streamType, ADJUST_UNMUTE, 0)
        }
    }

    private fun isVibrateHardwarePresent(): Boolean {
        return vibrator.hasVibrator()
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun isNotificationPolicyAccessGranted(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun setDefaults(): Unit {
        setInterruptionFilter(INTERRUPTION_FILTER_ALL)
        setStreamVolume(STREAM_MUSIC, 3, 0)
        setStreamVolume(STREAM_VOICE_CALL, 3, 0)
        setStreamVolume(STREAM_ALARM, 3, 0)
        setStreamVolume(STREAM_NOTIFICATION, 3, 0)
        sharedPreferencesUtil.clearPreferences()
    }

    @Suppress("newApi")
    private fun createNotificationPolicy(profile: Profile): Policy {
        return when {
            Build.VERSION_CODES.N > Build.VERSION.SDK_INT -> {
                Policy (
                    bitmaskOfListContents(profile.priorityCategories),
                    profile.priorityCallSenders,
                    profile.priorityMessageSenders
                )
            }
            Build.VERSION_CODES.N <= Build.VERSION.SDK_INT && Build.VERSION_CODES.R > Build.VERSION.SDK_INT -> {
                Policy (
                    bitmaskOfListContents(profile.priorityCategories),
                    profile.priorityCallSenders,
                    profile.priorityMessageSenders,
                    bitmaskOfListContents(profile.screenOnVisualEffects + profile.screenOffVisualEffects)
                )
            }
            else -> {
                Policy (
                    bitmaskOfListContents(profile.priorityCategories),
                    profile.priorityCallSenders,
                    profile.priorityMessageSenders,
                    bitmaskOfListContents(profile.screenOnVisualEffects + profile.screenOffVisualEffects),
                    profile.primaryConversationSenders
                )
            }
        }
    }

    private fun setNotificationPolicy(policy: Policy?): Unit {
        notificationManager.notificationPolicy = policy
    }

    private fun setInterruptionFilter(profile: Profile): Unit {
        if (profile.interruptionFilter == INTERRUPTION_FILTER_PRIORITY) {
            setNotificationPolicy(createNotificationPolicy(profile))
        }
        setInterruptionFilter(profile.interruptionFilter)
    }

    private fun setInterruptionFilter(interruptionFilter: Int): Unit {
        notificationManager.setInterruptionFilter(interruptionFilter)
    }

    fun setRingerMode(streamType: Int, streamVolume: Int, mode: Int, flags: Int = 0): Unit {
        when (mode) {
            RINGER_MODE_NORMAL -> setStreamVolume(streamType, streamVolume, flags)
            RINGER_MODE_VIBRATE -> setVibrateMode(streamType, flags)
            RINGER_MODE_SILENT -> setSilentMode(streamType, flags)
        }
    }

    fun playRingtone(): Unit {
        if (ringtone == null) {
            ringtone = getRingtone(context, getActualDefaultRingtoneUri(context, TYPE_RINGTONE))
            ringtone?.play()
        }
    }

    fun stopPreviousRingtone(): Unit {
        ringtone?.stop()
        ringtone = null
    }

    private fun setRingtoneUri(uri: Uri, type: Int): Unit {
        setActualDefaultRingtoneUri(context, type, uri)
    }

    private fun setVibrateWhenRingingBehaviour(state: Int): Unit {
        if (canModifySystemPreferences()) {
            try {
                Settings.System.putInt(context.contentResolver, Settings.System.VIBRATE_WHEN_RINGING, state)
            } catch (e: IllegalArgumentException) {
                Log.e("ProfileUtil", "Failed to change system settings", e)
            }
        } else {
            Log.w("ProfileUtil", "Not allowed to modify system settings", SecurityException())
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun canModifySystemPreferences(): Boolean {
        return Settings.System.canWrite(context)
    }

    fun shouldRequestPhonePermission(profile: Profile?): Boolean {
        return if (profile == null) {
            false
        } else {
            !checkSelfPermission(context, READ_PHONE_STATE) && requiresPhoneStatePermission(profile)
        }
    }

    fun shouldRequestPhonePermission(locationRelation: LocationRelation): Boolean {
        return !checkSelfPermission(context, READ_PHONE_STATE) && requiresPhoneStatePermission(locationRelation)
    }

    fun requiresPhoneStatePermission(locationRelation: LocationRelation): Boolean {
        return requiresPhoneStatePermission(locationRelation.onEnterProfile) &&
                requiresPhoneStatePermission(locationRelation.onExitProfile)
    }

    private fun requiresPhoneStatePermission(profile: Profile?): Boolean {
        return profile?.streamsUnlinked ?: false
    }

    fun grantedRequiredPermissions(locationRelation: LocationRelation): Boolean {
        return grantedRequiredPermissions(locationRelation.onExitProfile) &&
                grantedRequiredPermissions(locationRelation.onEnterProfile)
    }

    fun grantedSystemPreferencesAccess(): Boolean {
        return isNotificationPolicyAccessGranted() && canModifySystemPreferences()
    }

    fun grantedRequiredPermissions(profile: Profile?): Boolean {
        if (profile != null) {
            if (requiresPhoneStatePermission(profile) && !checkSelfPermission(context, READ_PHONE_STATE)) {
                return false
            }
            return grantedSystemPreferencesAccess()
        }
        else {
            return false
        }
    }

    fun grantedRequiredPermissions(
        checkStoragePermission: Boolean = false,
        checkPhonePermission: Boolean = false): Boolean {
        if (checkPhonePermission && !checkSelfPermission(context, READ_PHONE_STATE)) {
            return false
        }
        if (checkStoragePermission && !checkSelfPermission(context, READ_EXTERNAL_STORAGE)) {
            return false
        }
        return grantedSystemPreferencesAccess()
    }

    fun getMissingPermissions(): List<String> {
        val list: ArrayList<String> = arrayListOf()
        if (!checkSelfPermission(context, READ_EXTERNAL_STORAGE)) {
            list.add(READ_EXTERNAL_STORAGE)
        }
        if (!checkSelfPermission(context, READ_PHONE_STATE)) {
            list.add(READ_PHONE_STATE)
        }
        return list
    }

    companion object {

        @JvmStatic
        fun getMaxStreamVolume(context: Context, streamType: Int): Int {
            val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return audioManager.getStreamMaxVolume(streamType)
        }

        private fun bitmaskOfListContents(list: List<Int>): Int {
            var temp: Int = 0
            for (i in list) {
                temp = temp or i
            }
            return temp
        }
    }
}