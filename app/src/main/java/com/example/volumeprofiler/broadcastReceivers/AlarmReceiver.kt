package com.example.volumeprofiler.broadcastReceivers

import android.app.job.JobInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat.getSystemService
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.services.AlarmCancellationService
import com.example.volumeprofiler.util.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver: BroadcastReceiver() {

    @Inject
    lateinit var profileUtil: ProfileUtil

    @Inject
    lateinit var alarmUtil: AlarmUtil

    @Inject
    lateinit var eventBus: EventBus

    @SuppressWarnings("unchecked")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Application.ACTION_ALARM_ALERT) {

            val alarm: Alarm = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_ALARM)!!, ParcelableUtil.getParcelableCreator())
            val profile: Profile = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_PROFILE)!!, ParcelableUtil.getParcelableCreator())

            val shouldSchedule: Boolean = alarmUtil.scheduleAlarm(alarm, profile, true)

            if (!shouldSchedule) {
                alarmUtil.cancelAlarm(alarm, profile)
                startService(context!!, alarm.id, profile.title)
            }

            if (profileUtil.canSetProfile()) {
                profileUtil.setProfile(profile)
                eventBus.updateProfilesFragment(profile.id)
                postNotification(context!!, createAlarmAlertNotification(context, profile.title, alarm.localDateTime.toLocalTime()), ID_SCHEDULER)
            } else {
                notifyAboutDeniedPermissions(context!!)
            }
            eventBus.updateAlarmState(alarm)
        }
    }

    private fun notifyAboutDeniedPermissions(context: Context): Unit {
        val missingPermissions: List<String> = profileUtil.getMissingPermissions()
        if (missingPermissions.isNotEmpty()) {
            postNotification(context, createMissingPermissionsNotification(context, missingPermissions), ID_PERMISSIONS)
        }
        if (!profileUtil.canWriteSettings()) {
            postNotification(context, createSystemSettingsNotification(context), ID_SYSTEM_SETTINGS)
        }
        if (!profileUtil.isNotificationPolicyAccessGranted()) {
            postNotification(context, createInterruptionPolicyNotification(context), ID_INTERRUPTION_POLICY)
        }
    }

    private fun startService(context: Context, alarmId: Long, title: String): Unit {
        val intent: Intent = Intent(context, AlarmCancellationService::class.java).apply {
            putExtra(AlarmCancellationService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmCancellationService.EXTRA_PROFILE_TITLE, title)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    companion object {

        private const val LOG_TAG: String = "AlarmReceiver"
        const val EXTRA_ALARM: String = "extra_alarm"
        const val EXTRA_PROFILE: String = "extra_profile"
        const val EXTRA_ALARM_ID: String = "extra_alarm_id"
    }
}