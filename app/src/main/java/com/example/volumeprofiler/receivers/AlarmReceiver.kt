package com.example.volumeprofiler.receivers

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.services.StatsService
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.ParcelableUtil
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver: BroadcastReceiver() {

    @Inject lateinit var profileUtil: ProfileUtil
    @Inject lateinit var alarmUtil: AlarmUtil
    @Inject lateinit var sharedPreferencesUtil: SharedPreferencesUtil

    @SuppressWarnings("unchecked")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == Application.ACTION_ALARM_TRIGGER) {
            val alarm: Alarm = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_ALARM)!!, ParcelableUtil.getParcelableCreator())
            val profile: Profile = ParcelableUtil.toParcelable(intent.getByteArrayExtra(EXTRA_PROFILE)!!, ParcelableUtil.getParcelableCreator())
            val result: Long = alarmUtil.setAlarm(alarm, profile, true)
            if (result != (-1).toLong()) {
                alarmUtil.cancelAlarm(alarm, profile)
                // TODO update database
            }
            profileUtil.applyProfile(profile)
            if (isServiceRunning(context)) {
                updateNotification(context)
            } else {
                // TODO update recyclerView items using event bus implementation
            }
            sharedPreferencesUtil.writeCurrentProfileProperties(profile)
        }
    }

    private fun updateNotification(context: Context): Unit {
        val intent: Intent = Intent(context, StatsService::class.java).apply {
            this.putExtra(StatsService.EXTRA_UPDATE_NOTIFICATION, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        }
        else {
            context.startService(intent)
        }
    }

    @Suppress("deprecation")
    private fun isServiceRunning(context: Context?): Boolean {
        val serviceName: String = StatsService::class.java.name
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
        const val EXTRA_ALARM: String = "extra_alarm"
        const val EXTRA_PROFILE: String = "extra_profile"
    }
}