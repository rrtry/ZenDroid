package com.example.volumeprofiler.selection

import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.selection.SelectionTracker
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.ActionModeProvider
import dagger.hilt.android.scopes.FragmentScoped
import java.lang.ref.WeakReference

@FragmentScoped
class ItemSelectionObserver<T>(
    providerRef: WeakReference<ActionModeProvider<T>>
): SelectionTracker.SelectionObserver<T>() {

    private var actionMode: ActionMode? = null
    private val provider: ActionModeProvider<T>? = providerRef.get()
    private val tracker: SelectionTracker<T>? = provider?.getTracker()

    private val actionModeCallback = object : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (mode == null || menu == null) {
                return false
            }
            actionMode = mode
            mode.menuInflater?.inflate(R.menu.action_item_selection, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (mode == null || menu == null) {
                return false
            }
            mode.title = "Selected: ${tracker?.selection?.size()}"
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (mode == null || item == null) {
                return false
            }
            if (item.itemId == R.id.action_delete) {
                provider?.onActionItemRemove()
                actionMode?.finish()
                return true
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            tracker?.clearSelection()
        }
    }

    override fun onSelectionRestored() {
        super.onSelectionRestored()
        tracker?.let { tracker ->
            if (tracker.hasSelection()) {
                startActionMode()
            }
        }
    }

    override fun onSelectionChanged() {
        super.onSelectionChanged()
        tracker?.let { tracker ->
            if (tracker.hasSelection()) {
                if (actionMode != null) invalidate() else startActionMode()
                return
            }
        }
        finish()
    }

    private fun finish() {
        actionMode?.finish()
    }

    private fun invalidate() {
        actionMode?.invalidate()
    }

    private fun startActionMode() {
        provider?.getFragmentActivity()?.startActionMode(actionModeCallback)
    }
}