package com.example.volumeprofiler.ui.fragments

import android.content.Context
import androidx.core.util.Pair
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.*
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.SharedElementCallback
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.volumeprofiler.selection.DetailsLookup
import com.example.volumeprofiler.selection.KeyProvider
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.*
import com.example.volumeprofiler.util.ViewUtil.Companion.isViewPartiallyVisible

abstract class ListFragment<T: Parcelable, VB: ViewBinding, VH: RecyclerView.ViewHolder>:
    ViewBindingFragment<VB>(),
    FragmentSwipedListener,
    ActionModeProvider,
    ListItemActionListener<T> {

    protected var callback: MainActivityCallback? = null
    private var actionMode: ActionMode?
        get() = callback?.actionMode
        set(value) {
            callback?.actionMode = value
        }

    override var shouldStartDelayedTransition: Boolean
        get() = callback!!.isActivityReturning
        set(value) {
            callback!!.isActivityReturning = value
        }

    protected lateinit var selectionTracker: SelectionTracker<T>
    protected var childPosition: Int = 0

    abstract val selectionId: String
    abstract val listItem: Class<T>

    abstract fun getRecyclerView(): RecyclerView
    abstract fun getAdapter(): RecyclerView.Adapter<VH>
    abstract fun mapSharedElements(names: MutableList<String>?,
                                   sharedElements: MutableMap<String, View>?)

    private val actionModeCallback = object : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (mode == null || menu == null) return false
            actionMode = mode
            mode.menuInflater?.inflate(R.menu.action_item_selection, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (mode == null || menu == null) return false
            mode.title = "Selected: ${selectionTracker.selection.size()}"
            return true
        }

        @Suppress("unchecked_cast")
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (mode == null || item == null) return false
            if (item.itemId == R.id.action_delete) {
                onActionItemRemove()
                actionMode?.finish()
                return true
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            selectionTracker.clearSelection()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireActivity() as MainActivityCallback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childPosition = savedInstanceState?.getInt(EXTRA_CHILD_POSITION, 0) ?: 0
        requireActivity().postponeEnterTransition()
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectionTracker.onSaveInstanceState(outState)
        outState.putInt(EXTRA_CHILD_POSITION, childPosition)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        selectionTracker.onRestoreInstanceState(savedInstanceState)
    }

    @Suppress("unchecked_cast")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getRecyclerView().let { recyclerView ->

            recyclerView.adapter = getAdapter().apply {
                stateRestorationPolicy = PREVENT_WHEN_EMPTY
                setHasStableIds(true)
            }
            recyclerView.layoutManager = LinearLayoutManager(context)

            selectionTracker = SelectionTracker.Builder(
                selectionId,
                recyclerView,
                KeyProvider(recyclerView.adapter as ListAdapterItemProvider<T>),
                DetailsLookup<T>(recyclerView),
                StorageStrategy.createParcelableStorage(listItem)
            ).withSelectionPredicate(SelectionPredicates.createSelectAnything()).build()
            selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<T>() {

                override fun onSelectionRestored() {
                    super.onSelectionRestored()
                    if (selectionTracker.hasSelection()) {
                        startActionMode()
                    }
                }

                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    if (selectionTracker.hasSelection()) {
                        if (actionMode != null) {
                            actionMode?.invalidate()
                        } else {
                            startActionMode()
                        }
                        return
                    }
                    actionMode?.finish()
                }
            })
            requireActivity().setExitSharedElementCallback(object : SharedElementCallback() {

                override fun onMapSharedElements(
                    names: MutableList<String>?,
                    sharedElements: MutableMap<String, View>?
                ) {
                    mapSharedElements(names, sharedElements)
                    super.onMapSharedElements(names, sharedElements)
                }
            })
        }
    }

    override fun onEditWithTransition(
        entity: T,
        view: View,
        vararg sharedViews: Pair<View, String>
    ) {
        val recyclerView: RecyclerView = getRecyclerView()
        childPosition = recyclerView.getChildAdapterPosition(view)

        val options: Bundle? = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            *sharedViews).toBundle()

        val onEdit = { delay: Long ->
            Handler(Looper.getMainLooper()).postDelayed({
                onEdit(entity, options)
            }, delay)
        }

        if (recyclerView.isViewPartiallyVisible(view)) {
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    recyclerView.clearOnScrollListeners()
                    onEdit(100)
                }
            })
            recyclerView.smoothScrollToPosition(childPosition)
        } else onEdit(0)
    }

    override fun onSwipe() {
        if (!requireActivity().isChangingConfigurations) {
            selectionTracker.clearSelection()
        }
    }

    override fun isSelected(entity: T): Boolean {
        return selectionTracker.isSelected(entity)
    }

    private fun startActionMode() {
        requireActivity().startActionMode(actionModeCallback)
    }

    override fun onSharedViewReady() {
        shouldStartDelayedTransition = false
        requireActivity().startPostponedEnterTransition()
    }

    companion object {

        private const val EXTRA_CHILD_POSITION: String = "extra_child_position"
    }
}