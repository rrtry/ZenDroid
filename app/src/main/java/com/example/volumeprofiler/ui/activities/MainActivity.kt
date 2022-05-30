package com.example.volumeprofiler.ui.activities

import android.os.Bundle
import android.transition.Fade
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.ActivityMainBinding
import com.example.volumeprofiler.ui.fragments.LocationsListFragment
import com.example.volumeprofiler.ui.fragments.ProfilesListFragment
import com.example.volumeprofiler.ui.fragments.SchedulerFragment
import com.example.volumeprofiler.interfaces.FabContainer
import com.example.volumeprofiler.interfaces.FabContainerCallbacks
import com.example.volumeprofiler.util.canWriteSettings
import com.example.volumeprofiler.util.isNotificationPolicyAccessGranted
import com.example.volumeprofiler.viewmodels.MainActivityViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_SLIDE
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), FabContainerCallbacks {

    interface OptionsItemSelectedListener {

        fun onSelected(itemId: Int)
    }

    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var pagerAdapter: ScreenSlidePagerAdapter
    private lateinit var permissionRequestLauncher: ActivityResultLauncher<Array<String>>

    private var currentPosition: Int = 2

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {

        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)

            viewModel.onFragmentSwiped(currentPosition)
            onPrepareOptionsMenu(binding.toolbar.menu)
            viewModel.updateFloatingActionButton(position)

            currentPosition = position
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

    override fun showSnackBar(text: String, length: Int, action: (() -> Unit)?) {
        Snackbar.make(
            binding.coordinatorLayout,
            text,
            length
        ).apply {
            animationMode = ANIMATION_MODE_SLIDE
            if (action != null) {
                setAction("Grant") {
                    action()
                }
            }
        }.show()
    }

    override fun requestPermissions(permissions: Array<String>) {
        permissionRequestLauncher.launch(permissions)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_PAGER_POSITION, currentPosition)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        viewModel.onMenuOptionSelected(item.itemId)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return if (currentPosition == SCHEDULER_FRAGMENT) {
            super.onCreateOptionsMenu(menu)
            menuInflater.inflate(R.menu.action_item_selection, menu)
            true
        } else {
            false
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        return currentPosition == SCHEDULER_FRAGMENT
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
        setSupportActionBar(binding.toolbar)

        savedInstanceState?.let {
            currentPosition = it.getInt(EXTRA_PAGER_POSITION, -1)
        }

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
        permissionRequestLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (!it.values.contains(false)) {

            } else if (!canWriteSettings(this) || !isNotificationPolicyAccessGranted(this)) {

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.pager.unregisterOnPageChangeCallback(onPageChangeCallback)
    }

    companion object {

        const val PROFILE_FRAGMENT: Int = 0
        const val SCHEDULER_FRAGMENT: Int = 1
        const val LOCATIONS_FRAGMENT: Int = 2
        private const val EXTRA_PAGER_POSITION: String = "position"
    }
}