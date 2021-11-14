package com.example.volumeprofiler.fragments

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.R
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.volumeprofiler.activities.customContract.RingtonePickerContract
import com.example.volumeprofiler.databinding.CreateProfileFragmentBinding
import com.example.volumeprofiler.entities.Profile
import android.app.NotificationManager.*
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager.*
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.example.volumeprofiler.activities.EditProfileActivity
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.util.ProfileUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.volumeprofiler.viewmodels.EditProfileViewModel.Event.*
import android.media.RingtoneManager.*
import android.provider.Settings
import com.example.volumeprofiler.fragments.dialogs.PermissionExplanationDialog
import com.example.volumeprofiler.util.ViewUtil
import com.example.volumeprofiler.util.checkSelfPermission
import com.example.volumeprofiler.util.getApplicationSettingsIntent
import com.google.android.material.snackbar.Snackbar

@SuppressLint("UseSwitchCompatOrMaterialCode")
@AndroidEntryPoint
class EditProfileFragment: Fragment() {

    @Inject
    lateinit var profileUtil: ProfileUtil

    private val viewModel: EditProfileViewModel by activityViewModels()
    private var callbacks: EditProfileActivityCallbacks? = null

    private var _binding: CreateProfileFragmentBinding? = null
    private val binding: CreateProfileFragmentBinding get() = _binding!!

    private lateinit var ringtoneActivityCallback: ActivityResultLauncher<Int>
    private lateinit var storagePermissionCallback: ActivityResultLauncher<String>
    private lateinit var notificationPolicyCallback: ActivityResultLauncher<Intent>
    private lateinit var writeSystemSettingsCallback: ActivityResultLauncher<Intent>
    private lateinit var phonePermissionCallback: ActivityResultLauncher<String>

