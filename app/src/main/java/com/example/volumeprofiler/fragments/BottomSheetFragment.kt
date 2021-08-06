package com.example.volumeprofiler.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import com.example.volumeprofiler.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationBarView

class BottomSheetFragment: Fragment(), NavigationBarView.OnItemSelectedListener {

    private var callbacks: Callbacks? = null

    interface Callbacks {

        fun expandBottomSheet(): Unit

        fun collapseBottomSheet(): Unit

        fun setPeekHeight(height: Int): Unit

        fun getBottomSheetState(): Int

        fun setGesturesEnabled(enabled: Boolean): Unit
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = requireActivity() as Callbacks
    }

    override fun onDetach() {
        callbacks = null
        super.onDetach()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.fragment_bottom_sheet, container, false)
        view.setOnTouchListener(object : View.OnTouchListener {

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                return true
            }
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomNavBar: BottomNavigationView = view.findViewById(R.id.bottom_navigation)
        bottomNavBar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                bottomNavBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
                callbacks?.setPeekHeight(bottomNavBar.height)
            }
        })
        bottomNavBar.setOnItemSelectedListener(this)
        addFragment()
    }

    private fun replaceFragment(fragment: Fragment, tag: String): Unit {
        if (tag == TAG_PROFILES_FRAGMENT && childFragmentManager.backStackEntryCount < 1) {
            childFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
        } else if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStackImmediate()
        }
    }

    private fun addFragment(): Unit {
        val fragment: Fragment? = childFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment == null) {
            Log.i("BottomSheetFragment", "adding fragment")
            childFragmentManager
                    .beginTransaction()
                    .add(R.id.fragmentContainer, MapsCoordinatesFragment(), TAG_COORDINATES_FRAGMENT)
                    .commit()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.profile_tab -> replaceFragment(MapsProfileSelectionFragment(), TAG_PROFILES_FRAGMENT)
            R.id.location_tab -> replaceFragment(MapsCoordinatesFragment(), TAG_COORDINATES_FRAGMENT)
        }
        val currentState: Int = callbacks?.getBottomSheetState()!!
        if (currentState == BottomSheetBehavior.STATE_COLLAPSED
                || currentState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
            callbacks?.expandBottomSheet()
        }
        return true
    }

    companion object {
        private const val TAG_COORDINATES_FRAGMENT: String = "coordinates"
        private const val TAG_PROFILES_FRAGMENT: String = "profiles"
    }
}