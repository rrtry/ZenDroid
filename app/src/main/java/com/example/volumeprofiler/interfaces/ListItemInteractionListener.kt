package com.example.volumeprofiler.interfaces

import androidx.viewbinding.ViewBinding

interface ListItemInteractionListener<T, B: ViewBinding> {

    fun onEdit(entity: T, binding: B)

    fun onEnable(entity: T)

    fun onDisable(entity: T)

    fun onRemove(entity: T)

    fun isSelected(entity: T): Boolean
}