package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import java.time.LocalDateTime
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.volumeprofiler.fragments.ProfilesListFragment
import com.example.volumeprofiler.fragments.ProfilesListFragment.Companion.SHARED_PREFERENCES
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.AudioUtil
import java.util.*
import kotlin.collections.HashMap

class AlarmReceiver: BroadcastReceiver() {

    @SuppressWarnings("unchecked")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == ACTION_TRIGGER_ALARM) {
            Log.i("AlarmReceiver", "onReceive")
            val primaryVolumeSettings: Map<Int, Int> = intent.getSerializableExtra(EXTRA_PRIMARY_VOLUME_SETTINGS) as HashMap<Int, Int>
            val optionalVolumeSettings: Map<String, Int> = intent.getSerializableExtra(EXTRA_OPTIONAL_VOLUME_SETTINGS) as HashMap<String, Int>
            val eventOccurrences: Array<Int> = intent.extras?.get(EXTRA_EVENT_OCCURRENCES) as Array<Int>
            val eventId: Long = intent.extras?.get(EXTRA_ALARM_ID) as Long
            val profileId: UUID = intent.extras?.get(EXTRA_PROFILE_ID) as UUID
            val eventTime: LocalDateTime = intent.extras?.get(EXTRA_ALARM_TRIGGER_TIME) as LocalDateTime
            val alarmUtil = AlarmUtil(context.applicationContext)
            alarmUtil.setAlarm(Pair(primaryVolumeSettings, optionalVolumeSettings), eventOccurrences,
                    eventTime, eventId, true, profileId)
            AudioUtil.applyAudioSettings(context, Pair(primaryVolumeSettings, optionalVolumeSettings))
            //sendBroadcastToUpdateUI(context.applicationContext, profileId)
        }
    }

    private fun sendBroadcastToUpdateUI(context: Context, profileId: UUID): Unit {
        val localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
        val intent: Intent = Intent(ACTION_UPDATE_UI).apply {
            this.putExtra(EXTRA_PROFILE_ID, profileId)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    companion object {

        const val PREFS_PROFILE_ID = "prefs_profile_id"
        const val PREFS_PROFILE_NOTIFICATION_VOLUME = "prefs_profile_notification_volume"
        const val PREFS_PROFILE_RING_VOLUME = "prefs_profile_ring_volume"
        const val ACTION_UPDATE_UI = "update_ui"
        const val EXTRA_ALARM_TRIGGER_TIME = "alarm_trigger_time"
        const val EXTRA_PRIMARY_VOLUME_SETTINGS = "primary_volume_settings"
        const val EXTRA_OPTIONAL_VOLUME_SETTINGS = "optional_volume_settings"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_EVENT_OCCURRENCES = "event_occurrences"
        const val ACTION_TRIGGER_ALARM = "trigger_alarm"
    }
}