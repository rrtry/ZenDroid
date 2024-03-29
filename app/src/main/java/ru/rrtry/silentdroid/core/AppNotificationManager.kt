package ru.rrtry.silentdroid.core

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import androidx.core.app.NotificationCompat
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_ALARM
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_GEOFENCE_ENTER
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_GEOFENCE_EXIT
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_MANUAL
import ru.rrtry.silentdroid.entities.Alarm
import ru.rrtry.silentdroid.entities.Location
import ru.rrtry.silentdroid.entities.PreviousAndNextTrigger
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.util.TextUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.rrtry.silentdroid.ui.activities.ViewPagerActivity
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotificationManager @Inject constructor(@ApplicationContext private val context: Context) {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationChannelName: String = context.resources.getString(R.string.notification_channel_name)

    private fun notify(id: Int, notification: Notification) {
        notification.visibility = NotificationCompat.VISIBILITY_PUBLIC
        notificationManager.notify(id, notification)
    }

    fun cancelProfileNotification() {
        notificationManager.cancel(ID_PROFILE)
    }

    private fun getMainActivityIntent(): PendingIntent {
        return PendingIntent.getActivity(context,
            MAIN_ACTIVITY_REQUEST_CODE,
            Intent(context, ViewPagerActivity::class.java),
            FLAG_IMMUTABLE)
    }

    fun updateNotification(profile: Profile?, previousAndNextTrigger: PreviousAndNextTrigger?) {
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
                        previousAndNextTrigger = previousAndNextTrigger)
                }
                TRIGGER_TYPE_ALARM -> {
                    postProfileNotification(
                        profileTitle = profile.title,
                        alarmTitle = preferencesManager.getTrigger<Alarm>().title,
                        iconRes = profile.iconRes,
                        previousAndNextTrigger = previousAndNextTrigger
                    )
                }
            }
        } else if (previousAndNextTrigger != null) {
            postNextProfileNotification(
                previousAndNextTrigger.relation.startProfile,
                previousAndNextTrigger
            )
        } else {
            cancelProfileNotification()
        }
    }

    fun postGeofenceExitNotification(profileTitle: String, locationTitle: String) {
        val builder = NotificationCompat.Builder(context, PROFILE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(profileTitle)
            .setContentText(context.resources.getString(R.string.geofence_exit, locationTitle))
            .setSmallIcon(R.drawable.baseline_location_on_black_24dp)
            .setContentIntent(getMainActivityIntent())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                PROFILE_NOTIFICATION_CHANNEL_ID,
                notificationChannelName,
                NotificationManager.IMPORTANCE_LOW).also {
                builder.setChannelId(it.id)
            }
        }
        notify(ID_PROFILE, builder.build())
    }

    fun postGeofenceEnterNotification(profileTitle: String, locationTitle: String) {
        val builder = NotificationCompat.Builder(context, PROFILE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(profileTitle)
            .setContentText(context.resources.getString(R.string.geofence_enter, locationTitle))
            .setSmallIcon(R.drawable.baseline_location_on_black_24dp)
            .setContentIntent(getMainActivityIntent())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                PROFILE_NOTIFICATION_CHANNEL_ID,
                notificationChannelName,
                NotificationManager.IMPORTANCE_LOW).also {
                builder.setChannelId(it.id)
            }
        }
        notify(ID_PROFILE, builder.build())
    }

    private fun postNextProfileNotification(
        profile: Profile,
        previousAndNextTrigger: PreviousAndNextTrigger
    ) {

        val contentText = context.resources.getString(
            R.string.next_scheduled_profile,
            profile.title,
            TextUtil.formatNextAlarmDateTime(context, previousAndNextTrigger.until!!))

        val builder = NotificationCompat.Builder(context, PROFILE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.resources.getString(R.string.next_profile))
            .setContentText(contentText)
            .setSmallIcon(profile.iconRes)
            .setContentIntent(getMainActivityIntent())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                PROFILE_NOTIFICATION_CHANNEL_ID,
                notificationChannelName,
                NotificationManager.IMPORTANCE_LOW).also {
                builder.setChannelId(it.id)
            }
        }
        notify(ID_PROFILE, builder.build())
    }

    private fun postProfileNotification(
        profileTitle: String,
        alarmTitle: String? = null,
        iconRes: Int,
        previousAndNextTrigger: PreviousAndNextTrigger?
    ) {
        val until: LocalTime? = previousAndNextTrigger?.until?.toLocalTime()
        val contentText: String = if (until == null) {
            context.resources.getString(R.string.active_profile, profileTitle)
        } else {
            context.resources.getString(
                R.string.active_until,
                profileTitle,
                TextUtil.formatNextAlarmDateTime(context, previousAndNextTrigger.until))
        }
        val builder = NotificationCompat.Builder(context, PROFILE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(alarmTitle ?: profileTitle)
            .setContentText(contentText)
            .setSmallIcon(iconRes)
            .setContentIntent(getMainActivityIntent())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                PROFILE_NOTIFICATION_CHANNEL_ID,
                notificationChannelName,
                NotificationManager.IMPORTANCE_LOW).also {
                builder.setChannelId(it.id)
            }
        }
        notify(ID_PROFILE, builder.build())
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String, importance: Int): NotificationChannel {
        return NotificationChannel(
            channelId, channelName, importance
        ).also { channel ->
            channel.setBypassDnd(true)
            channel.setShowBadge(false)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {

        private const val PROFILE_NOTIFICATION_CHANNEL_ID: String = "CHANNEL_ID_PROFILE"
        private const val ID_PROFILE: Int = 6077
        private const val MAIN_ACTIVITY_REQUEST_CODE: Int = 2
    }
}