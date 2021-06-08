package com.example.volumeprofiler.services

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.volumeprofiler.R
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.util.AlarmUtil
import kotlinx.coroutines.*

class AlarmRescheduleService: Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private suspend fun doWork(): Unit {
        val repository: Repository = Repository.get()
        val toSchedule: List<ProfileAndEvent>? = repository.getProfilesWithScheduledEvents()
        if (toSchedule != null && toSchedule.isNotEmpty()) {
            Log.i(LOG_TAG, "setting the alarms again")
            val alarmUtil = AlarmUtil(this)
            alarmUtil.setMultipleAlarms(toSchedule)
        }
        else {
            Log.i(LOG_TAG, "there are no alarms")
        }
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Rescheduling alarms")
                .setSmallIcon(R.drawable.baseline_alarm_deep_purple_300_24dp)
                .setOngoing(true)
                .setNotificationSilent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel().also {
                builder.setChannelId(it.id)
            }
        }
        return builder.build()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): NotificationChannel {
        return NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        ).also { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("AlarmRescheduleService", "onStartCommand()")
        startForeground(SERVICE_ID, createNotification())
        scope.launch {
            val request = launch {
                doWork()
            }
            request.join()
            stopForeground(true)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "onDestroy")
        job.cancel()
        super.onDestroy()
    }

    companion object {

        private const val LOG_TAG: String = "AlarmRescheduleService"
        private const val NOTIFICATION_CHANNEL_ID: String = "channel_162"
        private const val NOTIFICATION_CHANNEL_NAME: String = "Service's notification"
        private const val SERVICE_ID: Int = 162
        private const val ACTIVITY_REQUEST_CODE: Int = 0
    }
}