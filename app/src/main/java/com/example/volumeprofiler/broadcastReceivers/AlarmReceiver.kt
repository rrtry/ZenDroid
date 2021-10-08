package com.example.volumeprofiler.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
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
    lateinit var sharedPreferencesUtil: SharedPreferencesUtil

    @Inject
    lateinit var eventBus: EventBus

    @SuppressWarnings("unchecked")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Application.ACTION_ALARM_TRIGGER) {

            val alarm: Alarm = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_ALARM)!!, ParcelableUtil.getParcelableCreator())
            val profile: Profile = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_PROFILE)!!, ParcelableUtil.getParcelableCreator())

            if (!alarmUtil.scheduleAlarm(alarm, profile, true)) {
                alarmUtil.cancelAlarm(alarm, profile)
                // TODO start service and initiate database operation for cancelled alarms
            }

            profileUtil.setProfile(profile)
            sharedPreferencesUtil.writeCurrentProfileProperties(profile)

            eventBus.updateProfilesFragment(profile.id)
            postNotification(context!!, createProfileNotification(context, profile.title, alarm.localDateTime.toLocalTime()))
        }
    }

    companion object {

        private const val LOG_TAG: String = "AlarmReceiver"
        const val EXTRA_ALARM: String = "extra_alarm"
        const val EXTRA_PROFILE: String = "extra_profile"
        const val EXTRA_ALARM_ID: String = "extra_alarm_id"
    }
}