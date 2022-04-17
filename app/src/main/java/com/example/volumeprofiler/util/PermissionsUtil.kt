package com.example.volumeprofiler.util

import android.Manifest.permission.*
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.volumeprofiler.entities.Profile
import java.lang.IllegalArgumentException

fun Fragment.checkPermission(permission: String): Boolean {
    return checkSelfPermission(requireContext(), permission)
}

fun Context.checkPermission(permission: String): Boolean {
    return checkSelfPermission(this, permission)
}

private fun checkSelfPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

fun isNotificationPolicyAccessGranted(context: Context): Boolean {
    return (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        .isNotificationPolicyAccessGranted
}

fun canWriteSettings(context: Context): Boolean {
    return Settings.System.canWrite(context)
}

fun getCategoryName(permission: String): String {
    return when (permission) {
        ACCESS_FINE_LOCATION -> "Location"
        ACCESS_BACKGROUND_LOCATION -> "Background location"
        WRITE_SETTINGS -> "System settings"
        ACCESS_NOTIFICATION_POLICY -> "Do not disturb access"
        READ_PHONE_STATE -> "Phone"
        else -> throw IllegalArgumentException(
            "Unknown permission"
        )
    }
}

fun sendPermissionsNotification(context: Context, profileManager: ProfileManager, profile: Profile): Unit {
    sendSystemPreferencesAccessNotification(context, profileManager)
    getDeniedPermissionsForProfile(context, profile).apply {
        if (isNotEmpty()) {
            postNotification(
                context, createMissingPermissionNotification(
                    context, this.toList()
                ), ID_PERMISSIONS)
        }
    }
}

fun getDeniedPermissionsForProfile(context: Context, profile: Profile): Array<String> {
    val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    val permissions: MutableList<String> = mutableListOf()
    if (!canWriteSettings(context)) {
        permissions.add(WRITE_SETTINGS)
    }
    if (!notificationManager.isNotificationPolicyAccessGranted) {
        permissions.add(ACCESS_NOTIFICATION_POLICY)
    }
    if (profile.streamsUnlinked && !context.checkPermission(READ_PHONE_STATE)) {
        permissions.add(READ_PHONE_STATE)
    }
    return permissions.toTypedArray()
}

fun shouldShowPermissionSuggestion(context: Context, profile: Profile): Boolean {
    return getDeniedPermissionsForProfile(context, profile).isNotEmpty()
}