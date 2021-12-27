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
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.media.AudioManager.*
import android.util.Log
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.fragments.dialogs.PermissionExplanationDialog.Companion.EXTRA_PERMISSION
import com.example.volumeprofiler.fragments.dialogs.PermissionExplanationDialog.Companion.EXTRA_REQUEST_MULTIPLE_PERMISSIONS
import com.example.volumeprofiler.fragments.dialogs.PermissionExplanationDialog.Companion.EXTRA_RESULT_OK
import com.example.volumeprofiler.fragments.dialogs.PermissionExplanationDialog.Companion.PERMISSION_REQUEST_KEY
import com.example.volumeprofiler.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), PermissionRequestCallback {

    private lateinit var viewPager: ViewPager2

    @Inject
    lateinit var pagerAdapter: MainActivityPagerAdapter

    @Inject
    lateinit var profileUtil: ProfileUtil

    @Inject
    lateinit var geofenceUtil: GeofenceUtil

    private lateinit var profilePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<out String>>

    private var profile: Profile? = null
    private var locationRelation: LocationRelation? = null

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        viewPager = findViewById(R.id.pager)
        viewPager.adapter = pagerAdapter
        setupTabLayout()

        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, FLAG_REMOVE_SOUND_AND_VIBRATE)
        Log.i("MainActivity", "max stream volume STREAM_ALARM: ${audioManager.getStreamMaxVolume(STREAM_ALARM)}")
        Log.i("MainActivity", "min stream volume STREAM_MUSIC: ${audioManager.getStreamMinVolume(STREAM_ALARM)}")
        Log.i("MainActivity", "max stream volume STREAM_VOICE_CALL: ${audioManager.getStreamMaxVolume(STREAM_VOICE_CALL)}")
        Log.i("MainActivity", "min stream volume STREAM_VOICE_CALL: ${audioManager.getStreamMinVolume(STREAM_VOICE_CALL)}")

        supportFragmentManager.setFragmentResultListener(PERMISSION_REQUEST_KEY, this,
            { requestKey, result ->
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

                    if (profileUtil.shouldRequestPhonePermission(locationRelation!!)) {
                        val snackbar: Snackbar = Snackbar.make(viewPager, "Phone and location permissions required", Snackbar.LENGTH_LONG)
                        setAction(snackbar, true)
                        snackbar.show()
                    } else {
                        val snackbar: Snackbar = Snackbar.make(viewPager, "Location permission required", Snackbar.LENGTH_LONG)
                        setAction(snackbar, false)
                        snackbar.show()
                    }
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
                else -> {
                    Snackbar.make(viewPager, "Required permissions were granted", Snackbar.LENGTH_LONG)
                        .show()
                }
            }
        }
        profilePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            when {
                it -> {
                    if (profileUtil.grantedSystemPreferencesAccess()) {
                        Snackbar.make(viewPager, "Phone permission was granted", Snackbar.LENGTH_LONG)
                            .show()
                    } else {
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
                Log.i("MainActivity", i)
                snackbar.setAction("Open settings") {
                    startActivity(getApplicationSettingsIntent(this))
                }
                break
            }
        }
    }

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

    override fun onBackPressed() {
        if (viewPager.currentItem == 0) {
            super.onBackPressed()
        }
        else {
            viewPager.currentItem = viewPager.currentItem - 1
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        profilePermissionLauncher.unregister()
        locationPermissionLauncher.unregister()
    }

    companion object {

        private val drawables: List<Int> = listOf(android.R.drawable.ic_menu_recent_history,
                android.R.drawable.ic_menu_sort_by_size,
                android.R.drawable.ic_lock_silent_mode, android.R.drawable.ic_menu_mylocation)
    }

    override fun requestProfilePermissions(profile: Profile) {
        this.profile = profile
        profilePermissionLauncher.launch(READ_PHONE_STATE)
    }

    override fun requestLocationPermissions(locationRelation: LocationRelation) {
        this.locationRelation = locationRelation
        var permissions: Array<String> = arrayOf(
            ACCESS_FINE_LOCATION
        )
        if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            permissions += ACCESS_BACKGROUND_LOCATION
        }
        if (profileUtil.requiresPhoneStatePermission(locationRelation)) {
            permissions += READ_PHONE_STATE
        }
        locationPermissionLauncher.launch(permissions)
    }
}