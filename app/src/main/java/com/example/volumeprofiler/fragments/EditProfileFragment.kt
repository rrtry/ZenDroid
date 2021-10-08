package com.example.volumeprofiler.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
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
import com.example.volumeprofiler.models.Profile
import android.app.NotificationManager.*
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.example.volumeprofiler.activities.EditProfileActivity
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@SuppressLint("UseSwitchCompatOrMaterialCode")
@AndroidEntryPoint
class EditProfileFragment: Fragment() {

    private val viewModel: EditProfileViewModel by activityViewModels()
    private var callbacks: EditProfileActivityCallbacks? = null

    private var _binding: CreateProfileFragmentBinding? = null
    private val binding: CreateProfileFragmentBinding get() = _binding!!

    private lateinit var ringtoneActivityCallback: ActivityResultLauncher<Int>
    private lateinit var storagePermissionCallback: ActivityResultLauncher<String>
    private lateinit var notificationPolicyCallback: ActivityResultLauncher<Intent>

    private var hapticService: Vibrator? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        registerForActivityResult()
        registerForPermissionResult()
        registerForNotificationPolicyResult()
        callbacks = requireActivity() as EditProfileActivityCallbacks
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
        ringtoneActivityCallback.unregister()
        storagePermissionCallback.unregister()
        notificationPolicyCallback.unregister()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.fragmentEventsFlow.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED).onEach {
                when (it) {
                    EditProfileViewModel.Event.NavigateToNextFragment -> {
                        callbacks?.onFragmentReplace(EditProfileActivity.DND_PREFERENCES_FRAGMENT)
                    }
                    EditProfileViewModel.Event.StoragePermissionRequestEvent -> {
                        requestStoragePermission()
                    }
                    EditProfileViewModel.Event.ShowPopupWindowEvent -> {
                        showPopupMenu()
                    }
                    EditProfileViewModel.Event.NotificationPolicyRequestEvent -> {
                        startNotificationPolicyActivity()
                    }
                    is EditProfileViewModel.Event.ChangeRingtoneEvent -> {
                        startRingtonePickerActivity(it.ringtoneType)
                    }
                    is EditProfileViewModel.Event.ChangeRingerMode -> {
                        if (it.fromUser) {
                            changeMode(it.streamType)
                            createVibrateEffect()
                            showToast(it.streamType)
                        }
                    }
                    else -> Log.i("EditProfileFragment", "unknown event")
                }
            }.collect()
        }
        removeFromLayout(binding.SilentModeLayout)
    }

    override fun onResume() {
        super.onResume()
        setStoragePermissionProperty()
        setNotificationPolicyProperty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkNotificationPolicyAccess(): Boolean {
        val notificationManager: NotificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    private fun setStoragePermissionProperty(): Unit {
        viewModel.storagePermissionGranted.value = checkStoragePermission()
    }

    private fun setNotificationPolicyProperty(): Unit {
        viewModel.notificationPolicyAccessGranted.value = checkNotificationPolicyAccess()
    }

    private fun registerForNotificationPolicyResult(): Unit {
        notificationPolicyCallback = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.notificationPolicyAccessGranted.value = checkNotificationPolicyAccess()
        }
    }

    private fun registerForPermissionResult(): Unit {
        storagePermissionCallback = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.storagePermissionGranted.value = it
            if (it) {
                viewModel.updateSoundUris()
            }
        }
    }

    private fun registerForActivityResult(): Unit {
        val contract: RingtonePickerContract = RingtonePickerContract()
        ringtoneActivityCallback = registerForActivityResult(contract) {
            if (it != null) {
                when (contract.ringtoneType) {
                    RingtoneManager.TYPE_RINGTONE -> {
                        viewModel.phoneRingtoneUri.value = it
                    }
                    RingtoneManager.TYPE_NOTIFICATION -> {
                        viewModel.notificationSoundUri.value = it
                    }
                    RingtoneManager.TYPE_ALARM -> {
                        viewModel.alarmSoundUri.value = it
                    }
                    else -> Log.i("EditProfileFragment", "unknown ringtone type")
                }
            }
        }
    }

    private fun startRingtonePickerActivity(type: Int): Unit {
        val contract: RingtonePickerContract = ringtoneActivityCallback.contract as RingtonePickerContract
        when (type) {
            RingtoneManager.TYPE_RINGTONE -> {
                contract.existingUri = viewModel.phoneRingtoneUri.value!!
            }
            RingtoneManager.TYPE_NOTIFICATION -> {
                contract.existingUri = viewModel.notificationSoundUri.value!!
            }
            RingtoneManager.TYPE_ALARM -> {
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
        setConstraints()
        if (!hapticService!!.hasVibrator()) {
            binding.constraintRoot.removeView(view)
        }
    }

    private fun setConstraints(): Unit {
        val constraintSet: ConstraintSet = ConstraintSet()
        constraintSet.clone(binding.constraintRoot)
        constraintSet.connect(R.id.separator1, ConstraintSet.TOP, R.id.alarmSeekBar, ConstraintSet.BOTTOM, (32 * getDisplayDensity()).toInt())
        constraintSet.applyTo(binding.constraintRoot)
    }

    @Suppress("deprecation")
    private fun createVibrateEffect(): Unit {
        if (hapticService!!.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hapticService!!.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                hapticService!!.vibrate(200)
            }
        }
    }

    private fun changeMode(streamType: Int, mode: Int): Unit {
        if (streamType == STREAM_NOTIFICATION) {
            viewModel.notificationMode.value = mode
        } else if (streamType == STREAM_RING) {
            viewModel.ringerMode.value = mode
        }
    }

    private fun changeMode(streamType: Int): Unit {
        if (hapticService!!.hasVibrator()) {
            changeMode(streamType, RINGER_MODE_VIBRATE)
        } else {
            changeMode(streamType, RINGER_MODE_SILENT)
        }
    }

    private fun showToast(streamType: Int): Unit {
        if (hapticService!!.hasVibrator()) {
            Toast.makeText(requireContext(), if (streamType == STREAM_NOTIFICATION) "Notifications are set to vibrate" else "Ringer is set to vibrate", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), if (streamType == STREAM_NOTIFICATION) "Notifications are set to silent" else "Ringer is set to silent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestStoragePermission(): Unit {
        storagePermissionCallback.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
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
