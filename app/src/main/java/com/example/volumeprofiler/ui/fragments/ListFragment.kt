package com.example.volumeprofiler.ui.fragments

import android.Manifest.permission.*
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.util.Pair
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.provider.Settings
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.volumeprofiler.core.PreferencesManager
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.interfaces.*
import com.example.volumeprofiler.util.ViewUtil.Companion.isViewPartiallyVisible
import com.example.volumeprofiler.util.canWriteSettings
import com.example.volumeprofiler.util.checkPermission
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

abstract class ListFragment<T: Parcelable, VB: ViewBinding, VH: RecyclerView.ViewHolder, IB: ViewBinding>:
    ViewBindingFragment<VB>(),
    FragmentStateListener,
    ActionModeProvider,
    ListItemActionListener<T> {

    protected var callback: MainActivityCallback? = null
    private var actionMode: ActionMode?
        get() = callback?.actionMode
        set(value) {
            callback?.actionMode = value
        }

    protected lateinit var selectionTracker: SelectionTracker<T>
    private var childPosition: Int = 0

    abstract val selectionId: String
    abstract val listItem: Class<T>

    abstract fun getRecyclerView(): RecyclerView
    abstract fun getAdapter(): RecyclerView.Adapter<VH>
    abstract fun onPermissionResult(permission: String, granted: Boolean)

    private lateinit var notificationPolicySettingsLauncher: ActivityResultLauncher<Intent>
    private lateinit var systemSettingsLauncher: ActivityResultLauncher<Intent>
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var notificationManager: NotificationManager

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
        callback = requireActivity() as MainActivityCallback
        notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

    protected fun showDeniedPermissionHint(profile: Profile): Boolean {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            callback?.showSnackBar(
                "Grant Do Not Disturb access",
                Snackbar.LENGTH_INDEFINITE)
            {
                notificationPolicySettingsLauncher.launch(
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                )
            }
            return true
        } else if (!canWriteSettings(requireContext())) {
            callback?.showSnackBar(
                "Grant System Settings access",
                Snackbar.LENGTH_INDEFINITE)
            {
                systemSettingsLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:${requireContext().packageName}"))
                )
            }
            return true
        } else if (!checkPermission(READ_PHONE_STATE) && profile.streamsUnlinked) {
            callback?.showSnackBar(
                "Grant 'Phone' permission",
                Snackbar.LENGTH_INDEFINITE)
            {
                phonePermissionLauncher.launch(READ_PHONE_STATE)
            }
            return true
        }
        return false
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