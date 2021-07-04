package com.example.volumeprofiler.fragments

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.EditProfileActivity
import com.example.volumeprofiler.fragments.dialogs.ProfileNameInputDialog
import com.example.volumeprofiler.interfaces.ApplyChangesDialogCallbacks
import com.example.volumeprofiler.interfaces.FragmentTransition
import com.example.volumeprofiler.interfaces.ProfileNameInputDialogCallback
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import java.util.*

@SuppressLint("UseSwitchCompatOrMaterialCode")
class EditProfileFragment: Fragment(), ApplyChangesDialogCallbacks, ProfileNameInputDialogCallback {

    private val viewModel: EditProfileViewModel by activityViewModels()

    private var profileName: TextView? = null
    private var mediaSeekBar: SeekBar? = null
    private var phoneSeekBar: SeekBar? = null
    private var notificationSeekBar: SeekBar? = null
    private var ringerSeekBar: SeekBar? = null
    private var alarmSeekBar: SeekBar? = null
    private var doNotDisturbLayout: RelativeLayout? = null
    private var doNotDisturbState: TextView? = null
    private var phoneRingtoneTitle: TextView? = null
    private var notificationSoundTitle: TextView? = null
    private var alarmSoundTitle: TextView? = null
    private var doNotDisturbPreferencesLayout: RelativeLayout? = null
    private var silentModeLayout: RelativeLayout? = null
    private var callbacks: FragmentTransition? = null
    private var profilesAndEvents: List<ProfileAndEvent>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val supportActionBar = (activity as AppCompatActivity).supportActionBar
        if (arguments?.getSerializable(EXTRA_UUID) != null) {
            supportActionBar?.title = ACTION_BAR_TITLE_EDIT
            viewModel.setProfile(arguments?.get(EXTRA_UUID) as UUID)
        } else {
            if (viewModel.mutableProfile == null) {
                viewModel.mutableProfile = Profile("Profile")
                viewModel.changesMade = true
            }
            supportActionBar?.title = ACTION_BAR_TITLE_CREATE
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = requireActivity() as FragmentTransition
    }

    override fun onDetach() {
        callbacks = null
        super.onDetach()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.create_profile_fragment, container, false)
        initializeViews(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(LOG_TAG, "onViewCreated")
        setLiveDataObservers()
        updateUI()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        activity?.menuInflater?.inflate(R.menu.action_menu_save_changes, menu)
        val drawable: Drawable?
        val item: MenuItem = menu.findItem(R.id.saveChangesButton)
        if (arguments?.getSerializable(EXTRA_UUID) != null) {
            drawable = ResourcesCompat.getDrawable(resources, android.R.drawable.ic_menu_save, null)
            item.icon = drawable
        } else {
            drawable = ResourcesCompat.getDrawable(resources, android.R.drawable.ic_menu_add, null)
            item.icon = drawable
        }
    }

    override fun onDestroyView() {
        clearReferences()
        super.onDestroyView()
    }

    private fun clearReferences(): Unit {
        profileName = null
        mediaSeekBar = null
        phoneSeekBar = null
        notificationSeekBar = null
        ringerSeekBar = null
        alarmSeekBar = null
        doNotDisturbLayout = null
        doNotDisturbState = null
        phoneRingtoneTitle = null
        notificationSoundTitle = null
        alarmSoundTitle = null
        doNotDisturbPreferencesLayout = null
        silentModeLayout = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if (item.itemId == R.id.saveChangesButton) {
            saveChanges()
        }
        return false
    }

    override fun onApply() {

    }

    override fun onApply(str: String) {
        viewModel.mutableProfile!!.title = str
        profileName?.text = str
    }

    override fun onDismiss() {

    }

