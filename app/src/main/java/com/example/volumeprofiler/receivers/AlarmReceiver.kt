package com.example.volumeprofiler.receivers

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import android.util.Log
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.services.NotificationWidgetService
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.ProfileUtil
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.HashMap

class AlarmReceiver: BroadcastReceiver() {

    @SuppressWarnings("unchecked")
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("AlarmReceiver", "onReceive")
        if (context != null && intent?.action == Application.ACTION_ALARM_TRIGGER) {

            val profileUtil: ProfileUtil = ProfileUtil.getInstance()
            val alarmUtil = AlarmUtil.getInstance()

            val profileTitle: String = intent.getStringExtra(EXTRA_PROFILE_TITLE) as String
            val primaryVolumeSettings: Map<Int, Int> = intent.getSerializableExtra(EXTRA_PRIMARY_VOLUME_SETTINGS) as HashMap<Int, Int>
            val optionalVolumeSettings: Map<String, Int> = intent.getSerializableExtra(EXTRA_OPTIONAL_VOLUME_SETTINGS) as HashMap<String, Int>
            val eventOccurrences: Array<Int> = intent.extras?.get(EXTRA_EVENT_OCCURRENCES) as Array<Int>
            val eventId: Long = intent.extras?.getLong(EXTRA_ALARM_ID) as Long
            val profileId: UUID = intent.extras?.getSerializable(EXTRA_PROFILE_ID) as UUID
            val eventTime: LocalDateTime = intent.extras?.getSerializable(EXTRA_ALARM_TRIGGER_TIME) as LocalDateTime

            val result: Long = alarmUtil.setAlarm(Pair(primaryVolumeSettings, optionalVolumeSettings), eventOccurrences,
                    eventTime, eventId, true, profileId, profileTitle)
            if (result > 0) {
                goAsync(GlobalScope, Dispatchers.IO) {
                    Repository.get().updateTriggeredEvent(result)
                }
            }
            profileUtil.applyAudioSettings(primaryVolumeSettings, optionalVolumeSettings, profileId, profileTitle)
            profileUtil.sendLocalBroadcast(profileId)
            if (isServiceRunning(context)) {
                updateNotification(context)
            }
        }
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
        const val PREFS_PROFILE_ID = "prefs_profile_id"
        const val PREFS_PROFILE_STREAM_ALARM = "prefs_profile_stream_alarm"
        const val PREFS_PROFILE_STREAM_VOICE_CALL = "prefs_profile_voice_call"
        const val PREFS_PROFILE_STREAM_MUSIC = "prefs_profile_stream_music"
        const val PREFS_PROFILE_STREAM_NOTIFICATION = "prefs_profile_stream_notification"
        const val PREFS_PROFILE_STREAM_RING = "prefs_profile_streams_ring"
        const val PREFS_PROFILE_TITLE = "prefs_profile_title"
        const val EXTRA_PROFILE_TITLE = "extra_profile_title"
        const val EXTRA_ALARM_TRIGGER_TIME = "alarm_trigger_time"
        const val EXTRA_PRIMARY_VOLUME_SETTINGS = "primary_volume_settings"
        const val EXTRA_OPTIONAL_VOLUME_SETTINGS = "optional_volume_settings"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_EVENT_OCCURRENCES = "event_occurrences"
    }
}