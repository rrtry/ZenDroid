package ru.rrtry.silentdroid.ui.fragments

import android.Manifest.permission.*
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.util.Pair
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.os.PowerManager
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.SharedElementCallback
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import ru.rrtry.silentdroid.selection.DetailsLookup
import ru.rrtry.silentdroid.selection.KeyProvider
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.*
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.entities.Hint
import ru.rrtry.silentdroid.entities.ListItem
import ru.rrtry.silentdroid.util.ViewUtil.Companion.isViewPartiallyVisible
import ru.rrtry.silentdroid.util.canWriteSettings
import ru.rrtry.silentdroid.util.checkPermission
import ru.rrtry.silentdroid.interfaces.*

abstract class ListFragment<T: Parcelable, VB: ViewBinding, VH: RecyclerView.ViewHolder, IB: ViewBinding, AD>:
    ViewBindingFragment<VB>(),
    FragmentStateListener,
    ActionModeProvider,
    ListViewContract<T> {

    protected var callback: ViewPagerActivityCallback? = null
    protected open val hintRes: Int = -1
    private var actionMode: ActionMode?
        get() = callback?.actionMode
        set(value) { callback?.actionMode = value }

    protected lateinit var selectionTracker: SelectionTracker<T>
    private var childPosition: Int = 0

    abstract val selectionId: String
    abstract val listItem: Class<T>

    abstract fun getRecyclerView(): RecyclerView
    abstract fun getAdapter(): AD
    abstract fun onPermissionResult(permission: String, granted: Boolean)

    private lateinit var notificationPolicySettingsLauncher: ActivityResultLauncher<Intent>
    private lateinit var systemSettingsLauncher: ActivityResultLauncher<Intent>
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var notificationManager: NotificationManager
    private lateinit var powerManager: PowerManager

    private val powerSaveModeStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (hintRes != -1) {
                showPowerSaveModeHint(requireContext().resources.getString(hintRes))
            }
        }
    }
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

    @Suppress("unchecked_cast")
    private fun getChildViewHolderBinding(): IB? {
        val recyclerView: RecyclerView = getRecyclerView()
        recyclerView.layoutManager?.findViewByPosition(childPosition)?.let { child ->
            return (recyclerView.getChildViewHolder(child) as ViewHolder<IB>).binding
        }
        return null
    }

    protected open fun mapSharedElements(
        names: MutableList<String>?,
        sharedElements: MutableMap<String, View>?
    ): IB? {
        return getChildViewHolderBinding()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireActivity() as ViewPagerActivityCallback

        notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager

        phonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            onPermissionResult(READ_PHONE_STATE, checkPermission(READ_PHONE_STATE))
        }
        systemSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onPermissionResult(WRITE_SETTINGS, canWriteSettings(requireContext()))
        }
        notificationPolicySettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onPermissionResult(ACCESS_NOTIFICATION_POLICY, notificationManager.isNotificationPolicyAccessGranted)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childPosition = savedInstanceState?.getInt(EXTRA_CHILD_POSITION, 0) ?: 0
        requireActivity().postponeEnterTransition()
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
        phonePermissionLauncher.unregister()
        systemSettingsLauncher.unregister()
        notificationPolicySettingsLauncher.unregister()
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

    override fun onStart() {
        super.onStart()
        requireActivity().registerReceiver(
            powerSaveModeStateReceiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        )
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(powerSaveModeStateReceiver)
    }

    @Suppress("unchecked_cast")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getRecyclerView().let { recyclerView ->

            recyclerView.adapter = (getAdapter() as RecyclerView.Adapter<VH>).apply {
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
            ).withSelectionPredicate(object : SelectionTracker.SelectionPredicate<T>() {

                override fun canSetStateForKey(key: T, nextState: Boolean): Boolean {
                    return key !is Hint
                }

                override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean {
                    return true
                }

                override fun canSelectMultiple(): Boolean {
                    return true
                }

            }).build()
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
        } else {
            onEdit(0)
        }
    }

    protected fun setSharedElementCallback() {
        requireActivity().setExitSharedElementCallback(object : SharedElementCallback() {

            override fun onMapSharedElements(
                names: MutableList<String>?,
                sharedElements: MutableMap<String, View>?
            ) {
                super.onMapSharedElements(names, sharedElements)
                mapSharedElements(names, sharedElements)
            }
        })
    }

    protected fun showPowerSaveModeHint(text: String) {

        val adapter: RecyclerView.Adapter<VH> = getAdapter() as RecyclerView.Adapter<VH>
        val provider: AdapterDatasetProvider<ListItem<Int>> = adapter as AdapterDatasetProvider<ListItem<Int>>

        val items: MutableList<ListItem<Int>> = provider.currentList.toMutableList()
        val hint: Hint = Hint(text)

        if (powerManager.isPowerSaveMode) {
            if (!items.contains(hint)) {
                items.add(0, hint)
                provider.currentList = items
                adapter.notifyItemInserted(0)
            }
        } else if (items.contains(hint)) {
            items.remove(hint)
            provider.currentList = items
            adapter.notifyItemRemoved(0)
        }
    }

    override fun onFragmentSwiped() {
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
        requireActivity().startPostponedEnterTransition()
    }

    companion object {

        private const val EXTRA_CHILD_POSITION: String = "extra_child_position"
    }
}