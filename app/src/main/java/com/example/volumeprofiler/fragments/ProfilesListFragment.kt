package com.example.volumeprofiler.fragments

import android.content.*
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.interfaces.AnimImplementation
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.R
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.activities.EditProfileActivity
import com.example.volumeprofiler.receivers.AlarmReceiver
import com.example.volumeprofiler.services.NotificationWidgetService
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import com.example.volumeprofiler.viewmodels.ProfileListViewModel
import com.example.volumeprofiler.viewmodels.SharedViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

class ProfilesListFragment: Fragment(), AnimImplementation, LifecycleObserver {

    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private var sharedPreferencesUtil = SharedPreferencesUtil.getInstance()
    private val profileAdapter: ProfileAdapter = ProfileAdapter()
    private lateinit var positionMap: ArrayMap<UUID, Int>
    private val viewModel: ProfileListViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var uiReceiver: BroadcastReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Application.ACTION_UPDATE_UI) {
                Log.i(LOG_TAG, "onReceive()")
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
    private var processLifecycleReceiver: BroadcastReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Application.ACTION_GONE_BACKGROUND) {
                val isProfileQueryEmpty: Boolean? = sharedViewModel.isProfileQueryEmpty.value
                if (isProfileQueryEmpty != null && !isProfileQueryEmpty) {
                    startService()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(uiReceiver, arrayOf(Application.ACTION_UPDATE_UI))
        registerReceiver(processLifecycleReceiver, arrayOf(Application.ACTION_GONE_BACKGROUND))
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
    ): View? {
        val view: View = inflater.inflate(R.layout.profiles, container, false)
        floatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton.setOnClickListener {
            val intent = EditProfileActivity.newIntent(requireContext(), null)
            startActivity(intent)
        }
        initRecyclerView(view)
        setItemTouchHelper()
        return view
    }

    private fun initRecyclerView(view: View): Unit {
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = profileAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.profileListLiveData.observe(viewLifecycleOwner,
                Observer<List<Profile>> { t ->
                    if (t != null) {
                        var sortedList: List<Profile> = restoreChangedPositions(t)
                        sharedViewModel.setValue(sortedList.isEmpty())
                        updateUI(sortedList)
                    }
                })
        viewModel.associatedEventsLiveData.observe(viewLifecycleOwner,
                Observer<List<ProfileAndEvent>?> { t ->
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
        super.onPause()
    }

    override fun onDestroy(): Unit {
        unregisterReceiver(processLifecycleReceiver)
        unregisterReceiver(uiReceiver)
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

    private fun checkProfileView(isPressed: Boolean, currentPosition: Int): Unit {
        val lastIndex: Int = viewModel.lastActiveProfileIndex
        val currentProfile: Profile = profileAdapter.getProfile(currentPosition)
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

        val itemTouchCallBack = object: ItemTouchHelper.SimpleCallback(DRAG_DIRS, SWIPE_DIRS) {

            override fun isItemViewSwipeEnabled(): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                /*
                    No implementation
                 */
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

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
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

    private fun restoreChangedPositions(list: List<Profile>): List<Profile> {
        if (positionMap.isNotEmpty()) {
            val arrayList: ArrayList<Profile> = list as ArrayList<Profile>
            arrayList.sortWith(object : Comparator<Profile> {

                override fun compare(o1: Profile?, o2: Profile?): Int {
                    if (o1 != null && o2 != null) {
                        if (positionMap.containsKey(o1.id) && positionMap.containsKey(o2.id)) {
                            return positionMap[o1.id]!! - positionMap[o2.id]!!
                        }
                        return 0
                    }
                    return 0
                }
            })
            return arrayList.toList()
        }
        return list
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

    private inner class ProfileHolder(view: View): RecyclerView.ViewHolder(view), CompoundButton.OnCheckedChangeListener {

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

        private fun removeProfile(): Unit {
            val position: Int = absoluteAdapterPosition
            if (position == viewModel.lastActiveProfileIndex) {
                viewModel.lastActiveProfileIndex = -1
            }
            //expandedViews.remove(position)
            //ids.removeAt(position)
            profileAdapter.getProfile(position).let {
                val id: UUID = it.id
                if (sharedPreferencesUtil.getActiveProfileId() == id.toString()) {
                    sharedPreferencesUtil.clearActiveProfileRecord(id)
                }
                positionMap.remove(id)
                scaleDownAnimation(itemView)
                viewModel.removeProfile(it)
            }
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
            //expandedViews.add(absoluteAdapterPosition)
        }

        private fun collapseView(): Unit {
            TransitionManager.beginDelayedTransition(recyclerView, AutoTransition())
            expandableView.visibility = View.GONE
            expandImageView.animate().rotation(0.0f).start()
            //expandedViews.remove(absoluteAdapterPosition)
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
            editTextView.setOnClickListener { startActivity(EditProfileActivity.newIntent(requireContext(), profile.id)) }
            removeTextView.setOnClickListener {
                removeProfile()
            }
        }

        fun bindProfile(profile: Profile, position: Int): Unit {
            val isProfileActive: Boolean = sharedPreferencesUtil.isProfileActive(profile)
            checkBox.isChecked = isProfileActive
            if (isProfileActive) {
                viewModel.lastActiveProfileIndex = absoluteAdapterPosition
            }
            setupTextViews(profile)
            setCallbacks(profile)
            /*
            if (expandedViews.contains(position)) {
                expandView(false)
            }
             */
        }

        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            if (buttonView != null && buttonView.isPressed) {
                if (isChecked) {
                    checkProfileView(true, absoluteAdapterPosition)
                }
                else {
                    val lastIndex: Int = viewModel.lastActiveProfileIndex
                    val currentProfile: Profile = profileAdapter.getProfile(absoluteAdapterPosition)
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
        val settingsPair = ProfileUtil.getVolumeSettingsMapPair(profile)
        profileUtil.applyAudioSettings(settingsPair.first, settingsPair.second, profile.id, profile.title)
    }

    private inner class ProfileAdapter : androidx.recyclerview.widget.ListAdapter<Profile, ProfileHolder>(
        object : DiffUtil.ItemCallback<Profile>() {

        override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem == newItem
        }

    }) {

        private var lastPosition: Int = -1

        fun getProfile(position: Int): Profile = getItem(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileHolder {
            val view = layoutInflater.inflate(PROFILE_LAYOUT, parent, false)
            return ProfileHolder(view)
        }

        override fun onBindViewHolder(holder: ProfileHolder, position: Int) {
            val profile = getItem(position)
            if (holder.absoluteAdapterPosition > lastPosition) {
                lastPosition = position
                scaleUpAnimation(holder.itemView)
            }
            holder.bindProfile(profile, position)
        }
    }

    companion object {

        private const val SWIPE_DIRS: Int = 0
        private const val DRAG_DIRS: Int = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        private const val LOG_TAG: String = "ProfilesListFragment"
        private const val PROFILE_LAYOUT = R.layout.item_view
    }
}