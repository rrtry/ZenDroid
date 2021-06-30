package com.example.volumeprofiler.adapters

import androidx.recyclerview.selection.ItemKeyProvider
import com.example.volumeprofiler.fragments.ProfilesListFragment
import java.util.UUID

class KeyProvider (private val adapter: ProfilesListFragment.ProfileAdapter):
    ItemKeyProvider<String>(SCOPE_CACHED) {

    override fun getKey(position: Int): String? {
        return adapter.currentList[position].id.toString()
    }

    override fun getPosition(key: String): Int {
        return adapter.currentList.indexOfFirst { it.id.toString() == key }
    }
}