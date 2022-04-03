package com.example.volumeprofiler.interfaces

interface FabContainerCallbacks {

    fun showSnackBar(
        text: String,
        length: Int,
        action: (() -> Unit)?
    )

    fun requestPermissions(permissions: Array<String>): Unit
}