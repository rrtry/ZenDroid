package com.example.volumeprofiler.util

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.volumeprofiler.fragments.PermissionExplanationDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.content.res.AppCompatResources
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.MenuItem
import androidx.annotation.IntegerRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.volumeprofiler.R
import com.google.android.material.snackbar.Snackbar
import kotlin.math.roundToInt

class ViewUtil {

    companion object {

        internal const val DISMISS_TIME_WINDOW: Int = 2000

        fun Fragment.getDrawable(drawableRes: Int): Drawable {
            return ResourcesCompat.getDrawable(
                requireContext().resources, drawableRes, requireContext().theme
            ) ?: throw IllegalArgumentException("Could not get drawableL $drawableRes")
        }

        fun Context.getDrawable(drawableRes: Int): Drawable {
            return ResourcesCompat.getDrawable(resources, drawableRes, theme)
                ?: throw IllegalArgumentException("Could not get drawable from resource: $drawableRes")
        }

        fun Context.showSnackbar(
            view: View,
            message: String,
            length: Int,
            title: String? = null,
            action: (() -> Unit)? = null) {
            Snackbar.make(view, message, length).apply {
                action?.let {
                    setAction(title) {
                        it()
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

        fun isInputMethodVisible(context: Context): Boolean {
            val inputMethodManager = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            return inputMethodManager.isActive
        }

        fun uiModeNightEnabled(context: Context): Boolean {
            return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }

        fun resolveResourceAttribute(context: Context, attribute: Int): Drawable? {
            val value = TypedValue()
            context.theme.resolveAttribute(attribute, value, true)
            return AppCompatResources.getDrawable(context, value.resourceId)
        }

        fun getHalfExpandedRatio(rootViewGroup: ViewGroup, targetView: View): Float {
            val rect: Rect = Rect()
            rootViewGroup.offsetDescendantRectToMyCoords(targetView, rect)
            return (rect.top).toFloat() / rootViewGroup.height
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