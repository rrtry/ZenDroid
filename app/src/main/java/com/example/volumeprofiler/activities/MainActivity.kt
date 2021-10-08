package com.example.volumeprofiler.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.volumeprofiler.R
import com.example.volumeprofiler.adapters.viewPager.MainActivityPagerAdapter
import com.example.volumeprofiler.adapters.viewPager.pageTransformer.ZoomOutPageTransformer
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    @Inject lateinit var pagerAdapter: MainActivityPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        supportActionBar?.title = "VolumeProfiler"
        viewPager = findViewById(R.id.pager)
        viewPager.adapter = pagerAdapter
        setupTabLayout()
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