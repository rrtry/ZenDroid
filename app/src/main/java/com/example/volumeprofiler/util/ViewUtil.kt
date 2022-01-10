package com.example.volumeprofiler.util

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
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
import androidx.core.content.res.ResourcesCompat
import com.example.volumeprofiler.R

class ViewUtil {

    companion object {

        internal const val DISMISS_TIME_WINDOW: Int = 2000

        fun setActionMenuAddIcon(context: Context, item: MenuItem): Unit {
            val drawable = ResourcesCompat.getDrawable(context.resources, android.R.drawable.ic_menu_add, null)
            item.icon = drawable
        }

        fun setActionMenuSaveIcon(context: Context, item: MenuItem): Unit {
            val drawable = ResourcesCompat.getDrawable(context.resources, android.R.drawable.ic_menu_save, null)
            item.icon = drawable
        }

        fun hideSoftwareInput(context: Context): Unit {
            try {
                val inputManager: InputMethodManager = context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.hideSoftInputFromWindow((context as AppCompatActivity).currentFocus?.windowToken, 0)
            } catch (e: ClassCastException) {
                Log.e("ViewUtil", "Passed context is not an activity", e)
            }
        }

        fun uiModeNightEnabled(context: Context): Boolean {
            return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }

        fun resolveResourceAttribute(context: Context, attribute: Int): Drawable? {
            val value = TypedValue()
            context.theme.resolveAttribute(attribute, value, true)
            return AppCompatResources.getDrawable(context, value.resourceId)
        }

        fun disableAutoHideBehavior(fab: FloatingActionButton): Unit {
            val layoutParams = fab.layoutParams as CoordinatorLayout.LayoutParams
            val behavior = layoutParams.behavior as FloatingActionButton.Behavior?
            behavior?.isAutoHideEnabled = false
        }

        fun detachAnchorView(fab: FloatingActionButton): Unit {
            val layoutParams = fab.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.anchorId = View.NO_ID
            fab.layoutParams = layoutParams
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