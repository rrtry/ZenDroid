package com.example.volumeprofiler.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
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
import com.example.volumeprofiler.interfaces.ActionModeProvider
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.animations.AnimUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import com.example.volumeprofiler.util.animations.Scale
import com.example.volumeprofiler.viewmodels.ProfileListViewModel
import com.example.volumeprofiler.viewmodels.MainActivitySharedViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.isNotEmpty
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.withIndex

class ProfilesListFragment: Fragment(), ActionModeProvider<String> {

    private var sharedPreferencesUtil = SharedPreferencesUtil.getInstance()
    private val profileAdapter: ProfileAdapter = ProfileAdapter()
    private lateinit var positionMap: ArrayMap<UUID, Int>
    private lateinit var tracker: SelectionTracker<String>

    private val viewModel: ProfileListViewModel by viewModels()
    private val mainActivitySharedViewModel: MainActivitySharedViewModel by activityViewModels()

    private var _binding: ProfilesListFragmentBinding? = null
    private val binding: ProfilesListFragmentBinding get() = _binding!!

    /*
    private var uiStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Application.ACTION_UPDATE_UI) {
                val id: UUID? = intent.extras?.getSerializable(AlarmReceiver.EXTRA_PROFILE_ID) as UUID?
                if (id != null) {
                    for ((index, item) in profileAdapter.currentList.withIndex()) {
                        if (item.id == id) {
                            checkProfileView(false, index)
                        }
                    }
                }
            }
        }
    }
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setPositionMap()
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
        val floatingActionButton: FloatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton.setOnClickListener {
            val intent: Intent = EditProfileActivity.newIntent(requireContext(), null)
            startActivity(intent)
        }
        initRecyclerView(view)
        setItemTouchHelper()
        initSelectionTracker()
        return view
    }

    private fun initRecyclerView(view: View): Unit {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = profileAdapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun initSelectionTracker(): Unit {
        tracker = SelectionTracker.Builder<String>(
            SELECTION_ID,
            binding.recyclerView,
            KeyProvider(profileAdapter),
            DetailsLookup(binding.recyclerView),
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
            .build()
        tracker.addObserver(BaseSelectionObserver<String>(WeakReference(this)))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivitySharedViewModel.profileListLiveData.observe(viewLifecycleOwner,
                Observer<List<Profile>> { t ->
                    if (t != null) {
                        updateUI(t)
                    }
                })
        viewModel.alarmsToRemoveLiveData.observe(viewLifecycleOwner,
                Observer<List<AlarmTrigger>?> { t ->
                    if (t != null && t.isNotEmpty()) {
                        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
                        alarmUtil.cancelMultipleAlarms(t)
                    }
                })
    }

    override fun onPause() {
        if (positionMap.isNotEmpty()) {
            sharedPreferencesUtil.saveRecyclerViewPositionsMap(positionMap)
        }
        tracker.clearSelection()
        super.onPause()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun checkProfileView(isPressed: Boolean, currentPosition: Int): Unit {
        val lastIndex: Int = viewModel.lastActiveProfileIndex
        val currentProfile: Profile = profileAdapter.getItemAtPosition(currentPosition)
        profileAdapter.notifyItemChanged(currentPosition)
        viewModel.lastActiveProfileIndex = currentPosition
        if (isPressed) {
            viewModel.applyProfileSettings(currentProfile)
        }
        if (lastIndex != -1) {
            profileAdapter.notifyItemChanged(lastIndex)
        }
    }

    private fun setItemTouchHelper(): Unit {
        val itemTouchCallBack = object: ItemTouchHelper.Callback() {

            override fun isItemViewSwipeEnabled(): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }

            private fun swapViews(fromPos: Int, toPos: Int, list: List<Profile>): List<Profile> {
                val arrayList: ArrayList<Profile> = ArrayList(list)
                if (fromPos < toPos) {
                    for (i in fromPos until toPos) {
                        Collections.swap(arrayList, i, i + 1)
                    }
                }
                else {
                    for (i in fromPos downTo toPos + 1) {
                        Collections.swap(arrayList, i, i - 1)
                    }
                }
                return arrayList.toList()
            }

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(DRAG_FLAGS, SWIPE_FLAGS)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                tracker.clearSelection()
                return true
            }

            override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                 fromPos: Int, target: RecyclerView.ViewHolder,
                                 toPos: Int, x: Int, y: Int): Unit {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
                val adapterData: List<Profile> = profileAdapter.currentList
                positionMap[adapterData[fromPos].id] = toPos
                positionMap[adapterData[toPos].id] = fromPos
                if (viewModel.lastActiveProfileIndex == fromPos) {
                    viewModel.lastActiveProfileIndex = toPos
                }
                else if (viewModel.lastActiveProfileIndex == toPos) {
                    viewModel.lastActiveProfileIndex = fromPos
                }
                val modifiedList: List<Profile> = swapViews(fromPos, toPos, adapterData)
                submitDataToAdapter(modifiedList)
            }

            override fun isLongPressDragEnabled(): Boolean = true
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchCallBack)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun submitDataToAdapter(list: List<Profile>): Unit {
        profileAdapter.submitList(list)
    }

    private fun updateUI(profiles: List<Profile>) {
        if (profiles.isNotEmpty()) {
            binding.placeHolderText.visibility = View.GONE
        }
        else {
            binding.placeHolderText.visibility = View.VISIBLE
        }
        submitDataToAdapter(profiles)
    }

    private inner class ProfileHolder(view: View):
            RecyclerView.ViewHolder(view),
            ViewHolderItemDetailsProvider<String>,
            CompoundButton.OnCheckedChangeListener {

        private val adapterBinding: ProfileItemViewBinding = profileAdapter.binding

        init {
            adapterBinding.expandableView.visibility = View.GONE
            adapterBinding.checkBox.setOnCheckedChangeListener(this)
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> {
            return ItemDetails(absoluteAdapterPosition, profileAdapter.getItemAtPosition(absoluteAdapterPosition).id.toString())
        }

        private fun expandView(animate: Boolean): Unit {
            if (animate) {
                val transition: TransitionSet = TransitionSet().apply {
                    this.ordering = TransitionSet.ORDERING_SEQUENTIAL
                    this.addTransition(ChangeBounds())
                    this.addTransition(Scale())
                }
                TransitionManager.beginDelayedTransition(adapterBinding.root, transition)
                adapterBinding.expandableView.visibility = View.VISIBLE
                adapterBinding.expandButton.animate().rotation(180.0f).start()
            }
            else {
                adapterBinding.expandableView.visibility = View.VISIBLE
                adapterBinding.expandButton.rotation = 180f
            }
        }

        private fun collapseView(): Unit {
            val transition: TransitionSet = TransitionSet().apply {
                this.ordering = TransitionSet.ORDERING_SEQUENTIAL
                this.addTransition(Scale())
                this.addTransition(ChangeBounds())
            }
            TransitionManager.beginDelayedTransition(adapterBinding.root, transition)
            adapterBinding.expandableView.visibility = View.GONE
            adapterBinding.expandButton.animate().rotation(0f).start()
        }

        private fun setupTextViews(profile: Profile): Unit {
            adapterBinding.checkBox.text = profile.title
        }

        private fun setCallbacks(profile: Profile): Unit {
            profileAdapter.binding.expandButton.setOnClickListener {
                if (adapterBinding.expandableView.visibility == View.GONE) {
                    expandView(true)
                }
                else {
                    collapseView()
                }
            }
            adapterBinding.editProfileButton.setOnClickListener {
                startActivity(EditProfileActivity.newIntent(requireContext(), profile)) }
            adapterBinding.removeProfileButton.setOnClickListener {
                viewModel.removeProfile(profile, absoluteAdapterPosition, positionMap)
            }
        }

        fun bind(profile: Profile, isSelected: Boolean): Unit {
            AnimUtil.selectedItemAnimation(itemView, isSelected)
            val isProfileActive: Boolean = sharedPreferencesUtil.isProfileActive(profile)
            adapterBinding.checkBox.isChecked = isProfileActive
            if (isProfileActive) {
                viewModel.lastActiveProfileIndex = absoluteAdapterPosition
            }
            setupTextViews(profile)
            setCallbacks(profile)
        }

        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            if (buttonView != null && buttonView.isPressed) {
                if (isChecked) {
                    checkProfileView(true, absoluteAdapterPosition)
                }
                else {
                    val lastIndex: Int = viewModel.lastActiveProfileIndex
                    val currentProfile: Profile = profileAdapter.getItemAtPosition(absoluteAdapterPosition)
                    viewModel.clearActiveProfileRecord(currentProfile)
                    if (lastIndex != -1) {
                        viewModel.lastActiveProfileIndex = -1
                    }
                }
            }
        }
    }

    private inner class ProfileAdapter : ListAdapter<Profile, ProfileHolder>(

        object : DiffUtil.ItemCallback<Profile>() {

        override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem == newItem
        }

        }), ListAdapterItemProvider<String> {

        private var lastPosition: Int = -1
        lateinit var binding: ProfileItemViewBinding

        fun getItemAtPosition(position: Int): Profile = getItem(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileHolder {
            binding = ProfileItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ProfileHolder(binding.root)
        }

        override fun onBindViewHolder(holder: ProfileHolder, position: Int) {
            val profile = getItem(position)
            if (holder.absoluteAdapterPosition > lastPosition) {
                lastPosition = position
            }
            tracker.let {
                holder.bind(profile, it.isSelected(getItemAtPosition(position).id.toString()))
            }
        }

        override fun getItemKey(position: Int): String {
            return this.getItemAtPosition(position).id.toString()
        }

        override fun getPosition(key: String): Int {
            return this.currentList.indexOfFirst { key == it.id.toString() }
        }
    }

    companion object {

        private const val SWIPE_FLAGS: Int = 0
        private const val DRAG_FLAGS: Int = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        private const val LOG_TAG: String = "ProfilesListFragment"
        private const val PROFILE_LAYOUT = R.layout.profile_item_view
        private const val SELECTION_ID: String = "PROFILE"
    }

    override fun onActionItemRemove() {
        for (i in tracker.selection) {
            val id: UUID = UUID.fromString(i)
            for ((index, j) in profileAdapter.currentList.withIndex()) {
                if (j.id == id) {
                    viewModel.removeProfile(j, index, positionMap)
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