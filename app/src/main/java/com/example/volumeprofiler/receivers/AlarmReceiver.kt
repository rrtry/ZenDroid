package com.example.volumeprofiler.receivers

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.services.NotificationWidgetService
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.ParcelableUtil
import com.example.volumeprofiler.util.ProfileUtil
import kotlinx.coroutines.*
import java.util.*

class AlarmReceiver: BroadcastReceiver() {

    @SuppressWarnings("unchecked")
    override fun onReceive(context: Context?, intent: Intent?) {

        if (context != null && intent?.action == Application.ACTION_ALARM_TRIGGER) {
            val alarm: Alarm = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_ALARM)!!, ParcelableUtil.getParcelableCreator<Alarm>())
            val profile: Profile = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_PROFILE)!!, ParcelableUtil.getParcelableCreator<Profile>())
            val result: Long = setAlarm(alarm, profile)
            if (result != (-1).toLong()) {
                cancelAlarm(alarm, profile)
                // Repository.get().updateTriggeredAlarm(alarm) start service
            }
            else {
                setAlarm(alarm, profile)
            }
            applyAudioSettings(profile)
            sendLocalBroadcast(profile.id)
            if (isServiceRunning(context)) {
                updateNotification(context)
            }
        }
    }

    private fun cancelAlarm(alarm: Alarm, profile: Profile): Unit {
        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
        alarmUtil.cancelAlarm(alarm, profile)
    }

    private fun setAlarm(alarm: Alarm, profile: Profile): Long {
        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
        return alarmUtil.setAlarm(alarm, profile, true)
    }

    private fun sendLocalBroadcast(id: UUID): Unit {
        val profileUtil: ProfileUtil = ProfileUtil.getInstance()
        profileUtil.sendLocalBroadcast(id)
    }

    private fun applyAudioSettings(profile: Profile): Unit {
        val profileUtil: ProfileUtil = ProfileUtil.getInstance()
        profileUtil.applyAudioSettings(profile)
    }

    private fun updateNotification(context: Context): Unit {
        val intent: Intent = Intent(context, NotificationWidgetService::class.java).apply {
            this.putExtra(NotificationWidgetService.EXTRA_UPDATE_NOTIFICATION, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        }
        else {
            context.startService(intent)
        }
    }

    @SuppressWarnings("deprecation")
    private fun isServiceRunning(context: Context?): Boolean {
        val serviceName: String = NotificationWidgetService::class.java.name
        val activityManager: ActivityManager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = activityManager.getRunningServices(Int.MAX_VALUE)
        for (i in services) {
            if (i.service.className == serviceName) {
                return true
            }
        }
        return false
    }

    private fun BroadcastReceiver.goAsync(
            coroutineScope: CoroutineScope,
            dispatcher: CoroutineDispatcher,
            block: suspend () -> Unit
    ) {
        val pendingResult = goAsync()
        coroutineScope.launch(dispatcher) {
            block()
            pendingResult.finish()
        }
    }

    companion object {

        private const val LOG_TAG: String = "AlarmReceiver"
        const val PREFS_PROFILE_ID: String = "prefs_profile_id"
        const val PREFS_PROFILE_STREAM_NOTIFICATION: String = "prefs_profile_stream_notification"
        const val PREFS_PROFILE_STREAM_RING: String = "prefs_profile_streams_ring"
        const val PREFS_PROFILE_TITLE: String = "prefs_profile_title"
        const val EXTRA_PROFILE_ID: String = "profile_id"
        const val EXTRA_ALARM: String = "extra_alarm"
        const val EXTRA_PROFILE: String = "extra_profile"
    }
}