package ru.rrtry.silentdroid.util

import android.Manifest.permission.*
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
            "Unknown permission $permission"
        )
    }
}