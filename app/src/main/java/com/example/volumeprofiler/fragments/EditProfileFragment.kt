package com.example.volumeprofiler.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
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
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.util.AlarmUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import android.app.NotificationManager.*
import android.media.AudioManager.*

@SuppressLint("UseSwitchCompatOrMaterialCode")
class EditProfileFragment: Fragment() {

    private val viewModel: EditProfileViewModel by activityViewModels()

    private var _binding: CreateProfileFragmentBinding? = null
    private val binding: CreateProfileFragmentBinding get() = _binding!!

    private lateinit var job: Job

    private lateinit var ringtoneActivityCallback: ActivityResultLauncher<Int>
    private lateinit var storagePermissionCallback: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setArgs()
    }

    private fun setArgs(): Unit {
        if (arguments?.getParcelable<Profile>(EXTRA_PROFILE) != null) {
            val arg: Profile = arguments?.getParcelable<Profile>(EXTRA_PROFILE)!!
            viewModel.setArgs(arg, true)
        } else {
            viewModel.setArgs(Profile("New profile"), false)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        registerForActivityResult()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.create_profile_fragment, container, false)
        binding.viewModel = this.viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        job.cancel()
    }

    override fun onStart() {
        super.onStart()
        job = viewModel.eventsFlow.onEach {
                    when (it) {
                        EditProfileViewModel.Event.StoragePermissionRequestEvent -> {
                            requestStoragePermission()
                        }
                        EditProfileViewModel.Event.ShowPopupWindowEvent -> {
                            showPopupMenu()
                        }
                        is EditProfileViewModel.Event.ChangeRingtoneEvent -> {
                            startRingtonePickerActivity(it.ringtoneType)
                        }
                        is EditProfileViewModel.Event.ResetAlarmsEvent -> {
                            // TODO handle event
                        }
                    }
                }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun startRingtonePickerActivity(type: Int): Unit {
        ringtoneActivityCallback.launch(type)
    }

    private fun requestStoragePermission(): Unit {
        storagePermissionCallback.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
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
                    viewModel.ringerMode.value = RINGER_MODE_SILENT
                    true
                }
                R.id.total_silence -> {
                    viewModel.interruptionFilter.value = INTERRUPTION_FILTER_NONE
                    viewModel.ringerMode.value = RINGER_MODE_SILENT
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

    private fun registerForPermissionResult(): Unit {
        storagePermissionCallback = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                Log.i("EditProfileFragment", "READ_EXTERNAL_STORAGE granted")
            } else {
                Log.i("EditProfileFragment", "READ_EXTERNAL_STORAGE denied")
            }
        }
    }

    fun setAlarms(triggers: List<AlarmTrigger>, newProfile: Profile): Unit {
        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
        for (i in triggers) {
            alarmUtil.setAlarm(i.alarm, newProfile, false)
        }
    }

    /*
    fun applyAudioSettingsIfActive(): Unit {
        val sharedPreferencesUtil = SharedPreferencesUtil.getInstance()
        if (sharedPreferencesUtil.getActiveProfileId()
                == mutableProfile!!.id.toString()) {
            val profileUtil: ProfileUtil = ProfileUtil.getInstance()
            profileUtil.applyAudioSettings(mutableProfile!!)
        }
    }
     */

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
