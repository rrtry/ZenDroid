package com.example.volumeprofiler.util

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings.*
import androidx.core.app.NotificationCompat
import com.example.volumeprofiler.R
import java.time.LocalTime

private const val PROFILE_NOTIFICATION_CHANNEL_ID: String = "CHANNEL_ID_PROFILE"
private const val SERVICE_NOTIFICATION_CHANNEL_ID: String = "CHANNEL_ID_SERVICE"
private const val PERMISSIONS_NOTIFICATION_CHANNEL_ID: String = "CHANNEL_ID_PERMISSIONS"

private const val PROFILE_NOTIFICATION_CHANNEL_NAME: String = "Activating profiles"
private const val SERVICE_NOTIFICATION_CHANNEL_NAME: String = "Background processing"
private const val PERMISSIONS_NOTIFICATION_CHANNEL_NAME: String = "Warnings"

const val ID_SYSTEM_SETTINGS: Int = 1072
const val ID_INTERRUPTION_POLICY: Int = 2073
const val ID_PERMISSIONS: Int = 3074
const val ID_SCHEDULER: Int = 4075
const val ID_GEOFENCE: Int = 5076

fun postNotification(context: Context, notification: Notification, id: Int): Unit {
    val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(id,  notification)
}

fun createMissingPermissionsNotification(context: Context, permissions: List<String>): Notification {
    val contentTitle: String = if (permissions.size > 1) "Permissions required" else "Permission required"
    val pNames: String = permissions.map { TextUtil.getPermissionName(it) }.toString().removeSurrounding("[", "]")
    val contentText: String = if (permissions.size > 1) "Missing '$pNames' permissions" else "Missing '$pNames' permission"
    val builder = NotificationCompat.Builder(context, PERMISSIONS_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.baseline_volume_down_deep_purple_300_24dp)
            .setContentIntent(getAppDetailsPendingIntent(context))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
                context,
                PERMISSIONS_NOTIFICATION_CHANNEL_ID,
                PERMISSIONS_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

fun createSystemSettingsNotification(context: Context): Notification {
    val builder = NotificationCompat.Builder(context, PERMISSIONS_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Write system settings")
            .setContentText("Click to open settings")
            .setContentIntent(getSystemSettingsPendingIntent(context))
            .setSmallIcon(R.drawable.baseline_volume_down_deep_purple_300_24dp)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
                context,
                PERMISSIONS_NOTIFICATION_CHANNEL_ID,
                PERMISSIONS_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

fun createInterruptionPolicyNotification(context: Context): Notification {
    val builder = NotificationCompat.Builder(context, PERMISSIONS_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Do not disturb access")
            .setContentText("Click to open settings")
            .setContentIntent(getInterruptionPolicyPendingIntent(context))
            .setSmallIcon(R.drawable.baseline_volume_down_deep_purple_300_24dp)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
                context,
                PERMISSIONS_NOTIFICATION_CHANNEL_ID,
                PERMISSIONS_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

private fun getSystemSettingsPendingIntent(context: Context): PendingIntent {
    val intent: Intent = Intent(ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
    return PendingIntent.getActivity(context, 0, intent, 0)
}

private fun getInterruptionPolicyPendingIntent(context: Context): PendingIntent {
    return PendingIntent.getActivity(context, 1, Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), 0)
}

private fun getAppDetailsPendingIntent(context: Context): PendingIntent {
    val intent: Intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    return PendingIntent.getActivity(context, 2, intent, 0)
}

fun createProximityAlertNotification(context: Context, profileTitle: String, address: String): Notification {
    val builder = NotificationCompat.Builder(context, PROFILE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Profile activation depending on location")
            .setContentText("$profileTitle in $address")
            .setSmallIcon(R.drawable.baseline_location_on_black_24dp)
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

fun createGeofenceRegistrationNotification(context: Context): Notification {
    val builder = NotificationCompat.Builder(context, SERVICE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Registering geofences")
            .setContentText("Google play services data has been cleared")
            .setSmallIcon(R.drawable.baseline_location_on_black_24dp)
            .setOngoing(true)
            .setSilent(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
                context,
                SERVICE_NOTIFICATION_CHANNEL_ID,
                SERVICE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

fun createAlarmCancelNotification(context: Context, title: String): Notification {
    val builder = NotificationCompat.Builder(context, SERVICE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Disabling scheduled activation for $title")
            .setSmallIcon(R.drawable.baseline_alarm_deep_purple_300_24dp)
            .setOngoing(true)
            .setSilent(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
                context,
                SERVICE_NOTIFICATION_CHANNEL_ID,
                SERVICE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

fun createSchedulerNotification(context: Context): Notification {
    val builder = NotificationCompat.Builder(context, SERVICE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Scheduling alarms")
            .setSmallIcon(R.drawable.baseline_alarm_deep_purple_300_24dp)
            .setOngoing(true)
            .setSilent(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
                context,
                SERVICE_NOTIFICATION_CHANNEL_ID,
                SERVICE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

fun createAlarmAlertNotification(context: Context, title: String, localTime: LocalTime): Notification {
    val builder = NotificationCompat.Builder(context, PROFILE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Profile activation on schedule")
            .setContentText("$title at ${TextUtil.localizedTimeToString(localTime)}")
            .setSmallIcon(R.drawable.baseline_alarm_deep_purple_300_24dp)
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
private fun createNotificationChannel(context: Context, channelId: String, channelName: String, importance: Int): NotificationChannel {
    val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return NotificationChannel(
            channelId, channelName, importance
    ).also { channel ->
        notificationManager.createNotificationChannel(channel)
    }
}