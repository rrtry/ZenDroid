package com.example.volumeprofiler.util

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.volumeprofiler.R
import com.example.volumeprofiler.services.SchedulerService
import java.time.LocalTime

private const val PROFILE_NOTIFICATION_CHANNEL_ID: String = "channel_163"
private const val PROFILE_NOTIFICATION_CHANNEL_NAME: String = "Activating profiles"

fun postNotification(context: Context, notification: Notification): Unit {
    val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(100, notification)
}

fun createSchedulerNotification(context: Context): Notification {
    val builder = NotificationCompat.Builder(context, SchedulerService.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Scheduling alarms")
            .setSmallIcon(R.drawable.baseline_alarm_deep_purple_300_24dp)
            .setOngoing(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
                context,
                SchedulerService.NOTIFICATION_CHANNEL_ID,
                SchedulerService.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

fun createProfileNotification(context: Context, title: String, localTime: LocalTime): Notification {
    val builder = NotificationCompat.Builder(context, PROFILE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Scheduled profile activation")
            .setContentText("$title at ${TextUtil.localizedTimeToString(localTime)}")
            .setSmallIcon(R.drawable.baseline_volume_down_deep_purple_300_24dp)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
                context,
                PROFILE_NOTIFICATION_CHANNEL_ID,
                PROFILE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

@TargetApi(Build.VERSION_CODES.O)
fun createNotificationChannel(context: Context, channelId: String, channelName: String, importance: Int): NotificationChannel {
    val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return NotificationChannel(
            channelId, channelName, importance
    ).also { channel ->
        notificationManager.createNotificationChannel(channel)
    }
}