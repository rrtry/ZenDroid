package ru.rrtry.silentdroid.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.util.Log
import android.view.*
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.common.ConnectionResult.SUCCESS
import com.google.android.gms.common.GoogleApiAvailability
import ru.rrtry.silentdroid.ui.fragments.LocationsListFragment
import ru.rrtry.silentdroid.ui.fragments.ProfilesListFragment
import ru.rrtry.silentdroid.ui.fragments.SchedulerFragment
import ru.rrtry.silentdroid.interfaces.ViewPagerActivityCallback
import ru.rrtry.silentdroid.viewmodels.MainActivityViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_SLIDE
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.core.*
import ru.rrtry.silentdroid.core.GeofenceManager.Companion.REQUEST_ENABLE_LOCATION_SERVICES
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.PREFS_STREAM_TYPE_ALIAS
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.PREFS_STREAM_TYPE_INDEPENDENT
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.PREFS_STREAM_TYPE_NOT_SET
import ru.rrtry.silentdroid.databinding.ActivityMainBinding
import ru.rrtry.silentdroid.ui.fragments.NotificationPolicyAccessDialog
import javax.inject.Inject

@AndroidEntryPoint
class ViewPagerActivity: AppActivity(), ViewPagerActivityCallback, GeofenceManager.LocationRequestListener {

    private val viewModel: MainActivityViewModel by viewModels()

    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var appAudioManager: AppAudioManager
    @Inject lateinit var notificationPolicyManager: NotificationPolicyManager

    interface MenuItemSelectedListener {

        fun onMenuOptionSelected(itemId: Int)
    }

    private lateinit var binding: ActivityMainBinding
    private var snackbar: Snackbar? = null
    override var actionMode: ActionMode? = null
    private val isGoogleServicesPresent: Boolean
    get() = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == SUCCESS

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {

        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            if (viewModel.currentFragment.value != position) {
                viewModel.onFragmentSwiped()
            }
            onPrepareOptionsMenu(binding.toolbar.menu)
            viewModel.animateFloatingActionButton(position)
            viewModel.currentFragment.value = position
        }
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = if (isGoogleServicesPresent) 3 else 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                PROFILE_FRAGMENT -> ProfilesListFragment()
                SCHEDULER_FRAGMENT -> SchedulerFragment()
                LOCATIONS_FRAGMENT -> LocationsListFragment()
                else -> throw IllegalArgumentException("No fragment at position $position")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        with(window) {
            exitTransition = Fade(Fade.OUT)
            enterTransition = Fade(Fade.IN)
        }

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.pager.adapter = ScreenSlidePagerAdapter(this)
        binding.pager.registerOnPageChangeCallback(onPageChangeCallback)

        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            when (position) {
                PROFILE_FRAGMENT -> {
                    tab.text = resources.getString(R.string.tab_profiles)
                    tab.icon = ResourcesCompat.getDrawable(resources, R.drawable.baseline_notifications_active_black_24dp, theme)
                }
                SCHEDULER_FRAGMENT -> {
                    tab.text = resources.getString(R.string.tab_scheduler)
                    tab.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_access_time_24, theme)
                }
                LOCATIONS_FRAGMENT -> {
                    tab.text = resources.getString(R.string.tab_locations)
                    tab.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_location_on_24, theme)
                }
            }
        }.attach()
        binding.fab.setOnClickListener {
            viewModel.onFloatingActionButtonClicked(binding.pager.currentItem)
        }
    }

    override fun onStart() {
        super.onStart()
        if (notificationPolicyManager.isPolicyAccessGranted) {
            if (preferencesManager.isFirstSetup()) {
                preferencesManager.setFirstSetup()
            }
            if (appAudioManager.getNotificationStreamType() == PREFS_STREAM_TYPE_NOT_SET) {
                val independent: Boolean = appAudioManager.isNotificationStreamIndependent()
                val streamType: Int = if (independent) PREFS_STREAM_TYPE_INDEPENDENT else PREFS_STREAM_TYPE_ALIAS
                appAudioManager.setNotificationStreamType(streamType)
            }
        } else if (preferencesManager.isFirstSetup()) {
            NotificationPolicyAccessDialog().show(
                supportFragmentManager, null
            )
        }
    }

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_LOCATION_SERVICES) {
            if (resultCode == Activity.RESULT_OK) {
                onLocationRequestSuccess()
            } else {
                onLocationRequestFailure()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.viewEvents.resetReplayCache()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.pager.unregisterOnPageChangeCallback(onPageChangeCallback)
    }

    override fun removeSnackbar() {
        snackbar?.dismiss()
    }

    override fun showSnackBar(text: String, actionText: String, length: Int, action: (() -> Unit)?) {
        snackbar = Snackbar.make(
            binding.coordinatorLayout,
            text,
            length
        ).apply {
            animationMode = ANIMATION_MODE_SLIDE
            action?.let { setAction(actionText) { action() } }
            show()
        }
    }

    override fun getFloatingActionButton(): FloatingActionButton {
        return binding.fab
    }

    override fun onLocationRequestSuccess() = Unit

    override fun onLocationRequestFailure() {
        showSnackBar(
            resources.getString(R.string.location_services_explanation),
            resources.getString(R.string.enable),
            Snackbar.LENGTH_INDEFINITE)
        {
            geofenceManager.checkLocationServicesAvailability(this)
        }
    }

    override fun onBack() {
        if (binding.pager.currentItem == 0) {
            finish()
        } else {
            binding.pager.currentItem =- 1
        }
    }

    companion object {

        const val PROFILE_FRAGMENT: Int = 0
        const val SCHEDULER_FRAGMENT: Int = 1
        const val LOCATIONS_FRAGMENT: Int = 2
    }
}