package com.example.volumeprofiler.activities

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.transition.Fade
import android.view.LayoutInflater
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.ActivityMainBinding
import com.example.volumeprofiler.fragments.LocationsListFragment
import com.example.volumeprofiler.fragments.ProfilesListFragment
import com.example.volumeprofiler.fragments.SchedulerFragment
import com.example.volumeprofiler.interfaces.FabContainer
import com.example.volumeprofiler.interfaces.FabContainerCallbacks
import com.example.volumeprofiler.interfaces.FragmentSwipedListener
import com.example.volumeprofiler.util.canWriteSettings
import com.example.volumeprofiler.util.isNotificationPolicyAccessGranted
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity(), FabContainerCallbacks {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pagerAdapter: ScreenSlidePagerAdapter
    private lateinit var permissionRequestLauncher: ActivityResultLauncher<Array<String>>

    private var afterTransition: Boolean = false

    private var currentPosition: Int = 0
    set(value) {
        (pagerAdapter.fragments[field] as FragmentSwipedListener).also {
            it.onSwipe()
        }
        field = value
    }

    private val selectedFragment: FabContainer
    get() {
        return pagerAdapter.fragments[binding.pager.currentItem] as FabContainer
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {

        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            currentPosition = position
            if (!afterTransition) {
                selectedFragment.onAnimateFab(binding.fab)
            }
            afterTransition = false
        }
    }

    private class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        val fragments: List<Fragment> = listOf(
            ProfilesListFragment(),
            SchedulerFragment(),
            LocationsListFragment()
        )

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }
    }

    override fun showSnackBar(text: String, length: Int, action: (() -> Unit)?) {
        Snackbar.make(
            binding.coordinatorLayout,
            text,
            length
        ).apply {
            action?.let {
                setAction("Grant") {
                    action()
                }
            }
        }.show()
    }

    override fun requestPermissions(permissions: Array<String>) {
        permissionRequestLauncher.launch(
            permissions
        )
    }

    override fun onBackPressed() {
        if (binding.pager.currentItem == 0) {
            super.onBackPressed()
        } else {
            binding.pager.currentItem =- 1
        }
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        afterTransition = true
        super.onActivityReenter(resultCode, data)
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
                0 -> {
                    tab.text = resources.getString(R.string.tab_profiles)
                    tab.icon = ResourcesCompat.getDrawable(resources, R.drawable.baseline_notifications_active_black_24dp, theme)
                }
                1 -> {
                    tab.text = resources.getString(R.string.tab_scheduler)
                    tab.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_access_time_24, theme)
                }
                2 -> {
                    tab.text = resources.getString(R.string.tab_locations)
                    tab.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_location_on_24, theme)
                }
            }
        }.attach()
        binding.fab.setOnClickListener {
            selectedFragment.onFabClick(binding.fab)
        }
        permissionRequestLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (!it.values.contains(false)) {
                // Not all permissions were granted
            } else if (canWriteSettings(this) || isNotificationPolicyAccessGranted(this)) {
                // Grant permissions from notification area
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.pager.unregisterOnPageChangeCallback(onPageChangeCallback)
    }
}