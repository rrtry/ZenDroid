package com.example.volumeprofiler.fragments

import android.animation.*
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.lifecycle.*
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.ListAdapter
import androidx.transition.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.EditProfileActivity
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.BaseSelectionObserver
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.DetailsLookup
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.ItemDetails
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.KeyProvider
import com.example.volumeprofiler.databinding.ProfileItemViewBinding
import com.example.volumeprofiler.databinding.ProfilesListFragmentBinding
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.interfaces.ActionModeProvider
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.util.animations.AnimUtil
import com.example.volumeprofiler.viewmodels.MainActivityViewModel
import com.example.volumeprofiler.viewmodels.ProfilesListViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject
import androidx.fragment.app.*
import com.example.volumeprofiler.interfaces.PermissionRequestCallback
import android.Manifest.permission.*
@AndroidEntryPoint
class ProfilesListFragment: Fragment(), ActionModeProvider<String> {

    @Inject
    lateinit var sharedPreferencesUtil: SharedPreferencesUtil

    @Inject
    lateinit var alarmUtil: AlarmUtil

    @Inject
    lateinit var geofenceUtil: GeofenceUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    @Inject
    lateinit var eventBus: EventBus

    private val profileAdapter: ProfileAdapter = ProfileAdapter()
    private var selectedItems: ArrayList<String> = arrayListOf()
    private lateinit var tracker: SelectionTracker<String>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var positionMap: ArrayMap<UUID, Int>

    //private var pendingProfileAction: Pair<Int, Int> = Pair(RecyclerView.NO_POSITION, NO_ACTION)

    private val viewModel: ProfilesListViewModel by viewModels()
    private val sharedViewModel: MainActivityViewModel by activityViewModels()

    private var _binding: ProfilesListFragmentBinding? = null
    private val binding: ProfilesListFragmentBinding get() = _binding!!

    private var searchQuery: String? = null

