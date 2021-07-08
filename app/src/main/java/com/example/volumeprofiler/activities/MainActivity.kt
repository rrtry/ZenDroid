package com.example.volumeprofiler.activities

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.volumeprofiler.fragments.ProfilesListFragment
import com.example.volumeprofiler.R
import com.example.volumeprofiler.fragments.ScheduledEventsListFragment
import com.example.volumeprofiler.fragments.ZoomOutPageTransformer
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setInterruptionFilter()
        supportActionBar?.title = "Title"
        viewPager = findViewById(R.id.pager)
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.setPageTransformer(ZoomOutPageTransformer())
        setupTabLayout()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setInterruptionFilter(): Unit {
        val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            Log.i("MainActivity", notificationManager.notificationPolicy.toString())
        } else {
            val intent: Intent = Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(this)
            }
        }

    }

    private fun setupTabLayout(): Unit {
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            if (position == 0) {
                tab.text = resources.getString(R.string.tab_profiles)
                tab.icon = ResourcesCompat.getDrawable(resources, drawables[2], theme)
            }
            else {
                tab.text = resources.getString(R.string.tab_scheduler)
                tab.icon = ResourcesCompat.getDrawable(resources, drawables[0], theme)
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

    private inner class ScreenSlidePagerAdapter(fa: AppCompatActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment {

            if (position == 0) {
                return ProfilesListFragment()
            }
            return ScheduledEventsListFragment()
        }
    }

    companion object {

        private const val REQUEST_CODE_NOTIFICATION_POLICY: Int = 0
        private val drawables: Array<Int> = arrayOf(android.R.drawable.ic_menu_recent_history,
                android.R.drawable.ic_menu_sort_by_size,
            android.R.drawable.ic_lock_silent_mode)
        private const val NUM_PAGES = 2
    }
}