package com.example.volumeprofiler.adapters.viewPager

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.volumeprofiler.fragments.SchedulerFragment
import com.example.volumeprofiler.fragments.LocationsListFragment
import com.example.volumeprofiler.fragments.ProfilesListFragment

class MainActivityPagerAdapter constructor(
    fa: Context
): FragmentStateAdapter(fa as AppCompatActivity) {

    override fun getItemCount(): Int {
        return NUM_PAGES
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ProfilesListFragment()
            1 -> SchedulerFragment()
            else -> LocationsListFragment()
        }
    }

    companion object {
        private const val NUM_PAGES: Int = 3
    }
}