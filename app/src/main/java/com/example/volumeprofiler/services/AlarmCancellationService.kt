package com.example.volumeprofiler.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.util.createAlarmCancelNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmCancellationService: Service() {

    private val job: Job = Job()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var wakeLock: PowerManager.WakeLock

    @Inject
    lateinit var repository: AlarmRepository

    private suspend fun updateAlarm(id: Long): Unit {
        val alarm: Alarm = repository.getScheduledAlarm(id)
        alarm.isScheduled = 0
        repository.updateAlarm(alarm)
    }

    override fun onCreate() {
        super.onCreate()
        wakeLock = createWakeLock()
        if (!wakeLock.isHeld) {
            acquireWakeLock()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(SERVICE_ID, createAlarmCancelNotification(this, intent?.getStringExtra(EXTRA_PROFILE_TITLE) ?: ""))

        val alarmId: Long? = intent?.getLongExtra(EXTRA_ALARM_ID, -1)
        if (alarmId != null) {
            scope.launch {
                updateAlarm(alarmId)
                stopService()
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun stopService(): Unit {
        stopForeground(true)
        stopSelf(SERVICE_ID)
    }

    private fun releaseWakeLock(): Unit {
        wakeLock.release()
    }

    private fun acquireWakeLock(): Unit {
        val timeout: Long = 1 * 60 * 1000L
        wakeLock.acquire(timeout)
    }

    private fun createWakeLock(): PowerManager.WakeLock {
        val powerManager: PowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Volumeprofiler::WAKE_LOCK_TAG")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        job.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {

        private const val WAKE_LOCK_TAG: String = "Volumeprofiler::WAKE_LOCK_TAG"
        private const val SERVICE_ID: Int = 163
        const val EXTRA_ALARM_ID: String = "extra_alarm_id"
        const val EXTRA_PROFILE_TITLE: String = "extra_profile_title"
    }
}