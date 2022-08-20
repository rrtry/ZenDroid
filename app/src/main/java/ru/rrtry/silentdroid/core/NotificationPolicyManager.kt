package ru.rrtry.silentdroid.core

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.rrtry.silentdroid.entities.Profile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPolicyManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val notificationManager: NotificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    val currentInterruptionFilter: Int get() = notificationManager.currentInterruptionFilter
    val isPolicyAccessGranted: Boolean get() = notificationManager.isNotificationPolicyAccessGranted

    fun setNotificationPolicy(profile: Profile) {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.notificationPolicy = createNotificationPolicy(profile)
            notificationManager.setInterruptionFilter(profile.interruptionFilter)
        }
    }

    fun setInterruptionFilter(interruptionFilter: Int) {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(interruptionFilter)
        }
    }

    private fun createNotificationPolicy(profile: Profile): NotificationManager.Policy {
        return when {
            Build.VERSION_CODES.N > Build.VERSION.SDK_INT -> {
                NotificationManager.Policy(
                    profile.priorityCategories,
                    profile.priorityCallSenders,
                    profile.priorityMessageSenders
                )
            }
            Build.VERSION_CODES.N <= Build.VERSION.SDK_INT && Build.VERSION_CODES.R > Build.VERSION.SDK_INT -> {
                NotificationManager.Policy(
                    profile.priorityCategories,
                    profile.priorityCallSenders,
                    profile.priorityMessageSenders,
                    profile.suppressedVisualEffects
                )
            }
            else -> {
                NotificationManager.Policy(
                    profile.priorityCategories,
                    profile.priorityCallSenders,
                    profile.priorityMessageSenders,
                    profile.suppressedVisualEffects,
                    profile.primaryConversationSenders
                )
            }
        }
    }
}