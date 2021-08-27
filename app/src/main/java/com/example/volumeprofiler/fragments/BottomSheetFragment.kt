package com.example.volumeprofiler.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.volumeprofiler.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import java.util.*

class BottomSheetFragment: Fragment(), NavigationBarView.OnItemSelectedListener{

    private var callbacks: Callbacks? = null
    private lateinit var bottomNavView: BottomNavigationView
    private var activeFragmentTag: String = TAG_COORDINATES_FRAGMENT

    interface Callbacks {

        fun setState(state: Int): Unit

        fun collapseBottomSheet(): Unit

        fun setPeekHeight(height: Int): Unit

        fun getBottomSheetState(): Int

        fun setGesturesEnabled(enabled: Boolean): Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            this.activeFragmentTag = savedInstanceState.getString(KEY_CURRENT_FRAGMENT, TAG_COORDINATES_FRAGMENT)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENT_FRAGMENT, this.activeFragmentTag)
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
        view.setOnTouchListener { v, event -> true }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomNavView = view.findViewById(R.id.bottom_navigation)
        bottomNavView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                bottomNavView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                callbacks?.setPeekHeight(bottomNavView.height)
            }
        })
        bottomNavView.setOnItemSelectedListener(this)
        if (childFragmentManager.fragments.isEmpty()) {
            addFragments()
        }
    }

    private fun addFragments(): Unit {
        val coordinatesFragment: Fragment = MapsCoordinatesFragment()
        val profileSelectionFragment: Fragment = MapsProfileSelectionFragment.buildArgs(arguments?.getSerializable(MapsProfileSelectionFragment.ID_PAIR) as? Pair<UUID, UUID>)
        childFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, coordinatesFragment, TAG_COORDINATES_FRAGMENT)
                .add(R.id.fragmentContainer, profileSelectionFragment, TAG_PROFILES_FRAGMENT).hide(profileSelectionFragment)
                .commit()
    }

    private fun replaceFragment(tag: String): Unit {
        childFragmentManager
                .beginTransaction()
                .hide(childFragmentManager.findFragmentByTag(activeFragmentTag)!!)
                .show(childFragmentManager.findFragmentByTag(tag)!!)
                .commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.profile_tab -> {
                val tag: String = TAG_PROFILES_FRAGMENT
                replaceFragment(tag)
                activeFragmentTag = tag
                true
            }
            R.id.location_tab -> {
                val tag: String = TAG_COORDINATES_FRAGMENT
                replaceFragment(tag)
                activeFragmentTag = tag
                true
            } else -> false
        }
    }

    companion object {

        private const val TAG_COORDINATES_FRAGMENT: String = "coordinates"
        private const val TAG_PROFILES_FRAGMENT: String = "profiles"
        private const val KEY_CURRENT_FRAGMENT: String = "key_current_fragment"

        fun buildArgs(ids: Pair<UUID, UUID>?): Fragment {
            val bundle: Bundle = Bundle().apply {
                if (ids != null) {
                    this.putSerializable(MapsProfileSelectionFragment.ID_PAIR, ids)
                }
            }
            return BottomSheetFragment().apply {
                this.arguments = bundle
            }
        }
    }
}