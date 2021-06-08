package com.example.volumeprofiler.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.volumeprofiler.fragments.ProfilesListFragment
import com.example.volumeprofiler.R
import com.example.volumeprofiler.fragments.ScheduledEventsListFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = "Title"
        viewPager = findViewById(R.id.pager)
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter
        setupTabLayout()
    }

    override fun onStop() {
        Log.i("MainActivity", "onStop()")
        super.onStop()
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

        private val drawables: Array<Int> = arrayOf(android.R.drawable.ic_menu_recent_history,
                android.R.drawable.ic_menu_sort_by_size,
            android.R.drawable.ic_lock_silent_mode)
        private const val NUM_PAGES = 2
    }
}