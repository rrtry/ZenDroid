package com.example.volumeprofiler.ui.fragments

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.volumeprofiler.interfaces.ActionModeProvider
import com.example.volumeprofiler.interfaces.FragmentSwipedListener
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.interfaces.ListItemActionListener
import com.example.volumeprofiler.selection.ItemSelectionObserver
import com.example.volumeprofiler.selection.DetailsLookup
import com.example.volumeprofiler.selection.KeyProvider
import java.lang.ref.WeakReference
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.*

abstract class ListFragment<T: Parcelable, VB: ViewBinding, VH: RecyclerView.ViewHolder>:
    ViewBindingFragment<VB>(),
    FragmentSwipedListener,
    ActionModeProvider<T>,
    ListItemActionListener<T> {

    protected lateinit var selectionTracker: SelectionTracker<T>

    abstract val selectionId: String
    abstract val listItem: Class<T>

    abstract fun getRecyclerView(): RecyclerView
    abstract fun getAdapter(): ListAdapter<T, VH>

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectionTracker.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        selectionTracker.onRestoreInstanceState(savedInstanceState)
    }

    @Suppress("unchecked_cast")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getRecyclerView().let {

            it.adapter = getAdapter().apply { stateRestorationPolicy = PREVENT_WHEN_EMPTY }
            it.layoutManager = LinearLayoutManager(context)

            selectionTracker = SelectionTracker.Builder(
                selectionId,
                it,
                KeyProvider(it.adapter as ListAdapterItemProvider<T>),
                DetailsLookup<T>(it),
                StorageStrategy.createParcelableStorage(listItem)
            ).withSelectionPredicate(SelectionPredicates.createSelectAnything()).build()
            selectionTracker.addObserver(ItemSelectionObserver(WeakReference(this)))
        }
    }

    override fun onSwipe() {
        if (!requireActivity().isChangingConfigurations) {
            selectionTracker.clearSelection()
        }
    }

    override fun isSelected(entity: T): Boolean {
        return selectionTracker.isSelected(entity)
    }
}