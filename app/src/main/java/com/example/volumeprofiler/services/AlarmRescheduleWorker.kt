package com.example.volumeprofiler.services

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.WorkerParameters
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import android.util.Log
import com.example.volumeprofiler.R
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.util.AlarmUtil

class AlarmRescheduleWorker constructor (val context: Context, args: WorkerParameters): CoroutineWorker(context, args) {

    override suspend fun doWork(): Result {
        return try {
            setForeground(createForegroundInfo())
            val repository: Repository = Repository.get()
            val toSchedule: List<ProfileAndEvent>? = repository.getProfilesWithScheduledEvents()
            if (toSchedule != null) {
                performRescheduling(toSchedule)
            }
            Log.i("AlarmRescheduleWorker", "doWork() success")
            Result.success()
        }
        catch (throwable: Throwable) {
            Log.i("AlarmRescheduleWorker", "doWork() failure", throwable)
            Result.failure()
        }
    }

    private fun performRescheduling(list: List<ProfileAndEvent>) {
        Log.i("AlarmRescheduleWorker", "performRescheduling, amount of alarms: ${list.size}")
        val alarmUtil = AlarmUtil(context.applicationContext)
        alarmUtil.setMultipleAlarms(list)
    }

    private fun createNotification(): Notification {
        val intent = WorkManager.getInstance(context).createCancelPendingIntent(id)
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Rescheduling events")
            .setSmallIcon(R.drawable.baseline_alarm_deep_purple_300_24dp)
            .setOngoing(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel().also {
                builder.setChannelId(it.id)
            }
        }
        return builder.build()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notificationId = NOTIFICATION_ID
        return ForegroundInfo(notificationId, createNotification())
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): NotificationChannel {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        ).also { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {

        private const val NOTIFICATION_ID: Int = 162
        private const val NOTIFICATION_CHANNEL_ID: String = "channel_01"
        private const val NOTIFICATION_CHANNEL_NAME: String = "VolumeProfiler"
    }
}