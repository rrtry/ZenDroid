package com.example.volumeprofiler.interfaces

import android.view.ActionMode
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

interface MainActivityCallback {

    var actionMode: ActionMode?

    fun showSnackBar(
        text: String,
        length: Int,
        action: (() -> Unit)?
    )

    fun removeSnackbar()

    fun requestPermissions(permissions: Array<String>)

    fun getFloatingActionButton(): FloatingActionButton
}