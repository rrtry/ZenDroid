package com.example.volumeprofiler.interfaces

import android.os.Bundle

interface ListItemActionListener<T> {

    fun onEdit(entity: T, options: Bundle? = null)

    fun onEnable(entity: T)

    fun onDisable(entity: T)

    fun onRemove(entity: T)

    fun isSelected(entity: T): Boolean
}