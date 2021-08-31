package com.example.volumeprofiler.activities

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.volumeprofiler.R
import com.example.volumeprofiler.adapters.viewPager.MainActivityPagerAdapter
import com.example.volumeprofiler.adapters.viewPager.pageTransformer.ZoomOutPageTransformer
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        supportActionBar?.title = "Title"
        viewPager = findViewById(R.id.pager)
        val pagerAdapter = MainActivityPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.setPageTransformer(ZoomOutPageTransformer())
        setupTabLayout()
        requestNotificationPolicyAccess()
    }

    private fun requestNotificationPolicyAccess(): Unit {
        val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val audioManager: AudioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            val intent: Intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } else {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            Log.i("MainActivity", "STREAM_NOTIFICATION: ${audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)}")
            Log.i("MainActivity", "STREAM_RING: ${audioManager.getStreamVolume(AudioManager.STREAM_RING)}")
            Log.i("MainActivity", "RINGER_MODE: ${audioManager.ringerMode}")
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

    companion object {

        private val drawables: List<Int> = listOf(android.R.drawable.ic_menu_recent_history,
                android.R.drawable.ic_menu_sort_by_size,
            android.R.drawable.ic_lock_silent_mode, android.R.drawable.ic_menu_mylocation)
    }
}