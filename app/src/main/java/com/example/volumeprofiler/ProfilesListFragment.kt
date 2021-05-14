package com.example.volumeprofiler

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.util.AlarmUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfilesListFragment: Fragment(), AnimImplementation, AlarmReceiver.Callbacks {

    private lateinit var floatingButtonAction: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private val profileAdapter: ProfileAdapter = ProfileAdapter()
    private var expandedViews: ArrayList<Int> = arrayListOf()
    private val model: ProfileListViewModel by viewModels()
    private lateinit var audioManager: AudioManager
    var lastCheckedIndex: Int = -1

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntegerArrayList(KEY_EXPANDED_VIEWS, expandedViews)
        outState.putInt(KEY_LAST_CHECKED_INDEX, lastCheckedIndex)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (savedInstanceState != null) {
            expandedViews = savedInstanceState.getIntegerArrayList(KEY_EXPANDED_VIEWS) as ArrayList<Int>
            lastCheckedIndex = savedInstanceState.getInt(KEY_LAST_CHECKED_INDEX)
        }
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
        val view: View = inflater.inflate(R.layout.profiles, container, false)
        floatingButtonAction = view.findViewById(R.id.fab)
        floatingButtonAction.setOnClickListener {
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
        model.profileListLiveData.observe(viewLifecycleOwner,
                Observer<List<Profile>> { t ->
                    if (t != null) {
                        updateUI(t)
                    }
                })
        model.associatedEventsLiveData.observe(viewLifecycleOwner,
            Observer<List<ProfileAndEvent>?> { t ->
                if (t != null) {
                    Log.i("ProfilesListFragment", "removing redundant alarms, amount of alarms: ${t.size}")
                    val alarmUtil: AlarmUtil = AlarmUtil(requireContext().applicationContext)
                    alarmUtil.cancelMultipleAlarms(t)
                }
            })
    }

    private fun updateUI(profiles: List<Profile>) {
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

        private fun setupTextViews(profile: Profile): Unit {
            checkBox.text = profile.title
            mediaVolValue.text = "${profile.mediaVolume}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}"
            callVolValue.text = "${profile.callVolume}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)}"
            alarmVolValue.text = "${profile.alarmVolume}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)}"
            notificationVolValue.text = "${profile.notificationVolume}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)}"
            ringerVolValue.text = "${profile.ringVolume}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)}"
        }

        private fun setupCallbacks(profile: Profile): Unit {
            expandImageView.setOnClickListener {
                TransitionManager.beginDelayedTransition(recyclerView, AutoTransition())

                if (expandableView.visibility == View.GONE) {
                    expandableView.visibility = View.VISIBLE
                    expandImageView.animate().rotation(180.0f).start()
                    expandedViews.add(absoluteAdapterPosition)
                }
                else {
                    expandableView.visibility = View.GONE
                    expandImageView.animate().rotation(0.0f).start()
                    expandedViews.remove(absoluteAdapterPosition)
                }
            }
            editTextView.setOnClickListener { startActivity(EditProfileActivity.newIntent(requireContext(), profile.id)) }
            removeTextView.setOnClickListener {
                val position: Int = absoluteAdapterPosition
                if (position == lastCheckedIndex) {
                    lastCheckedIndex = -1
                }
                expandedViews.remove(position)
                profileAdapter.getProfile(position).let {
                    Log.i("ProfilesListFragment", "deleting profile and cancelling alarms")
                    scaleDownAnimation(itemView)
                    model.removeProfile(it)
                }
            }
        }

        fun bindProfile(profile: Profile, position: Int): Unit {
            checkBox.isChecked = profile.isActive
            setupTextViews(profile)
            setupCallbacks(profile)
            if (expandedViews.contains(position)) {
                expandableView.visibility = View.VISIBLE
                expandImageView.rotation = 180.0f
            }
        }

        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            if (buttonView?.isPressed!!) {
                val lastIndex = lastCheckedIndex
                if (isChecked && lastIndex != -1) {
                    profileAdapter.getProfile(lastIndex).isActive = false
                    profileAdapter.notifyItemChanged(lastIndex)
                    profileAdapter.getProfile(absoluteAdapterPosition).isActive = isChecked
                    lastCheckedIndex = absoluteAdapterPosition
                    Log.d("ProfilesListFragment", "select single view and deselect others $lastCheckedIndex")
                }
                else if (!isChecked && lastIndex != -1) {
                    profileAdapter.getProfile(absoluteAdapterPosition).isActive = isChecked
                    lastCheckedIndex = -1
                    Log.d("ProfilesListFragment", "deselect single view $lastCheckedIndex")
                }
                else if (isChecked && lastIndex == -1) {
                    profileAdapter.getProfile(absoluteAdapterPosition).isActive = isChecked
                    lastCheckedIndex = absoluteAdapterPosition
                    Log.d("ProfilesListFragment", "select single view $lastCheckedIndex")
                }
                checkBox.isChecked = isChecked
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

        private fun startScaleUpAnimation(view: View) {
            val anim = ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            anim.duration = 400
            view.startAnimation(anim)
        }

        override fun onBindViewHolder(holder: ProfileHolder, position: Int) {
            val profile = getItem(position)
            if (holder.absoluteAdapterPosition > lastPosition) {
                lastPosition = position
                startScaleUpAnimation(holder.itemView)
            }
            holder.bindProfile(profile, position)
        }
    }

    companion object {

        private const val KEY_LAST_CHECKED_INDEX = "Last_checked_index"
        private const val PROFILE_LAYOUT = R.layout.item_view
        private const val KEY_EXPANDED_VIEWS = "Expanded_views"
    }

    override fun onProfileActivation() {
        TODO("Not yet implemented")
    }
}