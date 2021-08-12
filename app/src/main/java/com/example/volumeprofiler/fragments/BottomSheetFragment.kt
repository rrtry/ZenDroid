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
    private lateinit var bottomNavBar: BottomNavigationView

    interface Callbacks {

        fun setState(state: Int): Unit

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
        val view: View = inflater.inflate(R.layout.bottom_sheet_fragment, container, false)
        view.setOnTouchListener(object : View.OnTouchListener {

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                return true
            }
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomNavBar = view.findViewById(R.id.bottom_navigation)
        bottomNavBar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                bottomNavBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
                callbacks?.setPeekHeight(bottomNavBar.height)
            }
        })
        bottomNavBar.setOnItemSelectedListener(this)
        replaceFragment(MapsCoordinatesFragment(), TAG_COORDINATES_FRAGMENT)
    }

    private fun replaceFragment(fragment: Fragment, tag: String?): Unit {
        val currentFragment: Fragment? = childFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment?.tag != tag) {
            childFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment, tag)
                .commit()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == bottomNavBar.selectedItemId) {
            when (callbacks!!.getBottomSheetState()) {
                BottomSheetBehavior.STATE_COLLAPSED -> callbacks?.setState(BottomSheetBehavior.STATE_HALF_EXPANDED)
                BottomSheetBehavior.STATE_HALF_EXPANDED -> callbacks?.setState(BottomSheetBehavior.STATE_EXPANDED)
                BottomSheetBehavior.STATE_EXPANDED -> callbacks?.setState(BottomSheetBehavior.STATE_COLLAPSED)
            }
        } else if (callbacks!!.getBottomSheetState() == BottomSheetBehavior.STATE_COLLAPSED) {
            callbacks?.setState(BottomSheetBehavior.STATE_HALF_EXPANDED)
        }
        when (item.itemId) {
            R.id.profile_tab -> {
                replaceFragment(MapsProfileSelectionFragment(), TAG_PROFILES_FRAGMENT)

            }
            R.id.location_tab -> {
                replaceFragment(MapsCoordinatesFragment(), TAG_COORDINATES_FRAGMENT)

            }
        }
        return true
    }

    companion object {
        private const val TAG_COORDINATES_FRAGMENT: String = "coordinates"
        private const val TAG_PROFILES_FRAGMENT: String = "profiles"
    }
}