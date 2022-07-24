package ru.rrtry.silentdroid.interfaces

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

    fun getFloatingActionButton(): FloatingActionButton
}