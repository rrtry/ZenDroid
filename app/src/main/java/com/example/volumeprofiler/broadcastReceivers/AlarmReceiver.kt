package com.example.volumeprofiler.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.time.LocalDateTime

@AndroidEntryPoint
class AlarmReceiver: BroadcastReceiver() {

    @Inject
    lateinit var profileUtil: ProfileUtil

    @Inject
    lateinit var alarmUtil: AlarmUtil

    @Inject
    lateinit var eventBus: EventBus

    override fun onReceive(context: Context?, intent: Intent?) {
        /*
        if (intent?.action == Application.ACTION_ALARM_ALERT) {

            val context: Context = context as Context

            val alarm: Alarm = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_ALARM)!!, ParcelableUtil.getParcelableCreator())
            val profile: Profile = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_PROFILE)!!, ParcelableUtil.getParcelableCreator())

            val scheduled: Boolean = alarmUtil.scheduleAlarm(alarm, profile, true)

            if (!scheduled) {
                alarmUtil.cancelAlarm(alarm, profile)
                startAlarmCancelService(context, alarm.id, profile.title)
            } else {
                startAlarmUpdateService(context,
                    alarm.id, profile.title,
                    AlarmUtil.getAlarmNextDate(alarm.localDateTime.toLocalTime(), alarm.scheduledDays))
            }

            if (profileUtil.grantedRequiredPermissions(profile)) {
                profileUtil.setProfile(profile)
                eventBus.updateProfilesFragment(profile.id)
                postNotification(context, createAlarmAlertNotification(context, profile.title, alarm.localDateTime.toLocalTime()), ID_SCHEDULER)
            } else {
                notifyAboutDeniedPermissions(context)
            }
             */
        }
    /*

    private fun notifyAboutDeniedPermissions(context: Context): Unit {
        val missingPermissions: List<String> = profileUtil.getMissingPermissions()
        if (missingPermissions.isNotEmpty()) {
            postNotification(context, createMissingPermissionNotification(context, missingPermissions), ID_PERMISSIONS)
        }
        if (!profileUtil.canWriteSettings()) {
            postNotification(context, createSystemSettingsNotification(context), ID_SYSTEM_SETTINGS)
        }
        if (!profileUtil.isNotificationPolicyAccessGranted()) {
            postNotification(context, createInterruptionPolicyNotification(context), ID_INTERRUPTION_POLICY)
        }
    }

    private fun startService(context: Context, intent: Intent): Unit {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun startAlarmUpdateService(context: Context, alarmId: Long, title: String, localDateTime: LocalDateTime): Unit {
        val intent: Intent = Intent(context, AlarmUpdateService::class.java).apply {
            action = ACTION_UPDATE_ALARM_DATE
            putExtra(AlarmUpdateService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmUpdateService.EXTRA_PROFILE_TITLE, title)
            putExtra(AlarmUpdateService.EXTRA_LOCAL_DATE_TIME, localDateTime)
        }
        startService(context, intent)
    }

    private fun startAlarmCancelService(context: Context, alarmId: Long, title: String): Unit {
        val intent: Intent = Intent(context, AlarmUpdateService::class.java).apply {
            action = ACTION_CANCEL_ALARM
            putExtra(AlarmUpdateService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmUpdateService.EXTRA_PROFILE_TITLE, title)
        }
        startService(context, intent)
    }
     */

    companion object {

        private const val LOG_TAG: String = "AlarmReceiver"
        const val EXTRA_ALARM: String = "extra_alarm"
        const val EXTRA_PROFILE: String = "extra_profile"
        const val EXTRA_ALARM_ID: String = "extra_alarm_id"
    }
}