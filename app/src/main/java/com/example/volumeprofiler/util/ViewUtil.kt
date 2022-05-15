package com.example.volumeprofiler.util

import android.Manifest
import android.Manifest.permission.WRITE_SETTINGS
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.volumeprofiler.ui.fragments.PermissionExplanationDialog
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.volumeprofiler.R
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_SLIDE
import com.google.android.material.snackbar.Snackbar
import kotlin.math.roundToInt

class ViewUtil {

    companion object {

        internal const val DISMISS_TIME_WINDOW: Int = 2000

        fun Fragment.getDrawable(drawableRes: Int): Drawable {
            return ResourcesCompat.getDrawable(
                requireContext().resources, drawableRes, requireContext().theme
            ) ?: throw IllegalArgumentException("Could not get drawable from resource: $drawableRes")
        }

        fun Context.getDrawable(drawableRes: Int): Drawable {
            return ResourcesCompat.getDrawable(resources, drawableRes, theme)
                ?: throw IllegalArgumentException("Could not get drawable from resource: $drawableRes")
        }

        fun showSnackbar(
            view: View,
            message: String,
            length: Int,
            title: String? = null,
            action: (() -> Unit)? = null) {
            Snackbar.make(view, message, length).apply {
                animationMode = ANIMATION_MODE_SLIDE
                if (action != null) {
                    setAction(title) {
                        action()
                    }
                }
            }.show()
        }

        fun Context.convertDipToPx(dip: Float): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                resources.displayMetrics
            ).roundToInt()
        }

        fun showInterruptionPolicyAccessExplanation(fragmentManager: FragmentManager) {
            showPermissionRationaleDialog(
                fragmentManager,
                Manifest.permission.ACCESS_NOTIFICATION_POLICY,
                R.string.interruption_policy_access_explanation,
                R.drawable.ic_baseline_do_not_disturb_on_24
            )
        }

        fun showLocationPermissionExplanation(fragmentManager: FragmentManager) {
            showPermissionRationaleDialog(
                fragmentManager,
                Manifest.permission.ACCESS_FINE_LOCATION,
                R.string.location_permission_explanation,
                R.drawable.baseline_location_on_black_24dp,
                true
            )
        }

        @TargetApi(Build.VERSION_CODES.Q)
        fun showBackgroundLocationPermissionExplanation(fragmentManager: FragmentManager) {
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
            requestMultiplePermissionsOnResult: Boolean = false) {
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
                WRITE_SETTINGS,
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