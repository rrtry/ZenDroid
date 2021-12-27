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
import android.content.Intent.*
import android.provider.Settings.*
import androidx.core.app.NotificationCompat
import com.example.volumeprofiler.R
import com.example.volumeprofiler.entities.Event
import com.example.volumeprofiler.entities.Profile
import java.time.LocalTime

private const val SERVICE_NOTIFICATION_CHANNEL_ID: String = "CHANNEL_ID_SERVICES"
private const val PERMISSIONS_NOTIFICATION_CHANNEL_ID: String = "CHANNEL_ID_PERMISSIONS"
private const val SCHEDULER_NOTIFICATION_CHANNEL_ID: String = "CHANNEL_ID_SCHEDULER"
private const val CALENDAR_EVENTS_NOTIFICATION_CHANNEL_ID: String = "CHANNEL_ID_CALENDAR_EVENTS"
private const val GEOFENCES_NOTIFICATION_CHANNEL_ID: String = "CHANNEL_ID_GEOFENCES"

private const val SERVICES_NOTIFICATION_CHANNEL_NAME: String = "Background processing"
private const val PERMISSIONS_NOTIFICATION_CHANNEL_NAME: String = "Permission reminders"
private const val SCHEDULER_NOTIFICATION_CHANNEL_NAME: String = "Alarms"
private const val GEOFENCES_NOTIFICATION_CHANNEL_NAME: String = "Geofences"
private const val CALENDAR_EVENTS_NOTIFICATION_CHANNEL_NAME: String = "Calendar events"

const val ID_SYSTEM_SETTINGS: Int = 1072
const val ID_INTERRUPTION_POLICY: Int = 2073
const val ID_PERMISSIONS: Int = 3074
const val ID_SCHEDULER: Int = 4075
const val ID_GEOFENCE: Int = 5076
const val ID_CALENDAR_EVENT: Int = 6077

private const val REQUEST_GRANT_WRITE_SETTINGS_PERMISSION: Int = 0
private const val REQUEST_GRANT_NOTIFICATION_POLICY_PERMISSION: Int = 1
private const val REQUEST_LAUNCH_APPLICATION_DETAILS_SETTINGS: Int = 2

fun getApplicationSettingsIntent(context: Context): Intent {
    val intent: Intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
        flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
    }
    return intent
}

private fun getSystemSettingsPendingIntent(context: Context): PendingIntent {
    val intent: Intent = Intent(ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
        flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
    }
    return PendingIntent.getActivity(context, REQUEST_GRANT_WRITE_SETTINGS_PERMISSION, intent, PendingIntent.FLAG_IMMUTABLE)
}

private fun getInterruptionPolicyPendingIntent(context: Context): PendingIntent {
    val intent: Intent = Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
        flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
    }
    return PendingIntent.getActivity(context, REQUEST_GRANT_NOTIFICATION_POLICY_PERMISSION, intent, PendingIntent.FLAG_IMMUTABLE)
}

private fun getAppDetailsPendingIntent(context: Context): PendingIntent {
    val intent: Intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
        flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
    }
    return PendingIntent.getActivity(context, REQUEST_LAUNCH_APPLICATION_DETAILS_SETTINGS, intent, PendingIntent.FLAG_IMMUTABLE)
}

fun sendSystemPreferencesAccessNotification(context: Context, profileUtil: ProfileUtil): Unit {
    if (!profileUtil.canModifySystemPreferences()) {
        postNotification(context, createSystemSettingsNotification(context), ID_SYSTEM_SETTINGS)
    }
    if (!profileUtil.isNotificationPolicyAccessGranted()) {
        postNotification(context, createInterruptionPolicyNotification(context), ID_INTERRUPTION_POLICY)
    }
}

fun postNotification(context: Context, notification: Notification, id: Int): Unit {
    val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(id, notification)
}

fun cancelPermissionNotifications(context: Context): Unit {
    val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(ID_PERMISSIONS)
    notificationManager.cancel(ID_SYSTEM_SETTINGS)
    notificationManager.cancel(ID_INTERRUPTION_POLICY)
}

