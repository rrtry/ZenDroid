package com.example.volumeprofiler.selection

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.selection.SelectionTracker
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.ActionModeProvider
import dagger.hilt.android.scopes.FragmentScoped
import java.lang.ref.WeakReference

@FragmentScoped
class BaseSelectionObserver<T>(
    providerRef: WeakReference<ActionModeProvider<T>>
): SelectionTracker.SelectionObserver<T>() {

    private var actionMode: ActionMode? = null
    private val provider: ActionModeProvider<T>? = providerRef.get()

    override fun onSelectionChanged() {
        super.onSelectionChanged()

        provider?.let {

            val tracker = it.getTracker()
            val activity = it.getFragmentActivity()

            if (tracker.hasSelection() && actionMode == null) {

                activity.startActionMode(object : ActionMode.Callback {

                    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        actionMode = mode
                        mode?.menuInflater?.inflate(R.menu.action_item_selection, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        return true
                    }

                    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                        return when (item?.itemId) {
                            R.id.action_delete -> {
                                provider.onActionItemRemove()
                                actionMode?.finish()
                                true
                            } else -> false
                        }
                    }

                    override fun onDestroyActionMode(mode: ActionMode?) {
                        actionMode = null
                        tracker.clearSelection()
                    }
                })
                actionMode?.title = "Selected: ${it.getTracker().selection.size()}"
            } else if (!it.getTracker().hasSelection()) {
                actionMode?.finish()
                actionMode = null
            } else {
                actionMode?.title = "Selected: ${it.getTracker().selection.size()}"
            }
        }
    }
}