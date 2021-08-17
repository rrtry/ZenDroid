package com.example.volumeprofiler.adapters.recyclerview.multiSelection

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.selection.SelectionTracker
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.ActionModeProvider
import java.lang.ref.WeakReference

class BaseSelectionObserver<T>(
        private val provider: WeakReference<ActionModeProvider<T>>
        ): SelectionTracker.SelectionObserver<T>() {

    private var actionMode: ActionMode? = null

    private fun setSelectedTitle(selected: Int) {
        actionMode?.title = "Selected: $selected"
    }

    private fun getActivity(): FragmentActivity {
        return provider.get()!!.getFragmentActivity()
    }

    private fun getTracker(): SelectionTracker<T> {
        return provider.get()!!.getTracker()
    }

    private fun onActionItemRemove(): Unit {
        provider.get()!!.onActionItemRemove()
    }

    override fun onSelectionChanged() {
        super.onSelectionChanged()
        if (provider.get()!!.getTracker().hasSelection() && actionMode == null) {
            actionMode = getActivity().startActionMode(object : ActionMode.Callback {

                override fun onActionItemClicked(
                        mode: ActionMode?,
                        item: MenuItem?
                ): Boolean {
                    if (item?.itemId == R.id.deleteProfileButton) {
                        onActionItemRemove()
                        mode?.finish()
                        return true
                    }
                    return false
                }

                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    mode?.menuInflater?.inflate(R.menu.action_menu_selected, menu)
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = true

                override fun onDestroyActionMode(mode: ActionMode?) {
                    getTracker().clearSelection()
                }
            })
            setSelectedTitle(getTracker().selection.size())
        } else if (!getTracker().hasSelection()) {
            actionMode?.finish()
            actionMode = null
        } else {
            setSelectedTitle(getTracker().selection.size())
        }
    }
}