package ru.rrtry.silentdroid.selection

import androidx.recyclerview.selection.ItemKeyProvider
import ru.rrtry.silentdroid.interfaces.ListAdapterItemProvider

class KeyProvider<T> (private val adapter: ListAdapterItemProvider<T>) : ItemKeyProvider<T>(SCOPE_CACHED)
{
    override fun getKey(position: Int): T? {
        return adapter.getItemKey(position)
    }

    override fun getPosition(key: T): Int {
        return adapter.getPosition(key)
    }
}