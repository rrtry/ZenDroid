package ru.rrtry.silentdroid.interfaces

import android.view.ActionMode
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

interface ViewPagerActivityCallback {

    var actionMode: ActionMode?

    fun showSnackBar(
        text: String,
        actionText: String = "Grant",
        length: Int,
        action: (() -> Unit)?
    )

    fun removeSnackbar()

    fun getFloatingActionButton(): FloatingActionButton
}