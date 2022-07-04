package com.example.volumeprofiler.core
import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.service.notification.ZenPolicy
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.volumeprofiler.R
import com.example.volumeprofiler.core.PreferencesManager.Companion.TRIGGER_TYPE_ALARM
import com.example.volumeprofiler.core.PreferencesManager.Companion.TRIGGER_TYPE_GEOFENCE_ENTER
import com.example.volumeprofiler.core.PreferencesManager.Companion.TRIGGER_TYPE_GEOFENCE_EXIT
import com.example.volumeprofiler.core.PreferencesManager.Companion.TRIGGER_TYPE_MANUAL
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.OngoingAlarm
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.TextUtil
import com.example.volumeprofiler.util.ViewUtil.Companion.convertDipToPx
import com.example.volumeprofiler.util.getCategoryName
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationDelegate @Inject constructor(@ApplicationContext private val context: Context) {

    @Inject lateinit var preferencesManager: PreferencesManager

    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun getApplicationSettingsIntent(): PendingIntent {
        val intent: Intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
            flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(context, REQUEST_GRANT_WRITE_SETTINGS_PERMISSION, intent,  PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getBitmap(drawableRes: Int): Bitmap? {

        val widthPx: Int = context.convertDipToPx(context.resources.getDimension(android.R.dimen.notification_large_icon_width))
        val heightPx: Int = context.convertDipToPx(context.resources.getDimension(android.R.dimen.notification_large_icon_height))

        val drawable: Drawable? = ContextCompat.getDrawable(context, drawableRes)
        return drawable?.toBitmap(widthPx, heightPx, null)
    }

    private fun getSystemSettingsPendingIntent(): PendingIntent {
        val intent: Intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
            flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(context, REQUEST_GRANT_WRITE_SETTINGS_PERMISSION, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getInterruptionPolicyPendingIntent(): PendingIntent {
        val intent: Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(context, REQUEST_GRANT_NOTIFICATION_POLICY_PERMISSION, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getAppDetailsPendingIntent(): PendingIntent {
        val intent: Intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
            flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(context, REQUEST_LAUNCH_APPLICATION_DETAILS_SETTINGS, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    fun cancelProfileNotification() {
        notificationManager.cancel(ID_PROFILE)
    }

    fun updateNotification(profile: Profile?, ongoingAlarm: OngoingAlarm?) {
        if (profile != null) {
            when (preferencesManager.getTriggerType()) {
                TRIGGER_TYPE_GEOFENCE_ENTER -> {
                    postGeofenceEnterNotification(profile.title, preferencesManager.getTrigger<Location>().title)
                }
                TRIGGER_TYPE_GEOFENCE_EXIT -> {
                    postGeofenceExitNotification(profile.title, preferencesManager.getTrigger<Location>().title)
                }
                TRIGGER_TYPE_MANUAL -> {
                    postProfileNotification(
                        profileTitle = profile.title,
                        iconRes = profile.iconRes,
                        ongoingAlarm = ongoingAlarm)
                }
                TRIGGER_TYPE_ALARM -> {
                    postProfileNotification(
                        profileTitle = profile.title,
                        alarmTitle = preferencesManager.getTrigger<Alarm>().title,
                        iconRes = profile.iconRes,
                        ongoingAlarm = ongoingAlarm
                    )
                }
            }
        } else {
            ongoingAlarm?.let { ongoingAlarm ->
                ongoingAlarm.relation.startProfile.also { profile ->
                    postNextProfileNotification(profile, ongoingAlarm)
                }
            }
        }
    }

    fun createMissingPermissionNotification(permissions: List<String>): Notification {
        val contentTitle: String = "Insufficient permissions"
        val pNames: String = permissions.map { getCategoryName(it) }.toString().removeSurrounding("[", "]")
        val contentText: String = pNames
        val builder = NotificationCompat.Builder(context, PERMISSIONS_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_baseline_perm_device_information_24)
            .setContentIntent(getApplicationSettingsIntent())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
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

    fun postSystemSettingsNotification() {
        val builder = NotificationCompat.Builder(context, PERMISSIONS_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Write system settings")
            .setContentText("Click to open settings")
            .setContentIntent(getSystemSettingsPendingIntent())
            .setSmallIcon(R.drawable.ic_baseline_perm_device_information_24)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                PERMISSIONS_NOTIFICATION_CHANNEL_ID,
                PERMISSIONS_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH).also {
                builder.setChannelId(it.id)
            }
        }
        val notification: Notification = builder.build()
        notification.flags = Notification.FLAG_AUTO_CANCEL
        notificationManager.notify(ID_SYSTEM_SETTINGS, notification)
    }

    fun postInterruptionPolicyNotification() {
        val builder = NotificationCompat.Builder(context, PERMISSIONS_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Do not disturb access")
            .setContentText("Click to open settings")
            .setContentIntent(getInterruptionPolicyPendingIntent())
            .setSmallIcon(R.drawable.ic_baseline_perm_device_information_24)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                PERMISSIONS_NOTIFICATION_CHANNEL_ID,
                PERMISSIONS_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH).also {
                builder.setChannelId(it.id)
            }
        }
        val notification: Notification = builder.build()
        notification.flags = Notification.FLAG_AUTO_CANCEL
        notificationManager.notify(ID_INTERRUPTION_POLICY, notification)
    }

    fun postGeofenceExitNotification(profileTitle: String, locationTitle: String) {
        val builder = NotificationCompat.Builder(context, PROFILE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(profileTitle)
            .setContentText("You've left $locationTitle'")
            .setSmallIcon(R.drawable.baseline_location_on_black_24dp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                PROFILE_NOTIFICATION_CHANNEL_ID,
                PROFILE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW).also {
                builder.setChannelId(it.id)
            }
        }
        notificationManager.notify(ID_PROFILE, builder.build())
    }

    fun postGeofenceEnterNotification(profileTitle: String, locationTitle: String) {
        val builder = NotificationCompat.Builder(context, PROFILE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(profileTitle)
            .setContentText("You've entered $locationTitle'")
            .setSmallIcon(R.drawable.baseline_location_on_black_24dp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                PROFILE_NOTIFICATION_CHANNEL_ID,
                PROFILE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW).also {
                builder.setChannelId(it.id)
            }
        }
        notificationManager.notify(ID_PROFILE, builder.build())
    }

    private fun postNextProfileNotification(
        profile: Profile,
        ongoingAlarm: OngoingAlarm
    ) {
        val contentText = "next profile is '${profile.title}' set for ${TextUtil.formatNextAlarmDateTime(context, ongoingAlarm.until!!)}"
        val builder = NotificationCompat.Builder(context, PROFILE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(profile.title)
            .setContentText(contentText)
            .setSmallIcon(profile.iconRes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                PROFILE_NOTIFICATION_CHANNEL_ID,
                PROFILE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW).also {
                builder.setChannelId(it.id)
            }
        }
        notificationManager.notify(ID_PROFILE, builder.build())
    }

    private fun postProfileNotification(
        profileTitle: String,
        alarmTitle: String? = null,
        iconRes: Int,
        ongoingAlarm: OngoingAlarm?
    ) {
        val until: LocalTime? = ongoingAlarm?.until?.toLocalTime()
        val contentText: String = if (until == null) {
            "'$profileTitle' will stay until you turn it off"
        } else {
            "'$profileTitle' is on until ${TextUtil.formatNextAlarmDateTime(context, ongoingAlarm.until)}"
        }
        val builder = NotificationCompat.Builder(context, PROFILE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(alarmTitle ?: profileTitle)
            .setContentText(contentText)
            .setSmallIcon(iconRes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                PROFILE_NOTIFICATION_CHANNEL_ID,
                PROFILE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW).also {
                builder.setChannelId(it.id)
            }
        }
        notificationManager.notify(ID_PROFILE, builder.build())
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String, importance: Int): NotificationChannel {
        return NotificationChannel(
            channelId, channelName, importance
        ).also { channel ->
            channel.setBypassDnd(true)
            channel.setShowBadge(false)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {

        private const val PROFILE_NOTIFICATION_CHANNEL_ID: String = "CHANNEL_ID_PROFILE"
        private const val PERMISSIONS_NOTIFICATION_CHANNEL_ID: String = "CHANNEL_ID_PERMISSIONS"
        private const val PERMISSIONS_NOTIFICATION_CHANNEL_NAME: String = "Permission reminders"
        private const val PROFILE_NOTIFICATION_CHANNEL_NAME: String = "Current profile"

        const val ID_SYSTEM_SETTINGS: Int = 1072
        const val ID_INTERRUPTION_POLICY: Int = 2073
        const val ID_PERMISSIONS: Int = 3074
        const val ID_PROFILE: Int = 6077

        private const val REQUEST_GRANT_WRITE_SETTINGS_PERMISSION: Int = 0
        private const val REQUEST_GRANT_NOTIFICATION_POLICY_PERMISSION: Int = 1
        private const val REQUEST_LAUNCH_APPLICATION_DETAILS_SETTINGS: Int = 2
    }
}