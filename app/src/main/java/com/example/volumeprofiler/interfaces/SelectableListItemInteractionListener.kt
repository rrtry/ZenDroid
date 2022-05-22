package com.example.volumeprofiler.interfaces

import androidx.viewbinding.ViewBinding
import com.example.volumeprofiler.databinding.ProfileItemViewBinding

interface SelectableListItemInteractionListener<T, I>: ListItemInteractionListener<T, ProfileItemViewBinding> {

    fun setSelection(id: I?)

    fun isEnabled(entity: T): Boolean
}