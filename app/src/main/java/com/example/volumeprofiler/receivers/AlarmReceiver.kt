package com.example.volumeprofiler.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import android.util.Log
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.AudioUtil

class AlarmReceiver: BroadcastReceiver() {

    @SuppressWarnings("unchecked")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == ALARM_TRIGGER) {
            Log.i("AlarmReceiver", "onReceive")
            val primaryVolumeSettings: Map<Int, Int> = intent.getSerializableExtra(EXTRA_PRIMARY_VOLUME_SETTINGS) as HashMap<Int, Int>
            val optionalVolumeSettings: Map<String, Int> = intent.getSerializableExtra(EXTRA_OPTIONAL_VOLUME_SETTINGS) as HashMap<String, Int>
            val eventOccurrences: Array<Int> = intent.extras?.get(EXTRA_EVENT_OCCURRENCES) as Array<Int>
            val eventId: Long = intent.extras?.get(EXTRA_ALARM_ID) as Long
            val eventTime: LocalDateTime = intent.extras?.get(EXTRA_ALARM_TRIGGER_TIME) as LocalDateTime
            val alarmUtil = AlarmUtil(context.applicationContext)
            alarmUtil.setAlarm(Pair(primaryVolumeSettings, optionalVolumeSettings), eventOccurrences, eventTime, eventId, true)
            AudioUtil.applyAudioSettings(context, Pair(primaryVolumeSettings, optionalVolumeSettings))
        }
    }

    companion object {

        const val EXTRA_ALARM_TRIGGER_TIME = "alarm_trigger_time"
        const val EXTRA_PRIMARY_VOLUME_SETTINGS = "extra_primary_volume_settings"
        const val EXTRA_OPTIONAL_VOLUME_SETTINGS = "extra_optional_volume_settings"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_EVENT_OCCURRENCES = "event_occurrences"
        const val ALARM_TRIGGER = "alarm_trigger"
    }
}