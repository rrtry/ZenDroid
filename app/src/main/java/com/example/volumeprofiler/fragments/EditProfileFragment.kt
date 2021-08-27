package com.example.volumeprofiler.fragments

import android.annotation.SuppressLint
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
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import com.google.android.material.appbar.AppBarLayout
import java.util.*
import com.example.volumeprofiler.util.animations.AnimUtil.Companion.scaleAnimation
import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.res.ResourcesCompat
import com.example.volumeprofiler.activities.customContract.RingtonePickerContract
import com.example.volumeprofiler.databinding.CreateProfileActivityBinding
import com.example.volumeprofiler.databinding.CreateProfileFragmentBinding
import kotlin.math.abs

@SuppressLint("UseSwitchCompatOrMaterialCode")
class EditProfileFragment: Fragment(), ProfileNameInputDialogCallback {

    private val viewModel: EditProfileViewModel by activityViewModels()
    private var callbacks: EditProfileActivityCallbacks? = null
    private var alarms: List<AlarmTrigger>? = null
    private lateinit var ringtoneActivityCallback: ActivityResultLauncher<Int>
    private var passedExtras: Boolean = false

    private var _binding: CreateProfileFragmentBinding? = null
    private val binding: CreateProfileFragmentBinding get() = _binding!!
    private var _activityBinding: CreateProfileActivityBinding? = null
    private val activityBinding: CreateProfileActivityBinding get() = _activityBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile: Profile? = arguments?.getParcelable(EXTRA_PROFILE)
        if (profile != null) {
            passedExtras = true
            loadArgs(profile)
        } else if (viewModel.mutableProfile == null) {
            createEmptyProfile()
        }
    }

    private fun loadArgs(model: Profile): Unit {
        if (viewModel.mutableProfile == null) {
            viewModel.mutableProfile = model
            viewModel.setProfileID(model.id)
        }
    }

    private fun createEmptyProfile(): Unit {
        viewModel.mutableProfile = Profile("Profile")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CreateProfileFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setActionBar()
        initializeViews(view)
        setObservers()
        updateUI()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = requireActivity() as EditProfileActivityCallbacks
        _activityBinding = callbacks?.getBinding()
        registerForActivityResult()
    }

    override fun onDetach() {
        _activityBinding = null
        callbacks = null
        super.onDetach()
    }

    private fun setActionBar(): Unit {
        val toolbar: Toolbar = requireActivity().findViewById(R.id.toolbar)
        val activity: AppCompatActivity = requireActivity() as AppCompatActivity
        val shouldDisplayNavArrow: Boolean = activity.supportFragmentManager.backStackEntryCount > 0
        activity.setSupportActionBar(toolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(shouldDisplayNavArrow)
    }

    private fun setActionBarTitle(title: String): Unit {
        activityBinding.toolbarLayout.title = title
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

    override fun onTitleChanged(str: String) {
        viewModel.mutableProfile!!.title = str
        setActionBarTitle(str)
    }

    private fun initializeViews(view: View): Unit {

        if (passedExtras) {
            activityBinding.menuSaveChangesButton.setImageDrawable(ResourcesCompat.getDrawable(resources, android.R.drawable.ic_menu_save, null))
        }
        else {
            activityBinding.menuSaveChangesButton.setImageDrawable(ResourcesCompat.getDrawable(resources, android.R.drawable.ic_menu_add, null))
        }

        activityBinding.appBar.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {

            var isVisible: Boolean = false

            override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {

                if (abs(verticalOffset) - appBarLayout!!.totalScrollRange == 0) {
                    if (!isVisible && this@EditProfileFragment.isVisible) {
                        isVisible = true
                        changeMenuOptionVisibility(activityBinding?.menuEditNameButton, isVisible)
                    }
                } else if (isVisible) {
                    isVisible = false
                    changeMenuOptionVisibility(activityBinding.menuEditNameButton, isVisible)
                }
            }
        })
        val onClickListener: View.OnClickListener = View.OnClickListener {

            when (it.id) {

                R.id.SilentModeLayout -> {
                    if (binding.silentModeSwitch.isChecked) {
                        binding.silentModeSwitch.isChecked = false
                        viewModel.mutableProfile!!.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    }
                    else {
                        binding.silentModeSwitch.isChecked = true
                        viewModel.mutableProfile!!.ringerMode = AudioManager.RINGER_MODE_SILENT
                    }
                }
                R.id.vibrateForCallsLayout -> {
                    if (binding.vibrateForCallsSwitch.isChecked) {
                        binding.vibrateForCallsSwitch.isChecked = false
                        viewModel.mutableProfile!!.isVibrateForCallsActive = 0
                    }
                    else {
                        binding.vibrateForCallsSwitch.isChecked = true
                        viewModel.mutableProfile!!.isVibrateForCallsActive = 1
                    }
                }
                R.id.menuSaveChangesButton -> commitChanges()
                R.id.editNameButton, R.id.menuEditNameButton -> showTextInputDialog()
                R.id.phoneRingtoneLayout -> startRingtonePickerActivity(RingtoneManager.TYPE_RINGTONE)
                R.id.notificationSoundLayout -> startRingtonePickerActivity(RingtoneManager.TYPE_NOTIFICATION)
                R.id.alarmSoundLayout -> startRingtonePickerActivity(RingtoneManager.TYPE_ALARM)
                R.id.interruptionFilterLayout -> showPopupMenu(it)
                R.id.interruptionFilterPreferencesLayout -> callbacks?.onFragmentReplace(EditProfileActivity.DND_PREFERENCES_FRAGMENT)
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
                            if (binding.ringerSeekBar.progress == 0) {
                                silentModeLayoutTransition(true)
                            }
                            changeInterruptionFilterPrefsLayout(true)
                            changeSliderState(INTERRUPTION_FILTER_PRIORITY)
                        }
                        INTERRUPTION_FILTER_ALARMS -> {
                            silentModeLayoutTransition(false)
                            changeInterruptionFilterPrefsLayout(false)
                            changeSliderState(INTERRUPTION_FILTER_ALARMS)
                        }
                        INTERRUPTION_FILTER_NONE -> {
                            silentModeLayoutTransition(false)
                            changeInterruptionFilterPrefsLayout(false)
                            changeSliderState(INTERRUPTION_FILTER_NONE)
                        }
                    }
                }
                else {
                    viewModel.mutableProfile!!.isInterruptionFilterActive = 0
                    changeInterruptionFilterLayout(false)
                    changeInterruptionFilterPrefsLayout(false)
                    changeSliderState(INTERRUPTION_FILTER_ALL)
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
        if (activityBinding.menuSaveChangesButton.visibility != View.VISIBLE) {
            changeMenuOptionVisibility(activityBinding.menuSaveChangesButton, true)
        }
        activityBinding.menuSaveChangesButton.setOnClickListener(onClickListener)
        binding.vibrateForCallsLayout.setOnClickListener(onClickListener)
        binding.SilentModeLayout.setOnClickListener(onClickListener)
        binding.phoneRingtoneLayout.setOnClickListener(onClickListener)
        binding.notificationSoundLayout.setOnClickListener(onClickListener)
        binding.alarmSoundLayout.setOnClickListener(onClickListener)
        binding.interruptionFilterLayout.setOnClickListener(onClickListener)
        binding.interruptionFilterPreferencesLayout.setOnClickListener(onClickListener)

        binding.doNotDisturbSwitch.setOnCheckedChangeListener(onCheckedChangeListener)
        activityBinding.menuEditNameButton.setOnClickListener(onClickListener)
        activityBinding.editNameButton.setOnClickListener(onClickListener)

        binding.mediaSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        binding.phoneSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        binding.notificationSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        binding.alarmSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        binding.ringerSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)

        binding.phoneSeekBar.progress = MIN_CALL_STREAM_VALUE
        binding.alarmSeekBar.progress = MIN_ALARM_STREAM_VALUE
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
            binding.interruptionFilterLayout.isEnabled = true
            binding.interruptionFilterPolicy.setTextColor(Color.parseColor(DEFAULT_TEXT_COLOR))
            doNotDisturbTitle.setTextColor(Color.BLACK)
        }
        else {
            binding.interruptionFilterLayout.isEnabled = false
            binding.interruptionFilterPolicy.setTextColor(Color.parseColor(DISABLED_STATE_COLOR))
            doNotDisturbTitle.setTextColor(Color.parseColor(DISABLED_STATE_COLOR))
        }
    }

    private fun changeInterruptionFilterPrefsLayout(enable: Boolean): Unit {
        if (enable) {
            binding.interruptionFilterPreferencesLayout.isEnabled = true
            binding.doNotDisturbPreferencesTitle.setTextColor(Color.BLACK)
            binding.doNotDisturbRules.setTextColor(Color.parseColor(DEFAULT_TEXT_COLOR))
        }
        else {
            binding.interruptionFilterPreferencesLayout.isEnabled = false
            binding.doNotDisturbPreferencesTitle.setTextColor(Color.parseColor(DISABLED_STATE_COLOR))
            binding.doNotDisturbRules.setTextColor(Color.parseColor(DISABLED_STATE_COLOR))
        }
    }

    private fun showPopupMenu(view: View): Unit {
        val popupMenu: PopupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.dnd_mode_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {

                R.id.priority_only -> {
                    viewModel.mutableProfile!!.interruptionFilter = INTERRUPTION_FILTER_PRIORITY
                    changeSliderState(INTERRUPTION_FILTER_PRIORITY)
                    changeInterruptionFilterPrefsLayout(true)
                    binding.interruptionFilterPolicy.text = "Priority only"
                    if (binding.ringerSeekBar.progress == 0) {
                        silentModeLayoutTransition(true)
                    }
                    true
                }
                R.id.alarms_only -> {
                    viewModel.mutableProfile!!.interruptionFilter = INTERRUPTION_FILTER_ALARMS
                    changeSliderState(INTERRUPTION_FILTER_ALARMS)
                    changeInterruptionFilterPrefsLayout(false)
                    binding.interruptionFilterPolicy.text = "Alarms only"
                    silentModeLayoutTransition(false)
                    true
                }
                R.id.total_silence -> {
                    viewModel.mutableProfile!!.interruptionFilter = INTERRUPTION_FILTER_NONE
                    changeSliderState(INTERRUPTION_FILTER_NONE)
                    changeInterruptionFilterPrefsLayout(false)
                    binding.interruptionFilterPolicy.text = "Total silence"
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

    private fun changeSliderState(mode: Int) {
        when (mode) {
            INTERRUPTION_FILTER_ALL -> {
                binding.mediaSeekBar.isEnabled = true
                binding.notificationSeekBar.isEnabled = true
                binding.ringerSeekBar.isEnabled = true
                binding.alarmSeekBar.isEnabled = true
                binding.phoneSeekBar.isEnabled = true
            }
            INTERRUPTION_FILTER_PRIORITY -> {
                binding.phoneSeekBar.isEnabled = true
                binding.mediaSeekBar.isEnabled = containsCategory(PRIORITY_CATEGORY_MEDIA)
                binding.notificationSeekBar.isEnabled = true
                binding.ringerSeekBar.isEnabled = true
                binding.alarmSeekBar.isEnabled = containsCategory(PRIORITY_CATEGORY_ALARMS)
            }
            INTERRUPTION_FILTER_ALARMS -> {
                binding.phoneSeekBar.isEnabled = true
                binding.notificationSeekBar.isEnabled = false
                binding.ringerSeekBar.isEnabled = false
                binding.mediaSeekBar.isEnabled = true
                binding.alarmSeekBar.isEnabled = true
            }
            INTERRUPTION_FILTER_NONE -> {
                binding.phoneSeekBar.isEnabled = false
                binding.mediaSeekBar.isEnabled = false
                binding.notificationSeekBar.isEnabled = false
                binding.ringerSeekBar.isEnabled = false
                binding.alarmSeekBar.isEnabled = false
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
                        binding.phoneRingtone.text = getRingtoneTitle(it)
                    }
                    RingtoneManager.TYPE_NOTIFICATION -> {
                        viewModel.mutableProfile!!.notificationSoundUri = it
                        binding.notificationSound.text = getRingtoneTitle(it)
                    }
                    RingtoneManager.TYPE_ALARM -> {
                        viewModel.mutableProfile!!.alarmSoundUri = it
                        binding.alarmSound.text = getRingtoneTitle(it)
                    }
                    else -> Log.i("EditProfileFragment", "unknown ringtone type")
                }
            }
        }
    }

    private fun startRingtonePickerActivity(type: Int): Unit {
        ringtoneActivityCallback.launch(type)
    }

    private fun setObservers(): Unit {
        viewModel.alarmsLiveData.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it != null) {
                alarms = it
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
        binding.interruptionFilterPolicy.text = setInterruptionFilterTitle()
        binding.doNotDisturbSwitch.isChecked = isInterruptionFilterActive
        if (isInterruptionFilterActive) {
            changeInterruptionFilterLayout(true)
            changeSliderState(interruptionFilter)
            if (interruptionFilter == INTERRUPTION_FILTER_PRIORITY) {
                changeInterruptionFilterPrefsLayout(true)
            }
        }
        else {
            changeInterruptionFilterLayout(false)
            changeSliderState(INTERRUPTION_FILTER_ALL)
            changeInterruptionFilterPrefsLayout(false)
        }
    }

    private fun updateSliders(): Unit {
        binding.mediaSeekBar.progress = viewModel.mutableProfile!!.mediaVolume
        binding.phoneSeekBar.progress = viewModel.mutableProfile!!.callVolume
        binding.notificationSeekBar.progress = viewModel.mutableProfile!!.notificationVolume
        binding.ringerSeekBar.progress = viewModel.mutableProfile!!.ringVolume
        binding.alarmSeekBar.progress = viewModel.mutableProfile!!.alarmVolume
    }

    private fun updateUI(): Unit {
        setActionBarTitle(viewModel.mutableProfile!!.title)
        updateSliders()
        updateInterruptionFilterViews()
        if (passedExtras) {
            binding.phoneRingtone.text = getRingtoneTitle(viewModel.mutableProfile!!.phoneRingtoneUri)
            binding.notificationSound.text = getRingtoneTitle(viewModel.mutableProfile!!.notificationSoundUri)
            binding.alarmSound.text = getRingtoneTitle(viewModel.mutableProfile!!.alarmSoundUri)
        }
        else {
            setDefaultRingtoneUris()
            binding.phoneRingtone.text = getRingtoneTitle(viewModel.mutableProfile!!.phoneRingtoneUri)
            binding.alarmSound.text = getRingtoneTitle(viewModel.mutableProfile!!.alarmSoundUri)
            binding.notificationSound.text = getRingtoneTitle(viewModel.mutableProfile!!.notificationSoundUri)
        }
    }

    private fun getRingtoneTitle(uri: Uri): String {
        val contentResolver: ContentResolver = requireContext().contentResolver
        val projection: Array<String> = arrayOf(MediaStore.MediaColumns.TITLE)
        var title: String = ""
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                title = cursor.getString(0)
                cursor.close()
            }
        }
        return title
    }

    private fun applyImplicitChanges(): Unit {
        if (!alarms.isNullOrEmpty()) {
            viewModel.setMultipleAlarms(alarms!!, viewModel.mutableProfile!!)
        }
        viewModel.applyAudioSettingsIfActive()
    }

    private fun commitChanges(): Unit {
        applyImplicitChanges()
        if (passedExtras) {
            viewModel.updateProfile(viewModel.mutableProfile!!)
        }
        else {
            viewModel.addProfile(viewModel.mutableProfile!!)
        }
        requireActivity().finish()
    }

    private fun silentModeLayoutTransition(visible: Boolean): Unit {
        TransitionManager.beginDelayedTransition(binding.constraintRoot, AutoTransition())
        if (visible) binding.SilentModeLayout.visibility = View.VISIBLE else binding.SilentModeLayout.visibility = View.GONE
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