    private fun initializeViews(view: View): Unit {
        val phoneRingtoneLayout: LinearLayout = view.findViewById(R.id.phoneRingtoneLayout)
        val notificationSoundLayout: LinearLayout = view.findViewById(R.id.notificationSoundLayout)
        val alarmSoundLayout: LinearLayout = view.findViewById(R.id.alarmSoundLayout)
        val silentModeSwitch: Switch = view.findViewById(R.id.silentModeSwitch)
        val vibrateForCallsSwitch: Switch = view.findViewById(R.id.vibrateForCallsSwitch)
        val doNotDisturbSwitch: Switch = view.findViewById(R.id.doNotDisturbSwitch)

        doNotDisturbLayout = view.findViewById(R.id.doNotDisturbLayout)
        profileName = view.findViewById(R.id.profileName)
        mediaSeekBar = view.findViewById(R.id.mediaSeekBar)
        phoneSeekBar = view.findViewById(R.id.phoneSeekBar)
        doNotDisturbPreferencesLayout = view.findViewById(R.id.doNotDisturbPreferencesLayout)
        doNotDisturbState = view.findViewById(R.id.doNotDisturbState)
        notificationSeekBar = view.findViewById(R.id.notificationSeekBar)
        ringerSeekBar = view.findViewById(R.id.ringerSeekBar)
        alarmSeekBar = view.findViewById(R.id.alarmSeekBar)
        phoneRingtoneTitle = view.findViewById(R.id.currentPhoneRingtone)
        alarmSoundTitle = view.findViewById(R.id.currentAlarmSound)
        notificationSoundTitle = view.findViewById(R.id.currentNotificationSound)
        silentModeLayout = view.findViewById(R.id.SilentModeLayout)

        val onClickListener: View.OnClickListener = View.OnClickListener {

            when (it.id) {

                R.id.profileName -> {
                    val fragment = ProfileNameInputDialog.newInstance(profileName!!.text.toString()).apply {
                        this.setTargetFragment(this@EditProfileFragment, 1)
                    }
                    activity?.supportFragmentManager?.let { it1 -> fragment.show(it1, null) }
                }
                R.id.phoneRingtoneLayout -> {
                    val intent: Intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        this.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                    }
                    startActivityForResult(intent, REQUEST_CODE_RINGTONE)
                }
                R.id.notificationSoundLayout -> {
                    val intent: Intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        this.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                    }
                    startActivityForResult(intent, REQUEST_CODE_NOTIFICATION)
                }
                R.id.alarmSoundLayout -> {
                    val intent: Intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        this.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    }
                    startActivityForResult(intent, REQUEST_CODE_ALARM)
                }
                R.id.doNotDisturbLayout -> {
                    showPopupMenu(it)
                }
                R.id.doNotDisturbPreferencesLayout -> {
                    callbacks?.onFragmentReplace(EditProfileActivity.DND_PREFERENCES_FRAGMENT)
                }
            }
        }

        val onCheckedChangeListener: CompoundButton.OnCheckedChangeListener = CompoundButton.OnCheckedChangeListener {
            buttonView, isChecked ->

            when (buttonView.id) {

                R.id.silentModeSwitch-> {
                    if (isChecked) {
                        viewModel.mutableProfile!!.silentModeActive = 1
                    }
                    else {
                        viewModel.mutableProfile!!.silentModeActive = 0
                    }
                }
                R.id.vibrateForCallsSwitch -> {
                    if (isChecked) {
                        viewModel.mutableProfile!!.vibrateForCalls = 1
                    }
                    else {
                        viewModel.mutableProfile!!.vibrateForCalls = 0
                    }
                }
                R.id.doNotDisturbSwitch -> {
                    if (isChecked) {
                        viewModel.mutableProfile!!.isDndActive = 1
                        changeDndLayout(true)
                        when (viewModel.mutableProfile!!.dndMode) {

                            DND_MODE_PRIORITY_ONLY -> {
                                changeDndPreferencesLayout(true)
                                changeSeekbarState(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                            }
                            DND_MODE_ALARMS_ONLY -> {
                                changeDndPreferencesLayout(false)
                                changeSeekbarState(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                            }
                            DND_MODE_TOTAL_SILENCE -> {
                                changeDndPreferencesLayout(false)
                                changeSeekbarState(NotificationManager.INTERRUPTION_FILTER_NONE)
                            }
                        }
                    }
                    else {
                        viewModel.mutableProfile!!.isDndActive = 0
                        changeDndLayout(false)
                        changeDndPreferencesLayout(false)
                        changeSeekbarState(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    }
                }
            }
        }

        val seekBarChangeListener: SeekBar.OnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar != null && fromUser) {
                    viewModel.changesMade = true
                    if (progress < 1) {
                        if (seekBar.id == R.id.phoneSeekBar || seekBar.id == R.id.alarmSeekBar) {
                            seekBar.progress = 1
                        }
                    }
                    else {
                        seekBar.progress = progress
                    }
                    when (seekBar.id) {

                        R.id.mediaSeekBar -> {
                            viewModel.mutableProfile!!.mediaVolume = progress
                        }

                        R.id.phoneSeekBar -> {
                            viewModel.mutableProfile!!.callVolume = progress
                        }

                        R.id.notificationSeekBar -> {
                            viewModel.mutableProfile!!.notificationVolume = progress
                        }

                        R.id.ringerSeekBar -> {
                            viewModel.mutableProfile!!.ringVolume = progress
                        }

                        R.id.alarmSeekBar -> {
                            viewModel.mutableProfile!!.alarmVolume = progress
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Log.i(LOG_TAG, "onStartTrackingTouch")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar?.id == R.id.ringerSeekBar) {
                    silentModeLayoutTransition()
                }
            }
        }

        profileName?.setOnClickListener(onClickListener)
        phoneRingtoneLayout.setOnClickListener(onClickListener)
        notificationSoundLayout.setOnClickListener(onClickListener)
        alarmSoundLayout.setOnClickListener(onClickListener)
        doNotDisturbLayout?.setOnClickListener(onClickListener)
        doNotDisturbPreferencesLayout?.setOnClickListener(onClickListener)

        silentModeSwitch.setOnCheckedChangeListener(onCheckedChangeListener)
        vibrateForCallsSwitch.setOnCheckedChangeListener(onCheckedChangeListener)
        doNotDisturbSwitch.setOnCheckedChangeListener(onCheckedChangeListener)

        mediaSeekBar?.setOnSeekBarChangeListener(seekBarChangeListener)
        phoneSeekBar?.setOnSeekBarChangeListener(seekBarChangeListener)
        notificationSeekBar?.setOnSeekBarChangeListener(seekBarChangeListener)
        alarmSeekBar?.setOnSeekBarChangeListener(seekBarChangeListener)
        ringerSeekBar?.setOnSeekBarChangeListener(seekBarChangeListener)

        phoneSeekBar?.progress = MIN_CALL_STREAM_VALUE
        alarmSeekBar?.progress = MIN_ALARM_STREAM_VALUE
    }

    private fun setDefaultRingtoneUris(): Unit {
        when (Uri.EMPTY) {
            viewModel.mutableProfile!!.phoneRingtoneUri -> {
                viewModel.mutableProfile!!.phoneRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(
                        context, RingtoneManager.TYPE_RINGTONE
                )
            }
            viewModel.mutableProfile!!.alarmSoundUri -> {
                viewModel.mutableProfile!!.alarmSoundUri = RingtoneManager.getActualDefaultRingtoneUri(
                        context, RingtoneManager.TYPE_ALARM
                )
            }
            viewModel.mutableProfile!!.notificationSoundUri -> {
                viewModel.mutableProfile!!.notificationSoundUri = RingtoneManager.getActualDefaultRingtoneUri(
                        context, RingtoneManager.TYPE_NOTIFICATION
                )
            }
        }
    }

    private fun changeDndLayout(enable: Boolean): Unit {
        val doNotDisturbTitle: TextView? = view?.findViewById(R.id.doNotDisturbTitle)
        if (enable) {
            doNotDisturbLayout?.isEnabled = true
            doNotDisturbState?.setTextColor(Color.parseColor(DEFAULT_TEXT_COLOR))
            doNotDisturbTitle?.setTextColor(Color.BLACK)
        }
        else {
            doNotDisturbLayout?.isEnabled = false
            doNotDisturbState?.setTextColor(Color.parseColor(DISABLED_STATE_COLOR))
            doNotDisturbTitle?.setTextColor(Color.parseColor(DISABLED_STATE_COLOR))
        }
    }

    private fun changeDndPreferencesLayout(enable: Boolean): Unit {
        val doNotDisturbRules: TextView? = view?.findViewById(R.id.doNotDisturbRules)
        val doNotDisturbPreferencesTitle: TextView? = view?.findViewById(R.id.doNotDisturbPreferencesTitle)

        if (enable) {
            doNotDisturbPreferencesLayout?.isEnabled = true
            doNotDisturbPreferencesTitle?.setTextColor(Color.BLACK)
            doNotDisturbRules?.setTextColor(Color.parseColor(DEFAULT_TEXT_COLOR))
        }
        else {
            doNotDisturbPreferencesLayout?.isEnabled = false
            doNotDisturbPreferencesTitle?.setTextColor(Color.parseColor(DISABLED_STATE_COLOR))
            doNotDisturbRules?.setTextColor(Color.parseColor(DISABLED_STATE_COLOR))
        }
    }

    private fun showPopupMenu(view: View): Unit {
        val popupMenu: PopupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.dnd_mode_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {

                R.id.priority_only -> {
                    viewModel.mutableProfile!!.dndMode = DND_MODE_PRIORITY_ONLY
                    changeSeekbarState(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    changeDndPreferencesLayout(true)
                    doNotDisturbState?.text = "Priority only"
                    true
                }
                R.id.alarms_only -> {
                    viewModel.mutableProfile!!.dndMode = DND_MODE_ALARMS_ONLY
                    changeSeekbarState(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                    changeDndPreferencesLayout(false)
                    doNotDisturbState?.text = "Alarms only"
                    true
                }
                R.id.total_silence -> {
                    viewModel.mutableProfile!!.dndMode = DND_MODE_TOTAL_SILENCE
                    changeSeekbarState(NotificationManager.INTERRUPTION_FILTER_NONE)
                    changeDndPreferencesLayout(false)
                    doNotDisturbState?.text = "Total silence"
                    true
                }
                else -> {
                    false
                }
            }
        }
        popupMenu.show()
    }

    private fun changeSeekbarState(mode: Int) {
        when (mode) {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> {
                mediaSeekBar?.isEnabled = true
                notificationSeekBar?.isEnabled = true
                ringerSeekBar?.isEnabled = true
                alarmSeekBar?.isEnabled = true
            }
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> {
                notificationSeekBar?.isEnabled = false
                ringerSeekBar?.isEnabled = false
                mediaSeekBar?.isEnabled = true
                alarmSeekBar?.isEnabled = true
            }
            NotificationManager.INTERRUPTION_FILTER_NONE -> {
                mediaSeekBar?.isEnabled = false
                notificationSeekBar?.isEnabled = false
                ringerSeekBar?.isEnabled = false
                alarmSeekBar?.isEnabled = false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            val uri: Uri = data.extras?.getParcelable(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)!!
            val title: String? = getRingtoneTitle(uri)
            when (requestCode) {

                REQUEST_CODE_RINGTONE -> {
                    viewModel.mutableProfile!!.phoneRingtoneUri = uri
                    phoneRingtoneTitle?.text = title
                }
                REQUEST_CODE_NOTIFICATION -> {
                    viewModel.mutableProfile!!.notificationSoundUri = uri
                    notificationSoundTitle?.text = title
                }
                REQUEST_CODE_ALARM -> {
                    viewModel.mutableProfile!!.alarmSoundUri = uri
                    alarmSoundTitle?.text = title
                }
            }
        }
    }

    private fun setLiveDataObservers(): Unit {
        viewModel.profileLiveData.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it != null) {
                if (viewModel.mutableProfile == null) {
                    viewModel.mutableProfile = it
                }
                updateUI()
            }
        })
        viewModel.profileAndEventLiveData.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it != null) {
                profilesAndEvents = it
            }
        })
    }

    private fun updateUI() {
        val isDndActive: Boolean = viewModel.mutableProfile!!.isDndActive == 1

        profileName?.text = viewModel.mutableProfile!!.title
        mediaSeekBar?.progress = viewModel.mutableProfile!!.mediaVolume
        phoneSeekBar?.progress = viewModel.mutableProfile!!.callVolume
        notificationSeekBar?.progress = viewModel.mutableProfile!!.notificationVolume
        ringerSeekBar?.progress = viewModel.mutableProfile!!.ringVolume
        alarmSeekBar?.progress = viewModel.mutableProfile!!.alarmVolume
        doNotDisturbLayout?.isEnabled = isDndActive

        changeDndLayout(isDndActive)
        val isPriorityOnly: Boolean = isDndActive && viewModel.mutableProfile!!.dndMode == DND_MODE_PRIORITY_ONLY
        changeDndPreferencesLayout(isPriorityOnly)

        if (arguments?.getSerializable(EXTRA_UUID) != null) {
            phoneRingtoneTitle?.text = getRingtoneTitle(viewModel.mutableProfile!!.phoneRingtoneUri)
            notificationSoundTitle?.text = getRingtoneTitle(viewModel.mutableProfile!!.notificationSoundUri)
            alarmSoundTitle?.text = getRingtoneTitle(viewModel.mutableProfile!!.alarmSoundUri)
        }
        else {
            setDefaultRingtoneUris()
            phoneRingtoneTitle?.text = getRingtoneTitle(viewModel.mutableProfile!!.phoneRingtoneUri)
            alarmSoundTitle?.text = getRingtoneTitle(viewModel.mutableProfile!!.alarmSoundUri)
            notificationSoundTitle?.text = getRingtoneTitle(viewModel.mutableProfile!!.notificationSoundUri)
        }
    }

    private fun getRingtoneTitle(uri: Uri): String {
        val contentResolver: ContentResolver = requireContext().contentResolver
        val projection: Array<String> = arrayOf(MediaStore.MediaColumns.TITLE)
        var title: String = ""
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                title = cursor.getString(0)
                cursor.close()
            }
        }
        return title
    }

    private fun applyAudioSettings(): Unit {
        val sharedPreferencesUtil = SharedPreferencesUtil.getInstance()
        if (sharedPreferencesUtil.getActiveProfileId()
                == viewModel.mutableProfile!!.id.toString()) {
            val profileUtil: ProfileUtil = ProfileUtil.getInstance()
            val settingsPair = ProfileUtil.getVolumeSettingsMapPair(viewModel.mutableProfile!!)
            profileUtil.applyAudioSettings(settingsPair.first, settingsPair.second, viewModel.mutableProfile!!.id, viewModel.mutableProfile!!.title)
        }
    }

    private fun saveChanges(): Unit {
        val profile: Profile = viewModel.mutableProfile!!
        profile.title = profileName?.text.toString()
        if (profilesAndEvents != null) {
            scheduleAlarms()
        }
        applyAudioSettings()
        if (arguments?.getSerializable(EXTRA_UUID) == null) {
            viewModel.addProfile(profile)
        }
        else {
            viewModel.updateProfile(profile)
        }
    }

    private fun setAlarm(event: Event, profile: Profile): Unit {
        val eventOccurrences: Array<Int> = event.workingDays
                .split("")
                .slice(1..event.workingDays.length)
                .map { it.toInt() }.toTypedArray()
        val volumeSettingsMap: Pair<Map<Int, Int>, Map<String, Int>> = ProfileUtil.getVolumeSettingsMapPair(profile)
        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
        alarmUtil.setAlarm(volumeSettingsMap, eventOccurrences,
                event.localDateTime, event.id, false, profile.id, profile.title)
    }

    private fun scheduleAlarms(): Unit {
        val profilesAndEvents = profilesAndEvents as List<ProfileAndEvent>
        for (i in profilesAndEvents) {
            val event: Event = i.event
            setAlarm(event, viewModel.mutableProfile!!)
        }
    }

    private fun silentModeLayoutTransition(): Unit {
        Log.i(LOG_TAG, "silentModeLayoutTransition()")
        TransitionManager.beginDelayedTransition(view?.findViewById(R.id.root), AutoTransition())
        if (ringerSeekBar!!.progress > 0) {
            silentModeLayout?.visibility = View.GONE
        }
        else {
            silentModeLayout?.visibility = View.VISIBLE
        }
    }
    

    companion object {

        private const val DEFAULT_TEXT_COLOR: String = "#757575"
        private const val DISABLED_STATE_COLOR: String = "#bababa"

        private const val EXTRA_UUID = "uuid"
        private const val LOG_TAG = "EditProfileFragment"
        private const val REQUEST_CODE_RINGTONE: Int = 1
        private const val REQUEST_CODE_NOTIFICATION: Int = 2
        private const val REQUEST_CODE_ALARM: Int = 4
        private const val ACTION_BAR_TITLE_CREATE: String = "Create profile"
        private const val ACTION_BAR_TITLE_EDIT: String = "Edit profile"
        private const val MIN_ALARM_STREAM_VALUE: Int = 1
        private const val MIN_CALL_STREAM_VALUE: Int = 1

        private const val DND_MODE_PRIORITY_ONLY: Int = 6
        private const val DND_MODE_ALARMS_ONLY: Int = 7
        private const val DND_MODE_TOTAL_SILENCE: Int = 8

        fun newInstance(profileId: UUID?): EditProfileFragment {
            val args: Bundle = Bundle()
            args.putSerializable(EXTRA_UUID, profileId)
            return EditProfileFragment().apply {
                if (profileId != null) {
                    this.arguments = args
                }
            }
        }
    }
}
