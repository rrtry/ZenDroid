package com.example.volumeprofiler.adapters.viewPager

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.volumeprofiler.fragments.AlarmsListFragment
import com.example.volumeprofiler.fragments.LocationsListFragment
import com.example.volumeprofiler.fragments.ProfilesListFragment

class MainActivityPagerAdapter(fa: AppCompatActivity): FragmentStateAdapter(fa) {

    override fun getItemCount(): Int {
        return NUM_PAGES
    }

    override fun createFragment(position: Int): Fragment {
        if (position == 0) {
            return ProfilesListFragment()
        }
        else if (position == 1) {
            return AlarmsListFragment()
        }
        else {
            return LocationsListFragment()
        }
    }

    companion object {
        private const val NUM_PAGES: Int = 3
    }
}