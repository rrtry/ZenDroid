package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import java.time.LocalDateTime
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.AudioUtil
import java.util.*
import kotlin.collections.HashMap

class AlarmReceiver: BroadcastReceiver() {

    @SuppressWarnings("unchecked")
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("AlarmReceiver", "onReceive")
        if (context != null && intent?.action == Application.ACTION_TRIGGER_ALARM) {
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
            saveProfileId(context, profileId)
        }
    }

    private fun saveProfileId(context: Context, id: UUID): Unit {
        val storageContext: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            context.createDeviceProtectedStorageContext() else context
        val sharedPreferences: SharedPreferences = storageContext.getSharedPreferences(Application.SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(PREFS_PROFILE_ID, id.toString())
        editor.apply()
    }

    private fun sendBroadcastToUpdateUI(context: Context, profileId: UUID): Unit {
        val localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
        val intent: Intent = Intent(Application.ACTION_UPDATE_UI).apply {
            this.putExtra(EXTRA_PROFILE_ID, profileId)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    companion object {

        private const val LOG_TAG: String = "AlarmReceiver"
        const val PREFS_PROFILE_ID = "prefs_profile_id"
        const val PREFS_PROFILE_STREAM_ALARM = "prefs_profile_stream_alarm"
        const val PREFS_PROFILE_STREAM_VOICE_CALL = "prefs_profile_voice_call"
        const val PREFS_PROFILE_STREAM_MUSIC = "prefs_profile_stream_music"
        const val PREFS_PROFILE_STREAM_NOTIFICATION = "prefs_profile_stream_notification"
        const val PREFS_PROFILE_STREAM_RING = "prefs_profile_streams_ring"
        const val EXTRA_ALARM_TRIGGER_TIME = "alarm_trigger_time"
        const val EXTRA_PRIMARY_VOLUME_SETTINGS = "primary_volume_settings"
        const val EXTRA_OPTIONAL_VOLUME_SETTINGS = "optional_volume_settings"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_EVENT_OCCURRENCES = "event_occurrences"
    }
}