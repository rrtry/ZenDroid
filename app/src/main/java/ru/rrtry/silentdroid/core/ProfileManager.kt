package ru.rrtry.silentdroid.core

import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.media.AudioManager.*
import android.provider.Settings
import android.media.RingtoneManager.*
import android.provider.Settings.System.VIBRATE_WHEN_RINGING
import com.google.android.gms.location.Geofence
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_ALARM
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_GEOFENCE_ENTER
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_GEOFENCE_EXIT
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_MANUAL
import ru.rrtry.silentdroid.entities.*
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_ALARM_DEFAULT_VOLUME
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_MUSIC_DEFAULT_VOLUME
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_NOTIFICATION_DEFAULT_VOLUME
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_RING_DEFAULT_VOLUME
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_VOICE_CALL_DEFAULT_VOLUME
import ru.rrtry.silentdroid.event.EventBus
import java.time.LocalDateTime
import java.util.*

@Singleton
class ProfileManager @Inject constructor (@ApplicationContext private val context: Context) {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var appNotificationManager: AppNotificationManager
    @Inject lateinit var audioManager: AppAudioManager
    @Inject lateinit var notificationPolicyManager: NotificationPolicyManager
    @Inject lateinit var ringtoneManager: AppRingtoneManager

    fun getProfile(): Profile? {
        return preferencesManager.getProfile()
    }

    fun isProfileSet(profile: Profile): Boolean {
        return preferencesManager.isProfileSet(profile)
    }

    fun setProfile(profile: Profile, updatePreferences: Boolean = false) {
        if (!audioManager.isVolumeFixed) {
            if (notificationPolicyManager.currentInterruptionFilter != INTERRUPTION_FILTER_ALL) {
                notificationPolicyManager.setInterruptionFilter(INTERRUPTION_FILTER_ALL)
            }

            audioManager.setStreamVolume(STREAM_MUSIC, profile.mediaVolume, 0)
            audioManager.setStreamVolume(STREAM_VOICE_CALL, profile.callVolume, 0)
            audioManager.setStreamVolume(STREAM_ALARM, profile.alarmVolume, 0)

            if (audioManager.isNotificationStreamIndependent()) {
                audioManager.setRingerMode(STREAM_RING, profile.ringVolume, profile.ringerMode)
                audioManager.setStreamVolume(STREAM_NOTIFICATION, profile.notificationVolume, 0)
            } else if (profile.streamsUnlinked) {
                if (audioManager.isRinging) {
                    audioManager.setRingerMode(STREAM_RING, profile.ringVolume, profile.ringerMode)
                } else {
                    audioManager.setRingerMode(STREAM_NOTIFICATION, profile.notificationVolume, profile.notificationMode)
                }
            } else {
                audioManager.setRingerMode(STREAM_RING, profile.ringVolume, profile.ringerMode)
            }
        }

        notificationPolicyManager.setNotificationPolicy(profile)
        ringtoneManager.setRingtoneUri(profile.phoneRingtoneUri, TYPE_RINGTONE)
        ringtoneManager.setRingtoneUri(profile.notificationSoundUri, TYPE_NOTIFICATION)
        ringtoneManager.setRingtoneUri(profile.alarmSoundUri, TYPE_ALARM)
        ringtoneManager.setVibrateWhenRingingState(profile.isVibrateForCallsActive)

        if (updatePreferences) {
            preferencesManager.setProfile(profile)
        }
    }

    fun <T> setProfile(profile: Profile, triggerType: Int, trigger: T?) {
        setProfile(profile, false)
        preferencesManager.setProfile(profile, triggerType, trigger)
        eventBus.onProfileChanged(profile.id)
    }

    suspend fun updateProfileAsync(resetScheduledProfile: Boolean) {
        val alarms: List<AlarmRelation>? = scheduleManager.alarmRepository.getEnabledAlarms()
        scheduleManager.updateSchedule(alarms)
        updateProfile(
            alarms,
            resetScheduledProfile
        )
    }

