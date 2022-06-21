package com.example.volumeprofiler.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.volumeprofiler.R
import com.example.volumeprofiler.ui.activities.MapsActivity
import kotlin.IllegalArgumentException

class BottomSheetFragment: Fragment(), MapsActivity.ItemSelectedListener {

    private var currentFragment: String = TAG_GEOFENCE_FRAGMENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            currentFragment = it.getString(EXTRA_CURRENT_FRAGMENT, TAG_GEOFENCE_FRAGMENT)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_CURRENT_FRAGMENT, currentFragment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (childFragmentManager.fragments.isEmpty()) {
            addFragments()
        }
    }

    private fun findFragmentByTag(tag: String): Fragment {
        return childFragmentManager.findFragmentByTag(tag)
            ?: throw IllegalArgumentException("Invalid fragment tag")
    }

    private fun addFragments() {
        val profileFragment: Fragment = GeofenceProfileFragment()
        childFragmentManager
            .beginTransaction()
            .add(R.id.fragmentContainer, GeofenceDetailsFragment(), TAG_GEOFENCE_FRAGMENT)
            .add(R.id.fragmentContainer, profileFragment, TAG_PROFILE_FRAGMENT).hide(profileFragment)
            .commit()
    }

    private fun replaceFragment(tag: String) {
        if (tag != currentFragment) {
            childFragmentManager
                .beginTransaction()
                .hide(findFragmentByTag(currentFragment))
                .show(findFragmentByTag(tag))
                .commit()
            currentFragment = tag
        }
    }

    override fun onItemSelected(itemId: Int) {
        val tag: String = if (itemId == R.id.geofence_tab) {
            TAG_GEOFENCE_FRAGMENT
        } else {
            TAG_PROFILE_FRAGMENT
        }
        replaceFragment(tag)
    }

    companion object {

        private const val TAG_GEOFENCE_FRAGMENT: String = "tag_geofence_fragment"
        private const val TAG_PROFILE_FRAGMENT: String = "tag_profile_fragment"
        private const val EXTRA_CURRENT_FRAGMENT: String = "key_current_fragment"
    }
}