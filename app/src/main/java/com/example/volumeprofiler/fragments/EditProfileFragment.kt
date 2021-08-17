package com.example.volumeprofiler.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.media.AudioManager
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
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.EditProfileActivity
import com.example.volumeprofiler.fragments.dialogs.ProfileNameInputDialog
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.interfaces.ProfileNameInputDialogCallback
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*
import com.example.volumeprofiler.util.animations.AnimUtil.Companion.scaleAnimation
import com.google.android.material.appbar.CollapsingToolbarLayout
import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.res.ResourcesCompat
import com.example.volumeprofiler.activities.customContract.RingtonePickerContract
import com.example.volumeprofiler.models.Alarm

@SuppressLint("UseSwitchCompatOrMaterialCode")
class EditProfileFragment: Fragment(), ProfileNameInputDialogCallback {

    private val viewModel: EditProfileViewModel by activityViewModels()
    private var callbacks: EditProfileActivityCallbacks? = null
    private var alarmTriggers: List<AlarmTrigger>? = null
    private lateinit var interruptionFilterSwitch: Switch
    private lateinit var menuEditNameButton: ImageButton
    private lateinit var toolbarLayout: CollapsingToolbarLayout
    private lateinit var mediaSeekBar: SeekBar
    private lateinit var phoneSeekBar: SeekBar
    private lateinit var notificationSeekBar: SeekBar
    private lateinit var ringerSeekBar: SeekBar
    private lateinit var alarmSeekBar: SeekBar
    private lateinit var interruptionFilterLayout: RelativeLayout
    private lateinit var interruptionFilterPolicy: TextView
    private lateinit var phoneRingtoneTitle: TextView
    private lateinit var notificationSoundTitle: TextView
    private lateinit var alarmSoundTitle: TextView
    private lateinit var interruptionFilterPreferencesLayout: RelativeLayout
    private lateinit var silentModeLayout: RelativeLayout
    private lateinit var ringtoneActivityCallback: ActivityResultLauncher<Int>
    private var profileExists: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        val profile: Profile? = args?.getParcelable(EXTRA_PROFILE)
        if (profile != null) {
            profileExists = true
            viewModel.mutableProfile = profile
            val id: UUID = profile.id
            viewModel.setProfileID(id)
        } else {
            if (viewModel.mutableProfile == null) {
                viewModel.mutableProfile = Profile("Profile")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.create_profile_fragment, container, false)
        initializeViews(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setActionBar()
        setLiveDataObservers()
        updateUI()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = requireActivity() as EditProfileActivityCallbacks
        registerForActivityResult()
    }

    private fun setActionBar(): Unit {
        val toolbar: Toolbar = requireActivity().findViewById(R.id.toolbar)
        val activity: AppCompatActivity = requireActivity() as AppCompatActivity
        val shouldDisplayNavArrow: Boolean = activity.supportFragmentManager.backStackEntryCount > 0
        activity.setSupportActionBar(toolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(shouldDisplayNavArrow)
    }

    private fun setActionBarTitle(title: String): Unit {
        toolbarLayout.title = title
    }

    private fun changeMenuOptionVisibility(view: View, visible: Boolean) {
        scaleAnimation(view, visible)
    }

    private fun showTextInputDialog(): Unit {
        val title: String = viewModel.mutableProfile!!.title
        val fragment = ProfileNameInputDialog.newInstance(title).apply {
            this.setTargetFragment(this@EditProfileFragment, 1)
        }
        fragment.show(requireActivity().supportFragmentManager, null)
    }

    override fun onDetach() {
        callbacks = null
        super.onDetach()
    }

    override fun onTitleChanged(str: String) {
        viewModel.mutableProfile!!.title = str
        setActionBarTitle(str)
    }

    private fun initializeViews(view: View): Unit {
        val activity: Activity = requireActivity()

        val vibrateForCallsLayout: RelativeLayout = view.findViewById(R.id.vibrateForCallsLayout)
        val phoneRingtoneLayout: LinearLayout = view.findViewById(R.id.phoneRingtoneLayout)
        val notificationSoundLayout: LinearLayout = view.findViewById(R.id.notificationSoundLayout)
        val alarmSoundLayout: LinearLayout = view.findViewById(R.id.alarmSoundLayout)
        val silentModeSwitch: Switch = view.findViewById(R.id.silentModeSwitch)
        val vibrateForCallsSwitch: Switch = view.findViewById(R.id.vibrateForCallsSwitch)
        val editNameButton: FloatingActionButton = activity.findViewById(R.id.editNameButton)
        val menuSaveChangesButton: ImageButton = activity.findViewById(R.id.menuSaveChangesButton)
        if (profileExists) {
            menuSaveChangesButton.setImageDrawable(ResourcesCompat.getDrawable(resources, android.R.drawable.ic_menu_save, null))
        }
        else {
            menuSaveChangesButton.setImageDrawable(ResourcesCompat.getDrawable(resources, android.R.drawable.ic_menu_add, null))
        }
        menuEditNameButton = activity.findViewById(R.id.menuEditNameButton)
        toolbarLayout = activity.findViewById(R.id.toolbar_layout)
        interruptionFilterLayout = view.findViewById(R.id.doNotDisturbLayout)
        mediaSeekBar = view.findViewById(R.id.mediaSeekBar)
        phoneSeekBar = view.findViewById(R.id.phoneSeekBar)
        interruptionFilterPreferencesLayout = view.findViewById(R.id.doNotDisturbPreferencesLayout)
        interruptionFilterPolicy = view.findViewById(R.id.doNotDisturbState)
        notificationSeekBar = view.findViewById(R.id.notificationSeekBar)
        ringerSeekBar = view.findViewById(R.id.ringerSeekBar)
        alarmSeekBar = view.findViewById(R.id.alarmSeekBar)
        phoneRingtoneTitle = view.findViewById(R.id.currentPhoneRingtone)
        alarmSoundTitle = view.findViewById(R.id.currentAlarmSound)
        notificationSoundTitle = view.findViewById(R.id.currentNotificationSound)
        silentModeLayout = view.findViewById(R.id.SilentModeLayout)
        interruptionFilterSwitch = view.findViewById(R.id.doNotDisturbSwitch)

        val appBarLayout: AppBarLayout = activity.findViewById(R.id.app_bar)
        appBarLayout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {

            var isVisible: Boolean = false

            override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {

                if (Math.abs(verticalOffset) - appBarLayout!!.totalScrollRange == 0) {
                    if (!isVisible && this@EditProfileFragment.isVisible) {
                        isVisible = true
                        changeMenuOptionVisibility(menuEditNameButton, isVisible)
                    }
                } else if (isVisible) {
                    isVisible = false
                    changeMenuOptionVisibility(menuEditNameButton, isVisible)
                }
            }
        })
        val onClickListener: View.OnClickListener = View.OnClickListener {

            when (it.id) {

                R.id.SilentModeLayout -> {
                    if (silentModeSwitch.isChecked) {
                        silentModeSwitch.isChecked = false
                        viewModel.mutableProfile!!.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    }
                    else {
                        silentModeSwitch.isChecked = true
                        viewModel.mutableProfile!!.ringerMode = AudioManager.RINGER_MODE_SILENT
                    }
                }
                R.id.vibrateForCallsLayout -> {
                    if (vibrateForCallsSwitch.isChecked) {
                        vibrateForCallsSwitch.isChecked = false
                        viewModel.mutableProfile!!.isVibrateForCallsActive = 0
                    }
                    else {
                        vibrateForCallsSwitch.isChecked = true
                        viewModel.mutableProfile!!.isVibrateForCallsActive = 1
                    }
                }
                R.id.menuSaveChangesButton -> commitChanges()
                R.id.editNameButton, R.id.menuEditNameButton -> showTextInputDialog()
                R.id.phoneRingtoneLayout -> startRingtonePickerActivity(RingtoneManager.TYPE_RINGTONE)
                R.id.notificationSoundLayout -> startRingtonePickerActivity(RingtoneManager.TYPE_NOTIFICATION)
                R.id.alarmSoundLayout -> startRingtonePickerActivity(RingtoneManager.TYPE_ALARM)
                R.id.doNotDisturbLayout -> showPopupMenu(it)
                R.id.doNotDisturbPreferencesLayout -> callbacks?.onFragmentReplace(EditProfileActivity.DND_PREFERENCES_FRAGMENT)
            }
        }
        val onCheckedChangeListener: CompoundButton.OnCheckedChangeListener = CompoundButton.OnCheckedChangeListener {
            buttonView, isChecked ->

            if (buttonView.id == R.id.doNotDisturbSwitch) {
                if (isChecked) {
                    viewModel.mutableProfile!!.isInterruptionFilterActive = 1
                    changeInterruptionFilterLayout(true)
                    when (viewModel.mutableProfile!!.interruptionFilter) {

                        INTERRUPTION_FILTER_PRIORITY -> {
                            if (ringerSeekBar.progress == 0) {
                                silentModeLayoutTransition(true)
                            }
                            changeInterruptionFilterPrefsLayout(true)
                            changeSeekbarState(INTERRUPTION_FILTER_PRIORITY)
                        }
                        INTERRUPTION_FILTER_ALARMS -> {
                            silentModeLayoutTransition(false)
                            changeInterruptionFilterPrefsLayout(false)
                            changeSeekbarState(INTERRUPTION_FILTER_ALARMS)
                        }
                        INTERRUPTION_FILTER_NONE -> {
                            silentModeLayoutTransition(false)
                            changeInterruptionFilterPrefsLayout(false)
                            changeSeekbarState(INTERRUPTION_FILTER_NONE)
                        }
                    }
                }
                else {
                    viewModel.mutableProfile!!.isInterruptionFilterActive = 0
                    changeInterruptionFilterLayout(false)
                    changeInterruptionFilterPrefsLayout(false)
                    changeSeekbarState(INTERRUPTION_FILTER_ALL)
                }
            }
        }
        val seekBarChangeListener: SeekBar.OnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar != null && fromUser) {
                    if (progress < 1) {
                        if (seekBar.id == R.id.phoneSeekBar || seekBar.id == R.id.alarmSeekBar) {
                            seekBar.progress = 1
                        }
                    }
                    else {
                        seekBar.progress = progress
                    }
                    when (seekBar.id) {
                        R.id.mediaSeekBar -> viewModel.mutableProfile!!.mediaVolume = progress
                        R.id.phoneSeekBar -> viewModel.mutableProfile!!.callVolume = progress
                        R.id.notificationSeekBar -> viewModel.mutableProfile!!.notificationVolume = progress
                        R.id.ringerSeekBar -> viewModel.mutableProfile!!.ringVolume = progress
                        R.id.alarmSeekBar -> viewModel.mutableProfile!!.alarmVolume = progress
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Log.i(LOG_TAG, "onStartTrackingTouch")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar?.id == R.id.ringerSeekBar) {
                    silentModeLayoutTransition(seekBar.progress == 0)
                }
            }
        }
        if (menuSaveChangesButton.visibility != View.VISIBLE) {
            changeMenuOptionVisibility(menuSaveChangesButton, true)
        }
        menuSaveChangesButton.setOnClickListener(onClickListener)
        vibrateForCallsLayout.setOnClickListener(onClickListener)
        silentModeLayout.setOnClickListener(onClickListener)
        phoneRingtoneLayout.setOnClickListener(onClickListener)
        notificationSoundLayout.setOnClickListener(onClickListener)
        alarmSoundLayout.setOnClickListener(onClickListener)
        interruptionFilterLayout.setOnClickListener(onClickListener)
        interruptionFilterPreferencesLayout.setOnClickListener(onClickListener)

