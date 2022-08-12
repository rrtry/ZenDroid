package ru.rrtry.silentdroid.util

import android.Manifest.permission.*
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ru.rrtry.silentdroid.R
import java.lang.IllegalArgumentException

fun Fragment.checkPermission(permission: String): Boolean {
    return checkSelfPermission(requireContext(), permission)
}

fun Context.checkPermission(permission: String): Boolean {
    return checkSelfPermission(this, permission)
}

fun Context.openNotificationPolicySettingsActivity() {
    startActivity(Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
        addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
    })
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

fun getCategoryName(context: Context, permission: String): String {
    return when (permission) {
        ACCESS_FINE_LOCATION -> context.resources.getString(R.string.permission_location)
        ACCESS_BACKGROUND_LOCATION -> context.resources.getString(R.string.permission_background_location)
        WRITE_SETTINGS -> context.resources.getString(R.string.permission_system_settings_access)
        ACCESS_NOTIFICATION_POLICY -> context.resources.getString(R.string.permission_dnd_access)
        READ_PHONE_STATE -> context.resources.getString(R.string.permission_phone)
        else -> throw IllegalArgumentException("Unknown permission $permission")
    }
}