    private var notificationPolicyReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_NOTIFICATION_POLICY_CHANGED) {
                viewModel.notificationPolicyAccessGranted.value = profileUtil.isNotificationPolicyAccessGranted()
            }
        }
    }

    private var hapticService: Vibrator? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        registerForRingtonePickerResult()
        registerForStoragePermissionResult()
        registerForNotificationPolicyResult()
        registerForPhonePermissionResult()
        registerForSystemSettingsResult()
        callbacks = requireActivity() as EditProfileActivityCallbacks
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
        ringtoneActivityCallback.unregister()
        storagePermissionCallback.unregister()
        notificationPolicyCallback.unregister()
        writeSystemSettingsCallback.unregister()
        phonePermissionCallback.unregister()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hapticService = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.create_profile_fragment, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    private fun startSystemSettingsActivity(): Unit {
        writeSystemSettingsCallback.launch(
            Intent(ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${requireActivity().packageName}"))
        )
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(notificationPolicyReceiver, IntentFilter(ACTION_NOTIFICATION_POLICY_CHANGED))
        setStoragePermissionProperty()
        setNotificationPolicyProperty()
        setCanWriteSettingsProperty()
        setPhonePermissionProperty()
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(notificationPolicyReceiver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().supportFragmentManager.setFragmentResultListener(
            PermissionExplanationDialog.PERMISSION_REQUEST_KEY, viewLifecycleOwner,
            { requestKey, result ->
                if (result.getBoolean(PermissionExplanationDialog.EXTRA_RESULT_OK)) {
                    val permission: String = result.getString(PermissionExplanationDialog.EXTRA_PERMISSION)!!
                    if (permission == ACCESS_NOTIFICATION_POLICY) {
                        startNotificationPolicyActivity()
                    } else {
                        if (shouldShowRequestPermissionRationale(permission)) {
                            if (permission == READ_PHONE_STATE) {
                                phonePermissionCallback.launch(READ_PHONE_STATE)
                            }
                            else {
                                storagePermissionCallback.launch(READ_EXTERNAL_STORAGE)
                            }
                        } else {
                            startSystemSettingsActivity()
                        }
                    }
                }
            })
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.fragmentEventsFlow.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED).onEach {
                when (it) {

                    WriteSystemSettingsRequestEvent -> {
                        startSystemSettingsActivity()
                    }

                    NavigateToNextFragment -> {
                        callbacks?.onFragmentReplace(EditProfileActivity.DND_PREFERENCES_FRAGMENT)
                    }

                    StoragePermissionRequestEvent -> {
                        requestStoragePermission()
                    }

                    PhonePermissionRequestEvent -> {
                        requestPhoneStatePermission()
                    }

                    ShowPopupWindowEvent -> {
                        showPopupMenu()
                    }

                    NotificationPolicyRequestEvent -> {
                        startNotificationPolicyActivity()
                    }

                    is ChangeRingtoneEvent -> {
                        startRingtonePickerActivity(it.ringtoneType)
                    }

                    is ChangeRingerMode -> {
                        changeRingerMode(it.streamType)
                        if (it.vibrate) {
                            createVibrateEffect()
                        }
                    }
                    else -> Log.i("EditProfileFragment", "unknown event")
                }
            }.collect()
        }
        removeFromLayout(binding.SilentModeLayout)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setStoragePermissionProperty(): Unit {
        viewModel.storagePermissionGranted.value = checkSelfPermission(requireContext(), READ_EXTERNAL_STORAGE)
    }

    private fun setPhonePermissionProperty(): Unit {
        viewModel.phonePermissionGranted.value = checkSelfPermission(requireContext(), READ_PHONE_STATE)
    }

    private fun setNotificationPolicyProperty(): Unit {
        viewModel.notificationPolicyAccessGranted.value = profileUtil.isNotificationPolicyAccessGranted()
    }

    private fun setCanWriteSettingsProperty(): Unit {
        viewModel.canWriteSettings.value = profileUtil.canWriteSettings()
    }

    private fun registerForNotificationPolicyResult(): Unit {
        notificationPolicyCallback = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val granted: Boolean = profileUtil.isNotificationPolicyAccessGranted()
            viewModel.notificationPolicyAccessGranted.value = granted
            if (!granted) {
                ViewUtil.showInterruptionPolicyAccessExplanation(requireActivity().supportFragmentManager)
            }
        }
    }

    private fun registerForPhonePermissionResult(): Unit {
        phonePermissionCallback = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.phonePermissionGranted.value = it
            when {
                it -> {
                    viewModel.streamsUnlinked.value = true
                }
                shouldShowRequestPermissionRationale(READ_PHONE_STATE)  -> {
                    ViewUtil.showPhoneStatePermissionExplanation(requireActivity().supportFragmentManager)
                }
                else -> {
                    ViewUtil.showPhoneStatePermissionExplanation(requireActivity().supportFragmentManager)
                }
            }
        }
    }

    private fun registerForStoragePermissionResult(): Unit {
        storagePermissionCallback = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.storagePermissionGranted.value = it
            when {
                it -> {
                    viewModel.updateSoundUris()
                    Snackbar.make(binding.root,
                        "Storage permission was granted", Snackbar.LENGTH_LONG
                    ).show()
                }
                shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)  -> {
                    ViewUtil.showStoragePermissionExplanation(requireActivity().supportFragmentManager)
                }
                else -> {
                    ViewUtil.showStoragePermissionExplanation(requireActivity().supportFragmentManager)
                }
            }
        }
    }

    private fun registerForSystemSettingsResult(): Unit {
        writeSystemSettingsCallback = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val canWriteSettings: Boolean = Settings.System.canWrite(requireContext())
            Log.i("EditProfileFragment", "writeSystemSettings: $canWriteSettings")
            viewModel.canWriteSettings.value = canWriteSettings
            if (!canWriteSettings) {
                ViewUtil.showSystemSettingsPermissionExplanation(requireActivity().supportFragmentManager)
            }
        }
    }

    private fun registerForRingtonePickerResult(): Unit {
        val contract: RingtonePickerContract = RingtonePickerContract()
        ringtoneActivityCallback = registerForActivityResult(contract) {
            if (it != null) {
                when (contract.ringtoneType) {
                    TYPE_RINGTONE -> {
                        viewModel.phoneRingtoneUri.value = it
                    }
                    TYPE_NOTIFICATION -> {
                        viewModel.notificationSoundUri.value = it
                    }
                    TYPE_ALARM -> {
                        viewModel.alarmSoundUri.value = it
                    }
                    else -> Log.i("EditProfileFragment", "unknown ringtone type")
                }
            }
        }
    }

    private fun hasVibratorHardware(): Boolean {
        return hapticService!!.hasVibrator()
    }

    private fun startRingtonePickerActivity(type: Int): Unit {
        val contract: RingtonePickerContract = ringtoneActivityCallback.contract as RingtonePickerContract
        when (type) {
            TYPE_RINGTONE -> {
                contract.existingUri = viewModel.phoneRingtoneUri.value!!
            }
            TYPE_NOTIFICATION -> {
                contract.existingUri = viewModel.notificationSoundUri.value!!
            }
            TYPE_ALARM -> {
                contract.existingUri = viewModel.alarmSoundUri.value!!
            }
            else -> Log.i("EditProfileFragment", "unknown ringtone type")
        }
        ringtoneActivityCallback.launch(type)
    }

    private fun startNotificationPolicyActivity(): Unit {
        notificationPolicyCallback.launch(Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    @Suppress("deprecation")
    private fun getDisplayDensity(): Float {
        val windowManager: WindowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = windowManager.defaultDisplay
        val displayMetrics: DisplayMetrics = DisplayMetrics()
        display.getRealMetrics(displayMetrics)
        return displayMetrics.density
    }

    private fun removeFromLayout(view: View): Unit {
        if (!hasVibratorHardware()) {
            setConstraints()
            binding.constraintRoot.removeView(view)
        }
    }

    private fun setConstraints(): Unit {
        val constraintSet: ConstraintSet = ConstraintSet()
        constraintSet.clone(binding.constraintRoot)
        constraintSet.connect(R.id.separator1, ConstraintSet.TOP, R.id.unlinkStreamsLayout, ConstraintSet.BOTTOM, (8 * getDisplayDensity()).toInt())
        constraintSet.applyTo(binding.constraintRoot)
    }

    @Suppress("deprecation")
    private fun createVibrateEffect(): Unit {
        if (hasVibratorHardware()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hapticService!!.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                hapticService!!.vibrate(100)
            }
        }
    }

    private fun changeRingerMode(streamType: Int, mode: Int): Unit {
        when (streamType) {
            STREAM_NOTIFICATION -> {
                viewModel.notificationMode.value = mode
            }
            STREAM_RING -> {
                viewModel.ringerMode.value = mode
            }
        }
    }

    private fun changeRingerMode(streamType: Int): Unit {
        if (hasVibratorHardware()) {
            changeRingerMode(streamType, RINGER_MODE_VIBRATE)
        } else {
            changeRingerMode(streamType, RINGER_MODE_SILENT)
        }
    }

    private fun showToast(streamType: Int): Unit {
        if (hasVibratorHardware()) {
            Toast.makeText(requireContext(), if (streamType == STREAM_NOTIFICATION) "Notifications are set to vibrate" else "Ringer is set to vibrate", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), if (streamType == STREAM_NOTIFICATION) "Notifications are set to silent" else "Ringer is set to silent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestStoragePermission(): Unit {
        storagePermissionCallback.launch(READ_EXTERNAL_STORAGE)
    }

    private fun requestPhoneStatePermission(): Unit {
        phonePermissionCallback.launch(READ_PHONE_STATE)
    }

    private fun showPopupMenu(): Unit {
        val popupMenu: PopupMenu = PopupMenu(requireContext(), binding.interruptionFilterLayout)
        popupMenu.inflate(R.menu.dnd_mode_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.priority_only -> {
                    viewModel.interruptionFilter.value = INTERRUPTION_FILTER_PRIORITY
                    true
                }
                R.id.alarms_only -> {
                    viewModel.interruptionFilter.value = INTERRUPTION_FILTER_ALARMS
                    true
                }
                R.id.total_silence -> {
                    viewModel.interruptionFilter.value = INTERRUPTION_FILTER_NONE
                    true
                }
                R.id.allow_all -> {
                    viewModel.interruptionFilter.value = INTERRUPTION_FILTER_ALL
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    companion object {

        private const val EXTRA_PROFILE = "profile"
        private const val LOG_TAG = "EditProfileFragment"

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
