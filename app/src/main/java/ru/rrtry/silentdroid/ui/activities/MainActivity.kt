package ru.rrtry.silentdroid.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ru.rrtry.silentdroid.core.GeofenceManager
import ru.rrtry.silentdroid.ui.fragments.LocationsListFragment
import ru.rrtry.silentdroid.ui.fragments.ProfilesListFragment
import ru.rrtry.silentdroid.ui.fragments.SchedulerFragment
import ru.rrtry.silentdroid.interfaces.MainActivityCallback
import ru.rrtry.silentdroid.viewmodels.MainActivityViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_SLIDE
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.databinding.ActivityMainBinding
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainActivityCallback, GeofenceManager.LocationRequestListener {

    @Inject
    lateinit var geofenceManager: GeofenceManager
    interface MenuItemSelectedListener {

        fun onMenuOptionSelected(itemId: Int)
    }

    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var pagerAdapter: ScreenSlidePagerAdapter
    private var snackbar: Snackbar? = null

    override var actionMode: ActionMode? = null

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

    private class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = 3

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
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        with(window) {
            exitTransition = Fade(Fade.OUT)
            enterTransition = Fade(Fade.IN)
        }
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        pagerAdapter = ScreenSlidePagerAdapter(this)
        binding.pager.adapter = pagerAdapter
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

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GeofenceManager.REQUEST_ENABLE_LOCATION_SERVICES) {
            if (resultCode == Activity.RESULT_OK) {
                onLocationRequestSuccess()
            } else {
                onLocationRequestFailure()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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

    override fun onBackPressed() {
        if (binding.pager.currentItem == 0) {
            super.onBackPressed()
        } else {
            binding.pager.currentItem =- 1
        }
    }

    override fun onLocationRequestSuccess() = Unit

    override fun onLocationRequestFailure() {
        showSnackBar("For location triggers to work, turn on device location", "Enable", Snackbar.LENGTH_INDEFINITE) {
            geofenceManager.checkLocationServicesAvailability(this)
        }
    }

    companion object {

        const val PROFILE_FRAGMENT: Int = 0
        const val SCHEDULER_FRAGMENT: Int = 1
        const val LOCATIONS_FRAGMENT: Int = 2
    }
}