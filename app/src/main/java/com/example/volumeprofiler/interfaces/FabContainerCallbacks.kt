package com.example.volumeprofiler.interfaces

import com.google.android.material.floatingactionbutton.FloatingActionButton

interface FabContainerCallbacks {

    fun showSnackBar(
        text: String,
        length: Int,
        action: (() -> Unit)?
    )

    fun requestPermissions(permissions: Array<String>)

    fun getFloatingActionButton(): FloatingActionButton
}