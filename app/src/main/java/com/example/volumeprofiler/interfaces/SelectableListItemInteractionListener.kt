package com.example.volumeprofiler.interfaces

import androidx.viewbinding.ViewBinding

interface SelectableListItemInteractionListener<T, I>: ListItemInteractionListener<T, ViewBinding> {

    fun setSelection(id: I?)

    fun isEnabled(entity: T): Boolean
}