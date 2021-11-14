package com.example.volumeprofiler.util

import android.Manifest
import android.annotation.TargetApi
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.volumeprofiler.R
import com.example.volumeprofiler.fragments.dialogs.PermissionExplanationDialog

class ViewUtil {

    companion object {

        fun calculateHalfExpandedRatio(rootViewGroup: ViewGroup, targetView: View): Float {
            val rect: Rect = Rect()
            rootViewGroup.offsetDescendantRectToMyCoords(targetView, rect)
            return (rect.top * 100f / rootViewGroup.height) / 100f
        }

        fun showInterruptionPolicyAccessExplanation(fragmentManager: FragmentManager): Unit {
            showPermissionRationaleDialog(
                fragmentManager,
                Manifest.permission.ACCESS_NOTIFICATION_POLICY,
                R.string.interruption_policy_access_explanation,
                R.drawable.ic_baseline_do_not_disturb_on_24
            )
        }

        fun showLocationPermissionExplanation(fragmentManager: FragmentManager): Unit {
            showPermissionRationaleDialog(
                fragmentManager,
                Manifest.permission.ACCESS_FINE_LOCATION,
                R.string.location_permission_explanation,
                R.drawable.baseline_location_on_black_24dp,
                true
            )
        }

        fun showStoragePermissionExplanation(fragmentManager: FragmentManager): Unit {
            showPermissionRationaleDialog(
                fragmentManager,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                R.string.storage_permission_explanation,
                R.drawable.ic_baseline_sd_storage_24
            )
        }

        @TargetApi(Build.VERSION_CODES.Q)
        fun showBackgroundLocationPermissionExplanation(fragmentManager: FragmentManager): Unit {
            showPermissionRationaleDialog(
                fragmentManager,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                R.string.background_location_access_explanation,
                R.drawable.baseline_location_on_black_24dp,
                true
            )
        }

        fun showPhoneStatePermissionExplanation(
            fragmentManager: FragmentManager,
            requestMultiplePermissionsOnResult: Boolean = false): Unit {
            showPermissionRationaleDialog(
                fragmentManager,
                Manifest.permission.READ_PHONE_STATE,
                R.string.phone_permission_explanation,
                R.drawable.baseline_call_deep_purple_300_24dp,
                requestMultiplePermissionsOnResult
            )
        }

        fun showSystemSettingsPermissionExplanation(fragmentManager: FragmentManager): Unit {
            showPermissionRationaleDialog(
                fragmentManager,
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                R.string.system_settings_permission_explanation,
                R.drawable.ic_baseline_settings_24
            )
        }

        private fun showPermissionRationaleDialog(fragmentManager: FragmentManager,
                                                  permission: String,
                                                  message: Int,
                                                  drawable: Int, requestMultiplePermissionsOnResult: Boolean = false): Unit {
            val dialog: DialogFragment = PermissionExplanationDialog.newInstance(
                permission,
                message,
                drawable,
                requestMultiplePermissionsOnResult
            )
            dialog.show(fragmentManager, null)
        }
    }
}