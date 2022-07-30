package ru.rrtry.silentdroid.util

import android.Manifest.permission.*
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.net.Uri
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

fun Context.openPackageInfoActivity() {
    startActivity(
        Intent().apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
            addFlags(FLAG_ACTIVITY_CLEAR_TASK)
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", applicationContext.packageName, null)
        }
    )
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