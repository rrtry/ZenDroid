package com.example.volumeprofiler.fragments

import android.content.*
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.interfaces.AnimImplementation
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.R
import com.example.volumeprofiler.Application
import com.example.volumeprofiler.activities.EditProfileActivity
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.AudioUtil
import com.example.volumeprofiler.viewmodels.ProfileListViewModel
import com.example.volumeprofiler.viewmodels.SharedViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.collections.ArrayList

class ProfilesListFragment: Fragment(), AnimImplementation, LifecycleObserver {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var receiver: BroadcastReceiver
    //private val ids: ArrayList<UUID> = arrayListOf()
    private val profileAdapter: ProfileAdapter = ProfileAdapter()
    private var expandedViews: ArrayList<Int> = arrayListOf()
    private val viewModel: ProfileListViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var audioManager: AudioManager

    private fun registerReceiver(): Unit {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Application.ACTION_UPDATE_UI) {

                }
            }
        }
        val broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(requireContext().applicationContext)
        val filter: IntentFilter = IntentFilter(Application.ACTION_UPDATE_UI)
        broadcastManager.registerReceiver(receiver, filter)
    }

    private fun unregisterReceiver(): Unit {
        Log.i("ProfilesListFragment", "unregisterReceiver()")
        val broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(requireContext().applicationContext)
        broadcastManager.unregisterReceiver(receiver)
    }

    override fun onDestroy(): Unit {
        super.onDestroy()
        unregisterReceiver()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver()
        val context: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requireContext().createDeviceProtectedStorageContext()
        }
        else {
            requireContext()
        }
        sharedPreferences = context.getSharedPreferences(Application.SHARED_PREFERENCES, Context.MODE_PRIVATE)
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        /*
        if (savedInstanceState != null) {
            expandedViews = savedInstanceState.getIntegerArrayList(KEY_EXPANDED_VIEWS) as ArrayList<Int>
        }
         */
    }

    override fun onResume() {
        super.onResume()
        profileAdapter.notifyDataSetChanged()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Log.i("ProfilesListFragment", "onCreateView()")
        val view: View = inflater.inflate(R.layout.profiles, container, false)
        floatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton.setOnClickListener {
            val intent = EditProfileActivity.newIntent(requireContext(), null)
            startActivity(intent)
        }
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = profileAdapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("ProfilesListFragment", "onViewCreated()")
        viewModel.profileListLiveData.observe(viewLifecycleOwner,
                Observer<List<Profile>> { t ->
                    if (t != null) {
                        /*
                        if (t.isNotEmpty()) {
                            for (i in t) {
                                ids.add(i.id)
                            }
                        }
                         */
                        sharedViewModel.setValue(t.isEmpty())
                        updateUI(t)
                    }
                })
        viewModel.associatedEventsLiveData.observe(viewLifecycleOwner,
            Observer<List<ProfileAndEvent>?> { t ->
                if (t != null && t.isNotEmpty()) {
                    Log.i("ProfilesListFragment", "removing redundant alarms, amount of alarms: ${t.size}")
                    val alarmUtil: AlarmUtil = AlarmUtil(requireContext().applicationContext)
                    alarmUtil.cancelMultipleAlarms(t)
                }
            })
    }

    private fun updateUI(profiles: List<Profile>) {
        Log.i("ProfilesListFragment", "updateUI")
        if (profiles.isNotEmpty()) {
            view?.findViewById<TextView>(R.id.hint_profile)?.visibility = View.GONE
            view?.findViewById<ImageView>(R.id.hint_icon_scheduler)?.visibility = View.GONE
        }
        else {
            view?.findViewById<TextView>(R.id.hint_profile)?.visibility = View.VISIBLE
            view?.findViewById<ImageView>(R.id.hint_icon_scheduler)?.visibility = View.VISIBLE
        }
        profileAdapter.submitList(profiles)
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
                Log.i("ProfilesListFragment", "deleting profile and cancelling alarms")
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
            checkBox.isChecked = profile.isActive
            setupTextViews(profile)
            setCallbacks(profile)
            if (expandedViews.contains(position)) {
                expandView(false)
            }
        }

        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            if (buttonView != null && buttonView.isPressed) {
                val lastIndex: Int = viewModel.lastActiveProfileIndex
                val currentIndex: Int = absoluteAdapterPosition
                val currentProfile: Profile = profileAdapter.getProfile(currentIndex)
                if (isChecked) {
                    AudioUtil.applyAudioSettings(requireContext(), AudioUtil.getVolumeSettingsMapPair(currentProfile))
                    currentProfile.isActive = true
                    profileAdapter.notifyItemChanged(currentIndex)
                    viewModel.lastActiveProfileIndex = currentIndex
                    if (lastIndex != -1) {
                        profileAdapter.getProfile(lastIndex).isActive = false
                        profileAdapter.notifyItemChanged(lastIndex)
                    }
                }
                else {
                    currentProfile.isActive = false
                    if (lastIndex != -1) {
                        viewModel.lastActiveProfileIndex = -1
                    }
                }
            }
        }
    }

    private inner class ProfileAdapter : androidx.recyclerview.widget.ListAdapter<Profile, ProfileHolder>(object : DiffUtil.ItemCallback<Profile>() {

        override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem == newItem
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

        private const val LOG_TAG: String = "ProfilesListFragment"
        private const val PROFILE_LAYOUT = R.layout.item_view
    }
}