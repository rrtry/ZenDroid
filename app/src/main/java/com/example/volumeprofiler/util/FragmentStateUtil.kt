package com.example.volumeprofiler.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class FragmentStateUtil(private val fragmentManager: FragmentManager) {

    private val savedStatesMap = mutableMapOf<String, Fragment.SavedState?>()

    fun restoreState(fragment: Fragment, key: String) {
        savedStatesMap[key]?.let { savedState ->
            if (!fragment.isAdded) {
                fragment.setInitialSavedState(savedState)
            }
        }
    }

    fun saveState(fragment: Fragment, key: String) {
        if (fragment.isAdded) {
            savedStatesMap[key] = fragmentManager.saveFragmentInstanceState(fragment)
        }
    }
}