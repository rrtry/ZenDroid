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
import com.example.volumeprofiler.activities.MainActivity
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.util.AlarmUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AlarmRescheduleService: Service() {

    private suspend fun doWork(): Unit {
        val repository: Repository = Repository.get()
        val toSchedule: List<ProfileAndEvent>? = repository.getProfilesWithScheduledEvents()
        if (toSchedule != null) {
            Log.i(LOG_TAG, "setting the alarms again")
            val alarmUtil = AlarmUtil(this.applicationContext)
            alarmUtil.setMultipleAlarms(toSchedule)
        }
        else {
            Log.i(LOG_TAG, "there are no alarms set")
        }
    }

    private fun createNotification(contentIntent: PendingIntent): Notification {
        val builder = NotificationCompat.Builder(this, 1.toString())
                .setContentTitle("Rescheduling alarms")
                .setSmallIcon(R.drawable.baseline_alarm_deep_purple_300_24dp)
                .setOngoing(true)
                .setContentIntent(contentIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel().also {
                builder.setChannelId(it.id)
            }
        }
        return builder.build()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): NotificationChannel {
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        ).also { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("AlarmRescheduleService", "onStartCommand()")
        val pendingIntent: PendingIntent =
                Intent(this, MainActivity::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(this, ACTIVITY_REQUEST_CODE, notificationIntent, 0)
                }
        startForeground(SERVICE_ID, createNotification(pendingIntent))
        GlobalScope.launch {
            doWork()
            stopForeground(true)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(LOG_TAG, "onDestroy()")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {

        private const val LOG_TAG: String = "AlarmRescheduleService"
        private const val NOTIFICATION_CHANNEL_ID: String = "channel_162"
        private const val NOTIFICATION_CHANNEL_NAME: String = "channel_volumeprofiler"
        private const val SERVICE_ID: Int = 162
        private const val ACTIVITY_REQUEST_CODE: Int = 0
    }
}