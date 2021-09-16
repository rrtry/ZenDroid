package com.example.volumeprofiler.adapters.viewPager

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.volumeprofiler.fragments.AlarmsListFragment
import com.example.volumeprofiler.fragments.LocationsListFragment
import com.example.volumeprofiler.fragments.ProfilesListFragment
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class MainActivityPagerAdapter @Inject constructor(
    @ActivityContext fa: Context
): FragmentStateAdapter(fa as AppCompatActivity) {

    override fun getItemCount(): Int {
        return NUM_PAGES
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ProfilesListFragment()
            1 -> AlarmsListFragment()
            else -> LocationsListFragment()
        }
    }

    companion object {
        private const val NUM_PAGES: Int = 3
    }
}