        interruptionFilterSwitch.setOnCheckedChangeListener(onCheckedChangeListener)
        menuEditNameButton.setOnClickListener(onClickListener)
        editNameButton.setOnClickListener(onClickListener)

        mediaSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        phoneSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        notificationSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        alarmSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        ringerSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)

        phoneSeekBar.progress = MIN_CALL_STREAM_VALUE
        alarmSeekBar.progress = MIN_ALARM_STREAM_VALUE
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

    private fun changeInterruptionFilterLayout(enable: Boolean): Unit {
        val doNotDisturbTitle: TextView = requireView().findViewById(R.id.doNotDisturbTitle)
        if (enable) {
            interruptionFilterLayout.isEnabled = true
            interruptionFilterPolicy.setTextColor(Color.parseColor(DEFAULT_TEXT_COLOR))
            doNotDisturbTitle.setTextColor(Color.BLACK)
        }
        else {
            interruptionFilterLayout.isEnabled = false
            interruptionFilterPolicy.setTextColor(Color.parseColor(DISABLED_STATE_COLOR))
            doNotDisturbTitle.setTextColor(Color.parseColor(DISABLED_STATE_COLOR))
        }
    }

    private fun changeInterruptionFilterPrefsLayout(enable: Boolean): Unit {
        val doNotDisturbRules: TextView = requireView().findViewById(R.id.doNotDisturbRules)
        val doNotDisturbPreferencesTitle: TextView = requireView().findViewById(R.id.doNotDisturbPreferencesTitle)
        if (enable) {
            interruptionFilterPreferencesLayout.isEnabled = true
            doNotDisturbPreferencesTitle.setTextColor(Color.BLACK)
            doNotDisturbRules.setTextColor(Color.parseColor(DEFAULT_TEXT_COLOR))
        }
        else {
            interruptionFilterPreferencesLayout.isEnabled = false
            doNotDisturbPreferencesTitle.setTextColor(Color.parseColor(DISABLED_STATE_COLOR))
            doNotDisturbRules.setTextColor(Color.parseColor(DISABLED_STATE_COLOR))
        }
    }

    private fun showPopupMenu(view: View): Unit {
        val popupMenu: PopupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.dnd_mode_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {

                R.id.priority_only -> {
                    viewModel.mutableProfile!!.interruptionFilter = INTERRUPTION_FILTER_PRIORITY
                    changeSeekbarState(INTERRUPTION_FILTER_PRIORITY)
                    changeInterruptionFilterPrefsLayout(true)
                    interruptionFilterPolicy.text = "Priority only"
                    if (ringerSeekBar.progress == 0) {
                        silentModeLayoutTransition(true)
                    }
                    true
                }
                R.id.alarms_only -> {
                    viewModel.mutableProfile!!.interruptionFilter = INTERRUPTION_FILTER_ALARMS
                    changeSeekbarState(INTERRUPTION_FILTER_ALARMS)
                    changeInterruptionFilterPrefsLayout(false)
                    interruptionFilterPolicy.text = "Alarms only"
                    silentModeLayoutTransition(false)
                    true
                }
                R.id.total_silence -> {
                    viewModel.mutableProfile!!.interruptionFilter = INTERRUPTION_FILTER_NONE
                    changeSeekbarState(INTERRUPTION_FILTER_NONE)
                    changeInterruptionFilterPrefsLayout(false)
                    interruptionFilterPolicy.text = "Total silence"
                    silentModeLayoutTransition(false)
                    true
                }
                else -> {
                    false
                }
            }
        }
        popupMenu.show()
    }

    private fun containsCategory(category: Int): Boolean {
        val list: List<Int> = viewModel.mutableProfile!!.priorityCategories
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                (category == PRIORITY_CATEGORY_ALARMS || category == PRIORITY_CATEGORY_MEDIA || category == PRIORITY_CATEGORY_SYSTEM)) {
            list.contains(category)
        }
        else {
            list.contains(category)
        }
    }

    private fun changeSeekbarState(mode: Int) {
        when (mode) {
            INTERRUPTION_FILTER_ALL -> {
                mediaSeekBar.isEnabled = true
                notificationSeekBar.isEnabled = true
                ringerSeekBar.isEnabled = true
                alarmSeekBar.isEnabled = true
                phoneSeekBar.isEnabled = true
            }
            INTERRUPTION_FILTER_PRIORITY -> {
                phoneSeekBar.isEnabled = true
                mediaSeekBar.isEnabled = containsCategory(PRIORITY_CATEGORY_MEDIA)
                notificationSeekBar.isEnabled = true
                ringerSeekBar.isEnabled = true
                alarmSeekBar.isEnabled = containsCategory(PRIORITY_CATEGORY_ALARMS)
            }
            INTERRUPTION_FILTER_ALARMS -> {
                phoneSeekBar.isEnabled = true
                notificationSeekBar.isEnabled = false
                ringerSeekBar.isEnabled = false
                mediaSeekBar.isEnabled = true
                alarmSeekBar.isEnabled = true
            }
            INTERRUPTION_FILTER_NONE -> {
                phoneSeekBar.isEnabled = false
                mediaSeekBar.isEnabled = false
                notificationSeekBar.isEnabled = false
                ringerSeekBar.isEnabled = false
                alarmSeekBar.isEnabled = false
            }
        }
    }

    private fun registerForActivityResult() {
        val contract: RingtonePickerContract = RingtonePickerContract()
        ringtoneActivityCallback = registerForActivityResult(contract) {
            if (it != null) {
                when (contract.ringtoneType) {
                    RingtoneManager.TYPE_RINGTONE -> {
                        viewModel.mutableProfile!!.phoneRingtoneUri = it
                        phoneRingtoneTitle.text = getRingtoneTitle(it)
                    }
                    RingtoneManager.TYPE_NOTIFICATION -> {
                        viewModel.mutableProfile!!.notificationSoundUri = it
                        notificationSoundTitle.text = getRingtoneTitle(it)
                    }
                    RingtoneManager.TYPE_ALARM -> {
                        viewModel.mutableProfile!!.alarmSoundUri = it
                        alarmSoundTitle.text = getRingtoneTitle(it)
                    }
                    else -> Log.i("EditProfileFragment", "unknown ringtone type")
                }
            }
        }
    }

    private fun startRingtonePickerActivity(type: Int): Unit {
        ringtoneActivityCallback.launch(type)
    }

    private fun setLiveDataObservers(): Unit {
        viewModel.alarmTriggerLiveData.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it != null) {
                alarmTriggers = it
            }
        })
    }

    private fun setInterruptionFilterTitle(): String =
            when (viewModel.mutableProfile!!.interruptionFilter) {
                INTERRUPTION_FILTER_PRIORITY -> "Priority only"
                INTERRUPTION_FILTER_ALARMS -> "Alarms only"
                INTERRUPTION_FILTER_NONE -> "Total silence"
                else -> "INTERRUPTION_FILTER_UNKNOWN"
            }

    private fun updateInterruptionFilterViews(): Unit {
        val interruptionFilter: Int = viewModel.mutableProfile!!.interruptionFilter
        val isInterruptionFilterActive: Boolean = viewModel.mutableProfile!!.isInterruptionFilterActive == 1
        interruptionFilterPolicy.text = setInterruptionFilterTitle()
        interruptionFilterSwitch.isChecked = isInterruptionFilterActive
        if (isInterruptionFilterActive) {
            changeInterruptionFilterLayout(true)
            changeSeekbarState(interruptionFilter)
            if (interruptionFilter == INTERRUPTION_FILTER_PRIORITY) {
                changeInterruptionFilterPrefsLayout(true)
            }
        }
        else {
            changeInterruptionFilterLayout(false)
            changeSeekbarState(INTERRUPTION_FILTER_ALL)
            changeInterruptionFilterPrefsLayout(false)
        }
    }

    private fun updateSeekBars(): Unit {
        mediaSeekBar.progress = viewModel.mutableProfile!!.mediaVolume
        phoneSeekBar.progress = viewModel.mutableProfile!!.callVolume
        notificationSeekBar.progress = viewModel.mutableProfile!!.notificationVolume
        ringerSeekBar.progress = viewModel.mutableProfile!!.ringVolume
        alarmSeekBar.progress = viewModel.mutableProfile!!.alarmVolume
    }

    private fun updateUI(): Unit {
        setActionBarTitle(viewModel.mutableProfile!!.title)
        updateSeekBars()
        updateInterruptionFilterViews()
        if (arguments?.getParcelable<Profile>(EXTRA_PROFILE) != null) {
            phoneRingtoneTitle.text = getRingtoneTitle(viewModel.mutableProfile!!.phoneRingtoneUri)
            notificationSoundTitle.text = getRingtoneTitle(viewModel.mutableProfile!!.notificationSoundUri)
            alarmSoundTitle.text = getRingtoneTitle(viewModel.mutableProfile!!.alarmSoundUri)
        }
        else {
            setDefaultRingtoneUris()
            phoneRingtoneTitle.text = getRingtoneTitle(viewModel.mutableProfile!!.phoneRingtoneUri)
            alarmSoundTitle.text = getRingtoneTitle(viewModel.mutableProfile!!.alarmSoundUri)
            notificationSoundTitle.text = getRingtoneTitle(viewModel.mutableProfile!!.notificationSoundUri)
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
            profileUtil.applyAudioSettings(viewModel.mutableProfile!!)
        }
    }

    private fun commitChanges(): Unit {
        val profile: Profile = viewModel.mutableProfile!!
        if (!alarmTriggers.isNullOrEmpty()) {
            scheduleAlarms()
        }
        applyAudioSettings()
        if (!profileExists) {
            viewModel.addProfile(profile)
        }
        else {
            viewModel.updateProfile(profile)
        }
        requireActivity().finish()
    }

    private fun setAlarm(alarmTrigger: AlarmTrigger): Unit {
        val alarm: Alarm = alarmTrigger.alarm
        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
        alarmUtil.setAlarm(alarm, viewModel.mutableProfile!!, false)
    }

    private fun scheduleAlarms(): Unit {
        for (i in alarmTriggers!!) {
            setAlarm(i)
        }
    }

    private fun silentModeLayoutTransition(visible: Boolean): Unit {
        TransitionManager.beginDelayedTransition(view?.findViewById(R.id.root), AutoTransition())
        if (visible) silentModeLayout.visibility = View.VISIBLE else silentModeLayout.visibility = View.GONE
    }

    companion object {

        private const val DEFAULT_TEXT_COLOR: String = "#757575"
        private const val DISABLED_STATE_COLOR: String = "#bababa"
        private const val EXTRA_PROFILE = "profile"
        private const val LOG_TAG = "EditProfileFragment"
        private const val MIN_ALARM_STREAM_VALUE: Int = 1
        private const val MIN_CALL_STREAM_VALUE: Int = 1

        fun newInstance(profile: Profile?): EditProfileFragment {
            val args: Bundle = Bundle()
            args.putParcelable(EXTRA_PROFILE, profile)
            return EditProfileFragment().apply {
                if (profile != null) {
                    this.arguments = args
                }
            }
        }
    }
}
