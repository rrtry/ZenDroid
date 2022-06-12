package com.example.volumeprofiler.interfaces

import android.view.ActionMode
import com.google.android.material.floatingactionbutton.FloatingActionButton

interface FabContainerCallbacks {

    var actionMode: ActionMode?

    fun showSnackBar(
        text: String,
        length: Int,
        action: (() -> Unit)?
    )

    fun requestPermissions(permissions: Array<String>)

    fun getFloatingActionButton(): FloatingActionButton
}