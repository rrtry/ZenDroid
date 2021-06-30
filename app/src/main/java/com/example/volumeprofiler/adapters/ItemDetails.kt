package com.example.volumeprofiler.adapters

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import com.example.volumeprofiler.fragments.ProfilesListFragment

class ItemDetails(val adapter: ProfilesListFragment.ProfileAdapter, val adapterPosition: Int): ItemDetailsLookup.ItemDetails<String>() {

    override fun getSelectionKey(): String? {
        return adapter.getProfile(adapterPosition).id.toString()
    }

    override fun getPosition(): Int {
        return adapterPosition
    }

    override fun inSelectionHotspot(e: MotionEvent): Boolean {
        return false
    }
}