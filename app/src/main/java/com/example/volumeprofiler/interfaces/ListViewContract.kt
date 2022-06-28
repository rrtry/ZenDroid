package com.example.volumeprofiler.interfaces

import android.os.Bundle
import android.view.View
import androidx.core.util.Pair

interface ListViewContract<T> {

    fun onEditWithTransition(entity: T, view: View, vararg sharedViews: Pair<View, String>)

    fun onEdit(entity: T, options: Bundle? = null)

    fun onEnable(entity: T)

    fun onDisable(entity: T)

    fun onRemove(entity: T)

    fun isSelected(entity: T): Boolean

    fun onSharedViewReady()
}