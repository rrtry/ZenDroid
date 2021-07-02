package com.example.volumeprofiler.adapters

import androidx.recyclerview.selection.ItemKeyProvider
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider

class KeyProvider <T> (private val adapter: ListAdapterItemProvider<T>) : ItemKeyProvider<T>(SCOPE_CACHED)
{
    override fun getKey(position: Int): T? {
        return adapter.getItemKey(position)
    }

    override fun getPosition(key: T): Int {
        return adapter.getPosition(key)
    }
}