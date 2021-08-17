package com.example.volumeprofiler.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.*
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.EditProfileActivity
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.BaseSelectionObserver
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.DetailsLookup
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.ItemDetails
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.KeyProvider
import com.example.volumeprofiler.interfaces.ActionModeProvider
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.animations.AnimUtil
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import com.example.volumeprofiler.viewmodels.ProfileListViewModel
import com.example.volumeprofiler.viewmodels.ViewpagerSharedViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.isNotEmpty
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.withIndex
import com.example.volumeprofiler.restoreChangedPosition

class ProfilesListFragment: Fragment(), ActionModeProvider<String> {

    private var sharedPreferencesUtil = SharedPreferencesUtil.getInstance()
    private val profileAdapter: ProfileAdapter = ProfileAdapter()
    private lateinit var positionMap: ArrayMap<UUID, Int>
    private lateinit var tracker: SelectionTracker<String>
    private lateinit var recyclerView: RecyclerView
    private val viewModel: ProfileListViewModel by viewModels()
    private val viewpagerSharedViewModel: ViewpagerSharedViewModel by activityViewModels()

    /*
    private var uiStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Application.ACTION_UPDATE_UI) {
                Log.i(LOG_TAG, "onReceive()")
                val id: UUID? =
                    intent.extras?.getSerializable(AlarmReceiver.EXTRA_PROFILE_ID) as UUID?
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

    private var processLifecycleReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Application.ACTION_GONE_BACKGROUND) {
                val isProfileQueryEmpty: Boolean? = viewpagerSharedViewModel.profileListLiveData.value?.isEmpty()
                if (isProfileQueryEmpty != null && !isProfileQueryEmpty) {
                    startService()
                }
            }
        }
    }
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        //registerReceiver(uiStateReceiver, arrayOf(Application.ACTION_UPDATE_UI))
        //registerReceiver(processLifecycleReceiver, arrayOf(Application.ACTION_GONE_BACKGROUND))
        positionMap = sharedPreferencesUtil.getRecyclerViewPositionsMap() ?: arrayMapOf()
        /*
        if (savedInstanceState != null) {
            expandedViews = savedInstanceState.getIntegerArrayList(KEY_EXPANDED_VIEWS) as ArrayList<Int>
        }
         */
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.profiles_list_fragment, container, false)
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
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = profileAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun initSelectionTracker(): Unit {
        tracker = SelectionTracker.Builder<String>(
            SELECTION_ID,
            recyclerView,
            KeyProvider(profileAdapter),
            DetailsLookup(recyclerView),
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
            .build()
        tracker.addObserver(BaseSelectionObserver<String>(WeakReference(this)))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewpagerSharedViewModel.profileListLiveData.observe(viewLifecycleOwner,
                Observer<List<Profile>> { t ->
                    if (t != null) {
                        val sortedList: List<Profile> = restoreChangedPosition(t, positionMap)
                        updateUI(sortedList)
                    }
                })
        viewModel.associatedEventsLiveData.observe(viewLifecycleOwner,
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

    override fun onDestroy(): Unit {
        //unregisterReceiver(processLifecycleReceiver)
        //unregisterReceiver(uiStateReceiver)
        super.onDestroy()
    }

    private fun registerReceiver(receiver: BroadcastReceiver, actions: Array<String>): Unit {
        val broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(requireContext().applicationContext)
        val filter: IntentFilter = IntentFilter().apply {
            for (i in actions) {
                this.addAction(i)
            }
        }
        broadcastManager.registerReceiver(receiver, filter)
    }

    /*
    private fun unregisterReceiver(receiver: BroadcastReceiver): Unit {
        val broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(requireContext().applicationContext)
        broadcastManager.unregisterReceiver(receiver)
    }

    private fun startService(): Unit {
        val context: Context = requireContext()
        val intent: Intent = Intent(context, NotificationWidgetService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        }
        else {
            context.startService(intent)
        }
    }
     */

    private fun checkProfileView(isPressed: Boolean, currentPosition: Int): Unit {
        val lastIndex: Int = viewModel.lastActiveProfileIndex
        val currentProfile: Profile = profileAdapter.getItemAtPosition(currentPosition)
        profileAdapter.notifyItemChanged(currentPosition)
        viewModel.lastActiveProfileIndex = currentPosition
        if (isPressed) {
            applyAudioSettings(currentProfile)
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
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun submitDataToAdapter(list: List<Profile>): Unit {
        profileAdapter.submitList(list)
    }

    private fun updateUI(profiles: List<Profile>) {
        if (profiles.isNotEmpty()) {
            view?.findViewById<TextView>(R.id.hint_profile)?.visibility = View.GONE
        }
        else {
            view?.findViewById<TextView>(R.id.hint_profile)?.visibility = View.VISIBLE
        }
        submitDataToAdapter(profiles)
    }

    private fun removeProfile(profile: Profile, position: Int): Unit {
        if (position == viewModel.lastActiveProfileIndex) {
            viewModel.lastActiveProfileIndex = -1
        }
        val id: UUID = profile.id
        if (sharedPreferencesUtil.getActiveProfileId() == id.toString()) {
            sharedPreferencesUtil.clearActiveProfileRecord(id)
        }
        positionMap.remove(id)
        viewModel.removeProfile(profile)
    }

    private inner class ProfileHolder(view: View):
            RecyclerView.ViewHolder(view),
            ViewHolderItemDetailsProvider<String>,
            CompoundButton.OnCheckedChangeListener {

        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox) as CheckBox
        private val editTextView: TextView = itemView.findViewById(R.id.editTextView) as TextView
        private val removeTextView: TextView = itemView.findViewById(R.id.removeTextView) as TextView
        private val expandImageView: ImageView = itemView.findViewById(R.id.expandButton) as ImageView
        private val expandableView: ConstraintLayout = itemView.findViewById(R.id.expandableView) as ConstraintLayout
        private val mediaVolValue: TextView = itemView.findViewById(R.id.mediaVolValue) as TextView
        private val callVolValue: TextView = itemView.findViewById(R.id.callVolValue) as TextView
        private val alarmVolValue: TextView = itemView.findViewById(R.id.alarmVolValue) as TextView
        private val notificationVolValue: TextView = itemView.findViewById(R.id.notificationVolValue) as TextView
        private val ringerVolValue: TextView = itemView.findViewById(R.id.ringerVolValue) as TextView

        init {
            expandableView.visibility = View.GONE
            checkBox.setOnCheckedChangeListener(this)
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> {
            return ItemDetails(absoluteAdapterPosition, profileAdapter.getItemAtPosition(absoluteAdapterPosition).id.toString())
        }

        private fun expandView(animate: Boolean): Unit {
            if (animate) {
                TransitionManager.beginDelayedTransition(recyclerView, AutoTransition())
                expandableView.visibility = View.VISIBLE
                expandImageView.animate().rotation(180.0f).start()
            }
            else {
                expandableView.visibility = View.VISIBLE
                expandImageView.rotation = 180.0f
            }
        }

        private fun collapseView(): Unit {
            TransitionManager.beginDelayedTransition(recyclerView, AutoTransition())
            expandableView.visibility = View.GONE
            expandImageView.animate().rotation(0.0f).start()
        }

        private fun setupTextViews(profile: Profile): Unit {
            val audioManager: AudioManager =
                    requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            checkBox.text = profile.title
            mediaVolValue.text = "${profile.mediaVolume}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}"
            callVolValue.text = "${profile.callVolume}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)}"
            alarmVolValue.text = "${profile.alarmVolume}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)}"
            notificationVolValue.text = "${profile.notificationVolume}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)}"
            ringerVolValue.text = "${profile.ringVolume}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)}"
        }

        private fun setCallbacks(profile: Profile): Unit {
            expandImageView.setOnClickListener {
                if (expandableView.visibility == View.GONE) {
                    expandView(true)
                }
                else {
                    collapseView()
                }
            }
            editTextView.setOnClickListener {
                startActivity(EditProfileActivity.newIntent(requireContext(), profile)) }
            removeTextView.setOnClickListener {
                removeProfile(profile, absoluteAdapterPosition)
            }
        }

        fun bind(profile: Profile, isSelected: Boolean): Unit {
            AnimUtil.selectedItemAnimation(itemView, isSelected)
            val isProfileActive: Boolean = sharedPreferencesUtil.isProfileActive(profile)
            checkBox.isChecked = isProfileActive
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
                    if (sharedPreferencesUtil.getActiveProfileId() == currentProfile.id.toString()) {
                        sharedPreferencesUtil.clearActiveProfileRecord(currentProfile.id)
                    }
                    if (lastIndex != -1) {
                        viewModel.lastActiveProfileIndex = -1
                    }
                }
            }
        }
    }

    private fun applyAudioSettings(profile: Profile): Unit {
        val profileUtil = ProfileUtil.getInstance()
        profileUtil.applyAudioSettings(profile)
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

        fun getItemAtPosition(position: Int): Profile = getItem(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileHolder {
            val view = layoutInflater.inflate(PROFILE_LAYOUT, parent, false)
            return ProfileHolder(view)
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