package com.example.volumeprofiler.interfaces

import android.os.Bundle
import android.view.View

interface ListItemActionListener<T> {

    fun onEditWithScroll(entity: T, options: Bundle?, view: View)

    fun onEdit(entity: T, options: Bundle? = null)

    fun onEnable(entity: T)

    fun onDisable(entity: T)

    fun onRemove(entity: T)

    fun isSelected(entity: T): Boolean
}