    private var callback: PermissionRequestCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireActivity() as PermissionRequestCallback
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val profile: Profile = it.data?.getParcelableExtra(EditProfileActivity.EXTRA_PROFILE)!!
                val update: Boolean = it.data?.getBooleanExtra(EditProfileActivity.EXTRA_SHOULD_UPDATE, false)!!
                if (update) {
                    viewModel.updateProfile(profile)
                } else {
                    viewModel.addProfile(profile)
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
        activityResultLauncher.unregister()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val searchView: SearchView = menu.findItem(R.id.search_item).actionView as SearchView
        val searchBar: LinearLayout = searchView.findViewById(R.id.search_bar)
        searchBar.layoutTransition = LayoutTransition()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText
                profileAdapter.filter.filter(newText)
                return true
            }
        })
        searchView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {

            override fun onViewAttachedToWindow(v: View?) {

            }

            override fun onViewDetachedFromWindow(v: View?) {
                searchView.setQuery(null, false)
                submitFullListToAdapter(profileAdapter.currentFullItemsList, true)
            }
        })
        if (isSearchQueryNotEmpty()) {
            searchView.isIconified = false
            searchView.setQuery(searchQuery, false)
        }
    }

    private fun isSearchQueryNotEmpty(): Boolean {
        return searchQuery != null && searchQuery!!.isNotBlank() && searchQuery!!.isNotEmpty()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            searchQuery = savedInstanceState.getString(EXTRA_QUERY)
        }
        setHasOptionsMenu(true)
        setPositionMap()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(EXTRA_SELECTION, selectedItems)
        outState.putString(EXTRA_QUERY, searchQuery)
        outState.putParcelable(EXTRA_RV_STATE, binding.recyclerView.layoutManager?.onSaveInstanceState())
    }

    private fun startDetailsActivity(profile: Profile?): Unit {
        val intent: Intent = Intent(requireContext(), EditProfileActivity::class.java)
        if (profile != null) {
            intent.putExtra(EditProfileActivity.EXTRA_PROFILE, profile)
        }
        activityResultLauncher.launch(intent)
    }

    private fun setPositionMap(): Unit {
        positionMap = sharedPreferencesUtil.getRecyclerViewPositionsMap() ?: arrayMapOf()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = ProfilesListFragmentBinding.inflate(inflater, container, false)
        val view: View = binding.root
        initRecyclerView(savedInstanceState)
        initSelectionTracker(savedInstanceState)
        return view
    }

    private fun initRecyclerView(savedInstanceState: Bundle?): Unit {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = profileAdapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun initSelectionTracker(savedInstanceState: Bundle?): Unit {
        tracker = SelectionTracker.Builder<String>(
                SELECTION_ID,
                binding.recyclerView,
                KeyProvider(profileAdapter),
                DetailsLookup(binding.recyclerView),
                StorageStrategy.createStringStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
            .build()
        tracker.addObserver(BaseSelectionObserver(WeakReference(this)))
        if (savedInstanceState != null) {
            selectedItems = savedInstanceState.getStringArrayList(EXTRA_SELECTION) as ArrayList<String>
            tracker.setItemsSelected(selectedItems, true)
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.createProfileButton.setOnClickListener {
            startDetailsActivity(null)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.eventFlow.onEach {
                        when (it) {
                            is ProfilesListViewModel.Event.CancelAlarmsEvent -> {
                                if (!it.alarms.isNullOrEmpty()) {
                                    alarmUtil.cancelAlarms(it.alarms)
                                }
                            }
                            is ProfilesListViewModel.Event.RemoveGeofencesEvent -> {
                                for (i in it.geofences) {
                                    geofenceUtil.removeGeofence(
                                        i.location,
                                        i.onEnterProfile,
                                        i.onExitProfile
                                    )
                                }
                            }
                        }
                    }.collect()
                }
                launch {
                    viewModel.profilesFlow.map { list ->
                                if (sharedPreferencesUtil.getRecyclerViewPositionsMap() != null) {
                                    restoreChangedPositions(list, sharedPreferencesUtil.getRecyclerViewPositionsMap()!!)
                                } else {
                                    list
                                }
                            }
                            .onEach { list -> sharedViewModel.showDialog.value = list.isEmpty()
                                updateUI(list)
                            }.collect()
                }
                launch {
                    eventBus.sharedFlow.collectLatest {
                        if (it is EventBus.Event.ProfileApplied) {
                            setEnabledProfile(it.id)
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (positionMap.isNotEmpty()) {
            sharedPreferencesUtil.writeProfilePositions(positionMap)
        }
        tracker.clearSelection()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setEnabledProfile(id: UUID): Unit {
        for (item in profileAdapter.currentList) {
            if (item.id == id) {
                updatePreviousPosition()
                updateCurrentPosition(id)
                viewModel.lastSelected = id
            }
        }
    }

    private fun removeProfile(profile: Profile, position: Int): Unit {
        if (profileAdapter.getItemAtPosition(position).id == viewModel.lastSelected) {
            viewModel.lastSelected = null
        }
        if (sharedPreferencesUtil.isProfileEnabled(profile)) {
            setDefaults()
        }
        positionMap.remove(profile.id)
        viewModel.removeProfile(profile)
    }

    private fun submitFullListToAdapter(list: List<Profile>, apply: Boolean): Unit {
        profileAdapter.submitFullList(list, apply)
    }

    private fun updateUI(profiles: List<Profile>) {
        if (profiles.isNotEmpty()) {
            binding.placeHolderText.visibility = View.GONE
        }
        else {
            binding.placeHolderText.visibility = View.VISIBLE
        }

        if (isSearchQueryNotEmpty()) {
            profileAdapter.submitFilteredList(profiles)
            submitFullListToAdapter(profiles, false)
        } else {
            submitFullListToAdapter(profiles, true)
        }
    }

    private fun updatePreviousPosition(): Unit {
        if (viewModel.lastSelected != null) {
            profileAdapter.notifyItemChanged(profileAdapter.getPosition(viewModel.lastSelected!!), true)
        }
    }

    private fun updateCurrentPosition(id: UUID): Unit {
        profileAdapter.notifyItemChanged(profileAdapter.getPosition(id), true)
    }

    private fun setProfile(profile: Profile) {
        profileUtil.setProfile(profile)
        updatePreviousPosition()
        updateCurrentPosition(profile.id)
        viewModel.lastSelected = profile.id
    }

    private fun setDefaults(): Unit {
        if (profileUtil.grantedSystemPreferencesAccess()) {
            profileUtil.setDefaults()
        } else {
            Snackbar.make(
                binding.root,
                "Failed to restore settings due to missing permissions",
                Snackbar.LENGTH_LONG)
                .show()
        }
        updatePreviousPosition()
        viewModel.lastSelected = null
    }

    private inner class ProfileHolder(view: View):
            RecyclerView.ViewHolder(view),
            ViewHolderItemDetailsProvider<String>,
            View.OnClickListener {

        private val adapterBinding: ProfileItemViewBinding = profileAdapter.binding

        init {
            adapterBinding.root.setOnClickListener(this)
            adapterBinding.expandableView.visibility = View.GONE
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> {
            return ItemDetails(bindingAdapterPosition, profileAdapter.getItemAtPosition(bindingAdapterPosition).id.toString())
        }

        private fun expandView(animate: Boolean): Unit {
            if (animate) {
                TransitionManager.beginDelayedTransition(binding.root, AutoTransition())
                adapterBinding.expandButton.animate().rotation(180.0f).start()
            }
            else {
                adapterBinding.expandButton.rotation = 180f
            }
            adapterBinding.itemSeparator.visibility = View.VISIBLE
            adapterBinding.expandableView.visibility = View.VISIBLE
        }

        private fun collapseView(): Unit {
            TransitionManager.beginDelayedTransition(binding.root, AutoTransition())
            adapterBinding.itemSeparator.visibility = View.GONE
            adapterBinding.expandableView.visibility = View.GONE
            adapterBinding.expandButton.animate().rotation(0f).start()
        }

        private fun setCheckBoxText(profile: Profile): Unit {
            adapterBinding.checkBox.text = profile.title
        }

        private fun setViewScale(isSelected: Boolean): Unit {
            val scale: Float = if (isSelected) 0.8f else 1.0f
            itemView.scaleX = scale
            itemView.scaleY = scale
        }

        private fun setSelectedState(isSelected: Boolean, animate: Boolean): Unit {
            if (animate) {
                AnimUtil.selectedItemAnimation(itemView, isSelected)
            } else {
                setViewScale(isSelected)
            }
        }

        fun setEnabledState(profile: Profile): Unit {
            val isProfileActive: Boolean = sharedPreferencesUtil.isProfileEnabled(profile)
            adapterBinding.checkBox.isChecked = isProfileActive
            if (isProfileActive) {
                viewModel.lastSelected = profileAdapter.getItemAtPosition(bindingAdapterPosition).id
            }
        }

        private fun setListeners(profile: Profile): Unit {
            adapterBinding.expandButton.setOnClickListener {
                if (adapterBinding.expandableView.visibility == View.GONE) {
                    expandView(true)
                }
                else {
                    collapseView()
                }
            }
            adapterBinding.editProfileButton.setOnClickListener {
                when {
                    profileUtil.grantedRequiredPermissions(profile) -> {
                        startDetailsActivity(profile)
                    }
                    profileUtil.shouldRequestPhonePermission(profile) -> {
                        callback?.requestProfilePermissions(profile)
                    }
                    else -> {
                        sendSystemPreferencesAccessNotification(requireContext(), profileUtil)
                    }
                }
            }
            adapterBinding.removeProfileButton.setOnClickListener {
                removeProfile(profile, bindingAdapterPosition)
            }
        }

        fun bind(profile: Profile, isSelected: Boolean, animate: Boolean): Unit {
            setSelectedState(isSelected, animate)
            setEnabledState(profile)
            setCheckBoxText(profile)
            setListeners(profile)
        }

        override fun onClick(v: View?) {
            val profile: Profile = profileAdapter.getItemAtPosition(bindingAdapterPosition)
            when {
                profileUtil.grantedRequiredPermissions(profile) -> {
                    if (sharedPreferencesUtil.isProfileEnabled(profile)) {
                        setDefaults()
                    } else {
                        setProfile(profile)
                    }
                }
                profileUtil.shouldRequestPhonePermission(profile) -> {
                    callback?.requestProfilePermissions(profile)
                }
                else -> {
                    sendSystemPreferencesAccessNotification(requireContext(), profileUtil)
                }
            }
        }
    }

    private inner class ProfileAdapter : ListAdapter<Profile, ProfileHolder>(object : DiffUtil.ItemCallback<Profile>() {

        override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem == newItem
        }

    }), ListAdapterItemProvider<String>, Filterable {

        private var lastPosition: Int = -1
        lateinit var binding: ProfileItemViewBinding

        lateinit var currentFullItemsList: List<Profile>

        fun getItemAtPosition(position: Int): Profile {
            return getItem(position)
        }

        fun getPosition(id: UUID): Int {
            for ((index, i) in currentList.withIndex()) {
                if (i.id == id) {
                    return index
                }
            }
            return -1
        }

        private fun isSelected(tracker: SelectionTracker<String>, position: Int): Boolean {
            return tracker.isSelected(getItemAtPosition(position).id.toString())
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileHolder {
            binding = ProfileItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ProfileHolder(binding.root)
        }

        override fun onBindViewHolder(holder: ProfileHolder, @SuppressLint("RecyclerView") position: Int) {
            val profile = getItem(position)
            if (holder.absoluteAdapterPosition > lastPosition) {
                lastPosition = position
            }
            tracker.let {
                var animate: Boolean = true
                if (isSelected(tracker, position)) {
                    if (selectedItems.contains(profile.id.toString())) {
                        animate = false
                    } else {
                        selectedItems.add(profile.id.toString())
                    }
                } else {
                    selectedItems.remove(profile.id.toString())
                }
                holder.bind(profile, it.isSelected(getItemAtPosition(position).id.toString()), animate)
            }
        }

        override fun getItemKey(position: Int): String {
            return getItemAtPosition(position).id.toString()
        }

        override fun getPosition(key: String): Int {
            return currentList.indexOfFirst { key == it.id.toString() }
        }

        fun submitFullList(list: List<Profile>, apply: Boolean): Unit {
            currentFullItemsList = list
            if (apply) {
                submitList(list)
            }
        }

        fun submitFilteredList(list: List<Profile>): Unit {
            val filtered: List<Profile> = list.filter { profile -> if (isSearchQueryNotEmpty()) profile.title.contains(searchQuery!!, true) else false }
            submitList(filtered)
        }

        override fun getFilter(): Filter {
            return object : Filter() {

                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val filterResults: FilterResults = FilterResults()
                    return if (constraint != null && constraint.isNotEmpty() && constraint.isNotBlank()) {
                        filterResults.values = currentFullItemsList.filter { profile -> profile.title.contains(constraint.trim(), true) }
                        filterResults
                    } else {
                        filterResults
                    }
                }

                @Suppress("unchecked_cast")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    if (results?.values != null) {
                        submitList(results.values as List<Profile>)
                    } else {
                        submitList(currentFullItemsList)
                    }
                }
            }
        }
    }

    companion object {

        private const val SELECTION_ID: String = "PROFILE"
        private const val EXTRA_SELECTION: String = "extra_selection"
        private const val EXTRA_QUERY: String = "extra_query"
        private const val EXTRA_RV_STATE: String = "abs_position"
        private const val NO_ACTION: Int = 0x00
        private const val ACTION_OPEN_EDITOR: Int = 0x01
        private const val ACTION_APPLY_PROFILE: Int = 0x02

    }

    override fun onActionItemRemove() {
        for (i in tracker.selection) {
            val id: UUID = UUID.fromString(i)
            for ((index, j) in profileAdapter.currentList.withIndex()) {
                if (j.id == id) {
                    removeProfile(j, index)
                }
            }
        }
    }

    override fun getTracker(): SelectionTracker<String> {
        return tracker
    }

    override fun getFragmentActivity(): FragmentActivity {
        return requireActivity()
    }
}