fun createMissingPermissionNotification(context: Context, permissions: List<String>): Notification {
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
    val notification: Notification = builder.build()
    notification.flags = Notification.FLAG_AUTO_CANCEL
    return notification
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
    val notification: Notification = builder.build()
    notification.flags = Notification.FLAG_AUTO_CANCEL
    return notification
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
    val notification: Notification = builder.build()
    notification.flags = Notification.FLAG_AUTO_CANCEL
    return notification
}

fun createGeofenceExitNotification(context: Context, profileTitle: String, locationTitle: String): Notification {
    val builder = NotificationCompat.Builder(context, GEOFENCES_NOTIFICATION_CHANNEL_ID)
        .setContentTitle(GEOFENCES_NOTIFICATION_CHANNEL_NAME)
        .setContentText("Activating profile '$profileTitle'")
        .setSmallIcon(R.drawable.baseline_location_on_black_24dp)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
            context,
            GEOFENCES_NOTIFICATION_CHANNEL_ID,
            GEOFENCES_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

fun createGeofenceEnterNotification(context: Context, profileTitle: String, locationTitle: String): Notification {
    val builder = NotificationCompat.Builder(context, GEOFENCES_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(GEOFENCES_NOTIFICATION_CHANNEL_NAME)
            .setContentText("Activating profile '$profileTitle'")
            .setSmallIcon(R.drawable.baseline_location_on_black_24dp)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
                context,
                GEOFENCES_NOTIFICATION_CHANNEL_ID,
                GEOFENCES_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

fun createGeofenceRegistrationNotification(context: Context): Notification {
    val builder = NotificationCompat.Builder(context, SERVICE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(SERVICES_NOTIFICATION_CHANNEL_NAME)
            .setContentText("Registering geofences")
            .setSmallIcon(R.drawable.baseline_location_on_black_24dp)
            .setOngoing(true)
            .setSilent(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
                context,
                SERVICE_NOTIFICATION_CHANNEL_ID,
                SERVICES_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

fun createSchedulerNotification(context: Context): Notification {
    val builder = NotificationCompat.Builder(context, SERVICE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(SERVICES_NOTIFICATION_CHANNEL_NAME)
            .setContentText("Scheduling alarms")
            .setSmallIcon(R.drawable.baseline_alarm_deep_purple_300_24dp)
            .setOngoing(true)
            .setSilent(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
                context,
                SERVICE_NOTIFICATION_CHANNEL_ID,
                SERVICES_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

fun createCalendarEventNotification(context: Context, event: Event, profile: Profile, state: Event.State): Notification {
    val contentText: String = if (state == Event.State.STARTED) {
        "${TextUtil.formatEventTimestamp(context, event, event.currentInstanceStartTime)} - activating ${profile.title}"
    } else {
        "${TextUtil.formatEventTimestamp(context, event, event.currentInstanceEndTime)} - restoring ${profile.title}"
    }
    val builder = NotificationCompat.Builder(context, CALENDAR_EVENTS_NOTIFICATION_CHANNEL_ID)
        .setContentTitle(event.title)
        .setContentText(contentText)
        .setSmallIcon(R.drawable.ic_baseline_calendar_today_24)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
            context,
            CALENDAR_EVENTS_NOTIFICATION_CHANNEL_ID,
            CALENDAR_EVENTS_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT).also {
            builder.setChannelId(it.id)
        }
    }
    return builder.build()
}

fun createAlarmAlertNotification(context: Context, title: String, localTime: LocalTime): Notification {
    val builder = NotificationCompat.Builder(context, SCHEDULER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Scheduler")
            .setContentText("$title at ${TextUtil.localizedTimeToString(localTime)}")
            .setSmallIcon(R.drawable.baseline_alarm_deep_purple_300_24dp)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
                context,
                SCHEDULER_NOTIFICATION_CHANNEL_ID,
                SCHEDULER_NOTIFICATION_CHANNEL_NAME,
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