package com.example.volumeprofiler.activities

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.volumeprofiler.R
import com.example.volumeprofiler.adapters.viewPager.MainActivityPagerAdapter
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.interfaces.PermissionRequestCallback
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.Manifest.permission.*
import android.os.Build
import android.view.Window
import android.transition.Fade
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.fragments.PermissionExplanationDialog.Companion.EXTRA_PERMISSION
import com.example.volumeprofiler.fragments.PermissionExplanationDialog.Companion.EXTRA_REQUEST_MULTIPLE_PERMISSIONS
import com.example.volumeprofiler.fragments.PermissionExplanationDialog.Companion.EXTRA_RESULT_OK
import com.example.volumeprofiler.fragments.PermissionExplanationDialog.Companion.PERMISSION_REQUEST_KEY
import com.example.volumeprofiler.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), PermissionRequestCallback {

    @Inject
    lateinit var pagerAdapter: MainActivityPagerAdapter

    @Inject
    lateinit var profileUtil: ProfileUtil

    @Inject
    lateinit var geofenceUtil: GeofenceUtil

    private lateinit var viewPager: ViewPager2

    private lateinit var profilePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<out String>>

    private var profile: Profile? = null
    private var locationRelation: LocationRelation? = null

    private fun setupTabLayout(): Unit {
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = resources.getString(R.string.tab_profiles)
                    tab.icon = ResourcesCompat.getDrawable(resources, drawables[2], theme)
                }
                1 -> {
                    tab.text = resources.getString(R.string.tab_scheduler)
                    tab.icon = ResourcesCompat.getDrawable(resources, drawables[0], theme)
                }
                2 -> {
                    tab.text = resources.getString(R.string.tab_locations)
                    tab.icon = ResourcesCompat.getDrawable(resources, drawables[3], theme)
                }
            }
        }.attach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        with(window) {
            exitTransition = Fade(Fade.OUT)
            enterTransition = Fade(Fade.IN)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        viewPager = findViewById(R.id.pager)
        viewPager.adapter = pagerAdapter
        setupTabLayout()

        supportFragmentManager.setFragmentResultListener(PERMISSION_REQUEST_KEY, this,
            { _, result ->
                val permission: String = result.getString(EXTRA_PERMISSION, "")
                val requestMultiplePermissions: Boolean = result.getBoolean(EXTRA_REQUEST_MULTIPLE_PERMISSIONS, false)
                if (result.getBoolean(EXTRA_RESULT_OK)) {
                    if (shouldShowRequestPermissionRationale(permission)) {
                        if (requestMultiplePermissions) {
                            requestLocationPermissions(locationRelation!!)
                        } else {
                            profilePermissionLauncher.launch(permission)
                        }
                    }
                    else {
                        startActivity(getApplicationSettingsIntent(this))
                    }
                }
            })
        locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            when {
                !geofenceUtil.locationAccessGranted() && !profileUtil.grantedRequiredPermissions(locationRelation!!)-> {

                    val snackbar: Snackbar = Snackbar.make(viewPager, "", Snackbar.LENGTH_LONG)

                    if (profileUtil.shouldRequestPhonePermission(locationRelation!!)) {
                        snackbar.setText("Phone and location permissions required")
                        setAction(snackbar, true)
                    } else {
                        snackbar.setText("Location permission required")
                        setAction(snackbar, false)
                    }
                    snackbar.show()
                }
                !geofenceUtil.locationAccessGranted() -> {
                    if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
                        ViewUtil.showBackgroundLocationPermissionExplanation(supportFragmentManager)
                    } else {
                        ViewUtil.showLocationPermissionExplanation(supportFragmentManager)
                    }
                }
                profileUtil.shouldRequestPhonePermission(locationRelation!!) -> {
                    ViewUtil.showPhoneStatePermissionExplanation(supportFragmentManager)
                }
                !profileUtil.grantedSystemPreferencesAccess() -> {
                    sendSystemPreferencesAccessNotification(this, profileUtil)
                }
            }
        }
        profilePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            when {
                it -> {
                    if (!profileUtil.grantedSystemPreferencesAccess()) {
                        sendSystemPreferencesAccessNotification(this, profileUtil)
                    }
                }
                shouldShowRequestPermissionRationale(READ_PHONE_STATE) -> {
                    Snackbar.make(viewPager, "Phone permission is required", Snackbar.LENGTH_LONG)
                        .setAction("Show explanation") {
                            ViewUtil.showPhoneStatePermissionExplanation(supportFragmentManager)
                        }
                        .show()
                }
                else -> {
                    ViewUtil.showPhoneStatePermissionExplanation(supportFragmentManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        profilePermissionLauncher.unregister()
        locationPermissionLauncher.unregister()
    }

    private fun setAction(snackbar: Snackbar, requiresPhonePermission: Boolean): Unit {
        val permissions: MutableList<String> = mutableListOf()
        permissions += if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            ACCESS_BACKGROUND_LOCATION
        } else {
            ACCESS_FINE_LOCATION
        }
        if (requiresPhonePermission) {
            permissions += READ_PHONE_STATE
        }
        for (i in permissions) {
            if (!shouldShowRequestPermissionRationale(i)) {
                snackbar.setAction("Open settings") {
                    startActivity(getApplicationSettingsIntent(this))
                }
                break
            }
        }
    }

    override fun onBackPressed() {
        if (viewPager.currentItem == 0) {
            super.onBackPressed()
        }
        else {
            viewPager.currentItem = viewPager.currentItem - 1
        }
    }

    override fun requestProfilePermissions(profile: Profile) {
        this.profile = profile
        profilePermissionLauncher.launch(READ_PHONE_STATE)
    }

    override fun requestLocationPermissions(locationRelation: LocationRelation) {
        this.locationRelation = locationRelation
        var permissions: Array<String> = arrayOf(ACCESS_FINE_LOCATION)
        if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            permissions += ACCESS_BACKGROUND_LOCATION
        }
        if (profileUtil.requiresPhoneStatePermission(locationRelation)) {
            permissions += READ_PHONE_STATE
        }
        locationPermissionLauncher.launch(permissions)
    }

    companion object {

        private val drawables: List<Int> = listOf(android.R.drawable.ic_menu_recent_history,
                android.R.drawable.ic_menu_sort_by_size,
                android.R.drawable.ic_lock_silent_mode, android.R.drawable.ic_menu_mylocation)
    }
}