    fun onGeofenceTrigger(
        geofence: Location,
        transitionType: Int,
        enterProfile: Profile,
        exitProfile: Profile)
    {
        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER, Geofence.GEOFENCE_TRANSITION_DWELL -> {
                setProfile(enterProfile, TRIGGER_TYPE_GEOFENCE_ENTER, geofence)
                appNotificationManager.postGeofenceEnterNotification(enterProfile.title, geofence.title)
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                setProfile(exitProfile, TRIGGER_TYPE_GEOFENCE_EXIT, geofence)
                appNotificationManager.postGeofenceExitNotification(exitProfile.title, geofence.title)
            }
        }
    }

    suspend fun onTimeTrigger(alarm: Alarm, startProfile: Profile, endProfile: Profile) {

        scheduleManager.setNextAlarm(
            alarm,
            startProfile,
            endProfile
        )

        val profile: Profile = getProfile(alarm, startProfile, endProfile)
        val previousAndNextTrigger: PreviousAndNextTrigger? = scheduleManager.getPreviousAndNextTrigger()

        setProfile<Alarm?>(
            profile,
            if (previousAndNextTrigger != null) TRIGGER_TYPE_ALARM else TRIGGER_TYPE_MANUAL,
            if (previousAndNextTrigger != null) alarm else null
        )
        appNotificationManager.updateNotification(profile, previousAndNextTrigger)
    }

    private fun getProfile(alarm: Alarm, startProfile: Profile, endProfile: Profile): Profile {
        return if (scheduleManager.meetsSchedule &&
            scheduleManager.isAlarmValid(alarm)) startProfile else endProfile
    }

    fun updateProfile(alarms: List<AlarmRelation>?, resetScheduledProfile: Boolean = true) {

        var overrideCurrentProfile: Boolean = true
        val previousAndNextTrigger: PreviousAndNextTrigger? = scheduleManager.getPreviousAndNextTriggers(alarms)
        val profileDateTime: LocalDateTime? = preferencesManager.getLastProfileDateTime()

        if (previousAndNextTrigger == null) {
            appNotificationManager.updateNotification(
                preferencesManager.getProfile(),
                previousAndNextTrigger
            )
            return
        }
        if (profileDateTime != null &&
            !resetScheduledProfile)
        {
            overrideCurrentProfile = (previousAndNextTrigger.from ?: LocalDateTime.MIN) >= profileDateTime
        }

        val alarm: Alarm = previousAndNextTrigger.relation.alarm
        if (scheduleManager.hasPreviouslyFired(alarm)) {
            if (overrideCurrentProfile) {
                if (scheduleManager.isAlarmValid(alarm)) {
                    setProfile(previousAndNextTrigger.profile!!, TRIGGER_TYPE_ALARM, alarm)
                } else {
                    setProfile(previousAndNextTrigger.profile!!, TRIGGER_TYPE_MANUAL, null)
                }
            }
            appNotificationManager.updateNotification(
                if (overrideCurrentProfile) previousAndNextTrigger.profile else getProfile(),
                previousAndNextTrigger
            )
        } else {
            appNotificationManager.updateNotification(preferencesManager.getProfile(), previousAndNextTrigger)
        }
    }

    fun getDefaultProfile(): Profile {
        return Profile(
                UUID.randomUUID(),
                context.resources.getString(R.string.new_profile_title),
                R.drawable.ic_baseline_do_not_disturb_on_total_silence_24,
                STREAM_MUSIC_DEFAULT_VOLUME,
                STREAM_VOICE_CALL_DEFAULT_VOLUME,
                STREAM_NOTIFICATION_DEFAULT_VOLUME,
                STREAM_RING_DEFAULT_VOLUME,
                STREAM_ALARM_DEFAULT_VOLUME,
                ringtoneManager.getDefaultRingtoneUri(TYPE_RINGTONE),
                ringtoneManager.getDefaultRingtoneUri(TYPE_NOTIFICATION),
                ringtoneManager.getDefaultRingtoneUri(TYPE_ALARM),
                audioManager.isNotificationStreamIndependent(),
                INTERRUPTION_FILTER_ALL,
                RINGER_MODE_NORMAL,
                RINGER_MODE_NORMAL,
                Settings.System.getInt(context.contentResolver, VIBRATE_WHEN_RINGING),
                PRIORITY_CATEGORY_CALLS,
                PRIORITY_SENDERS_ANY,
                PRIORITY_SENDERS_ANY,
                0,
                CONVERSATION_SENDERS_NONE
        )
    }
}