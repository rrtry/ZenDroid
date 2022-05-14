package com.example.volumeprofiler.fragments

import android.Manifest.permission.*
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.R
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel
import androidx.databinding.DataBindingUtil
import com.example.volumeprofiler.activities.contract.RingtonePickerContract
import com.example.volumeprofiler.databinding.CreateProfileFragmentBinding
import com.example.volumeprofiler.entities.Profile
import android.app.NotificationManager.*
import android.content.*
import android.content.Context.VIBRATOR_SERVICE
import android.media.*
import android.media.AudioManager.*
import android.net.Uri
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.core.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.media.RingtoneManager.*
import android.os.*
import android.provider.Settings
import android.provider.Settings.System.canWrite
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.activities.ProfileDetailsActivity.Companion.INTERRUPTION_FILTER_FRAGMENT
import com.example.volumeprofiler.activities.ProfileDetailsActivity.Companion.TAG_PROFILE_FRAGMENT
import com.example.volumeprofiler.services.PlaybackService
import com.example.volumeprofiler.util.ViewUtil
import com.example.volumeprofiler.util.checkPermission
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileDetailsFragment: Fragment(), MediaPlayer.OnCompletionListener {

    @Inject lateinit var profileManager: ProfileManager

    private val detailsViewModel: ProfileDetailsViewModel by activityViewModels()

    private var bindingImpl: CreateProfileFragmentBinding? = null
    private val binding: CreateProfileFragmentBinding get() = bindingImpl!!

    private lateinit var ringtoneActivityLauncher: ActivityResultLauncher<Int>
    private lateinit var notificationPolicyLauncher: ActivityResultLauncher<Intent>
    private lateinit var systemPreferencesLauncher: ActivityResultLauncher<Intent>
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>

    private var callbacks: EditProfileActivityCallbacks? = null
    private var vibrator: Vibrator? = null
    private var mediaService: PlaybackService? = null
    private val serviceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            (service as PlaybackService.LocalBinder).apply {
                mediaService = getService()
                if (detailsViewModel.resumePlayback) {
                    detailsViewModel.onResumeRingtonePlayback(
                        detailsViewModel.currentStreamType,
                        detailsViewModel.playerPosition
                    )
                }
                detailsViewModel.resumePlayback = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            detailsViewModel.setPlaybackState(
                detailsViewModel.getPlayingRingtone(),
                false
            )
            mediaService = null
        }
    }

    private val notificationPolicyReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_NOTIFICATION_POLICY_CHANGED) {
                detailsViewModel.notificationPolicyAccessGranted.value = profileManager.isNotificationPolicyAccessGranted()
            }
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        mediaService?.release(detailsViewModel.currentStreamType)
        detailsViewModel.setPlaybackState(detailsViewModel.currentStreamType, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        detailsViewModel.currentFragmentTag.value = TAG_PROFILE_FRAGMENT
        registerForRingtonePickerResult()
        registerForNotificationPolicyResult()
        registerForPhonePermissionResult()
        registerForSystemSettingsResult()
        callbacks = requireActivity() as EditProfileActivityCallbacks
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("deprecation")
        vibrator = requireContext().getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        bindingImpl = DataBindingUtil.inflate(inflater, R.layout.create_profile_fragment, container, false)
        binding.viewModel = detailsViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        activity?.let {
            Intent(it, PlaybackService::class.java).also { intent ->
                it.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(notificationPolicyReceiver)

        mediaService?.let {
            detailsViewModel.currentMediaUri = it.mediaUri
            detailsViewModel.currentStreamType = it.streamType
            detailsViewModel.playerPosition = it.getCurrentPosition()
        }
        activity?.let {
            detailsViewModel.resumePlayback = it.isChangingConfigurations
            it.unbindService(serviceConnection)
        }
        detailsViewModel.setPlaybackState(
            detailsViewModel.getPlayingRingtone(),
            false
        )
        mediaService = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    detailsViewModel.fragmentEventsFlow.collect {
                        when (it) {
                            is ProfileDetailsViewModel.ViewEvent.ShowDialogFragment -> {
                                getFragmentInstance(it.dialogType).show(requireActivity().supportFragmentManager, null)
                            }
                            is ProfileDetailsViewModel.ViewEvent.ResumeRingtonePlayback -> {
                                detailsViewModel.currentMediaUri?.let { uri ->
                                    mediaService?.resume(
                                        this@ProfileDetailsFragment,
                                        uri,
                                        detailsViewModel.currentStreamType,
                                        detailsViewModel.getStreamVolume(detailsViewModel.currentStreamType),
                                        it.position)
                                }
                                detailsViewModel.setPlaybackState(detailsViewModel.currentStreamType, true)
                            }
                            is ProfileDetailsViewModel.ViewEvent.StopRingtonePlayback -> {
                                mediaService?.release(it.streamType)
                                detailsViewModel.setPlaybackState(it.streamType, false)
                            }
                            is ProfileDetailsViewModel.ViewEvent.StartRingtonePlayback -> {
                                if (detailsViewModel.getStreamVolume(it.streamType) > 0) {

                                    val ringtoneMap: Map<Int, Int> = mapOf(
                                        STREAM_ALARM to TYPE_ALARM,
                                        STREAM_NOTIFICATION to TYPE_NOTIFICATION,
                                        STREAM_RING to TYPE_RINGTONE,
                                        STREAM_MUSIC to TYPE_RINGTONE,
                                        STREAM_VOICE_CALL to TYPE_RINGTONE
                                    )

                                    detailsViewModel.currentMediaUri = getRingtoneUri(ringtoneMap[it.streamType]!!)
                                    detailsViewModel.currentStreamType = it.streamType
                                    detailsViewModel.currentMediaUri?.let { uri ->
                                        mediaService?.start(
                                            this@ProfileDetailsFragment,
                                            uri,
                                            it.streamType,
                                            detailsViewModel.getStreamVolume(it.streamType))
                                    }
                                    detailsViewModel.setPlaybackState(it.streamType, true)
                                }
                            }
                            is ProfileDetailsViewModel.ViewEvent.ChangeRingerMode -> {
                                updateRingerMode(it.streamType)
                                if (it.vibrate) {
                                    createVibrateEffect()
                                }
                            }

                            is ProfileDetailsViewModel.ViewEvent.StreamVolumeChanged -> changePlaybackVolume(it.streamType, it.volume)
                            is ProfileDetailsViewModel.ViewEvent.GetDefaultRingtoneUri -> setDefaultRingtoneUri(it.type)
                            is ProfileDetailsViewModel.ViewEvent.ChangeRingtoneEvent -> startRingtonePickerActivity(it.ringtoneType)

                            ProfileDetailsViewModel.ViewEvent.PhonePermissionRequestEvent -> phonePermissionLauncher.launch(READ_PHONE_STATE)
                            ProfileDetailsViewModel.ViewEvent.WriteSystemSettingsRequestEvent -> startSystemSettingsActivity()

                            ProfileDetailsViewModel.ViewEvent.NavigateToNextFragment -> callbacks?.onFragmentReplace(INTERRUPTION_FILTER_FRAGMENT)
                            ProfileDetailsViewModel.ViewEvent.ShowPopupWindowEvent -> showPopupMenu()
                            ProfileDetailsViewModel.ViewEvent.NotificationPolicyRequestEvent -> startNotificationPolicyActivity()

                            else -> Log.i("EditProfileFragment", "unknown event")
                        }
                    }
                }
                launch {
                    detailsViewModel.policyAllowsRingerStream.collect { policyAllowsStream ->
                        if (!policyAllowsStream) {
                            detailsViewModel.ringVolume.value = 0
                            detailsViewModel.ringerMode.value = RINGER_MODE_SILENT
                            if (detailsViewModel.currentStreamType == STREAM_RING) {
                                detailsViewModel.onPlayRingtoneButtonClick(STREAM_RING)
                            }
                        }
                    }
                }
                launch {
                    detailsViewModel.policyAllowsNotificationsStream.collect { policyAllowsStream ->
                        if (!policyAllowsStream) {
                            detailsViewModel.notificationVolume.value = 0
                            detailsViewModel.notificationMode.value = RINGER_MODE_SILENT
                            if (detailsViewModel.currentStreamType == STREAM_NOTIFICATION) {
                                detailsViewModel.onStopRingtonePlayback(STREAM_NOTIFICATION)
                            }
                        }
                    }
                }
                launch {
                    detailsViewModel.policyAllowsMediaStream.collect {
                        if (!it && detailsViewModel.currentStreamType == STREAM_MUSIC) {
                            detailsViewModel.onStopRingtonePlayback(STREAM_MUSIC)
                        }
                    }
                }
                launch {
                    detailsViewModel.policyAllowsAlarmStream.collect {
                        if (!it && detailsViewModel.currentStreamType == STREAM_ALARM) {
                            detailsViewModel.onStopRingtonePlayback(STREAM_ALARM)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(notificationPolicyReceiver, IntentFilter(ACTION_NOTIFICATION_POLICY_CHANGED))
        setNotificationPolicyProperty()
        setCanWriteSettingsProperty()
        setPhonePermissionProperty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingImpl = null
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
        ringtoneActivityLauncher.unregister()
        notificationPolicyLauncher.unregister()
        systemPreferencesLauncher.unregister()
        phonePermissionLauncher.unregister()
    }

    private fun startSystemSettingsActivity() {
        systemPreferencesLauncher.launch(
            Intent(ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${requireContext().packageName}"))
        )
    }

    private fun startRingtonePickerActivity(type: Int) {
        val contract: RingtonePickerContract = ringtoneActivityLauncher.contract as RingtonePickerContract
        when (type) {
            TYPE_RINGTONE -> contract.existingUri = detailsViewModel.phoneRingtoneUri.value
            TYPE_NOTIFICATION -> contract.existingUri = detailsViewModel.notificationSoundUri.value
            TYPE_ALARM -> contract.existingUri = detailsViewModel.alarmSoundUri.value
            else -> Log.i("EditProfileFragment", "unknown ringtone type")
        }
        ringtoneActivityLauncher.launch(type)
    }

    private fun startNotificationPolicyActivity() {
        notificationPolicyLauncher.launch(Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun setPhonePermissionProperty() {
        detailsViewModel.phonePermissionGranted.value = checkPermission(READ_PHONE_STATE)
    }

    private fun setNotificationPolicyProperty() {
        detailsViewModel.notificationPolicyAccessGranted.value = profileManager.isNotificationPolicyAccessGranted()
    }

    private fun setCanWriteSettingsProperty() {
        detailsViewModel.canWriteSettings.value = canWrite(requireContext())
    }

    private fun registerForSystemSettingsResult(): Unit {
        systemPreferencesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            canWrite(requireContext()).let {
                detailsViewModel.canWriteSettings.value = it
                if (!it) {
                    ViewUtil.showSystemSettingsPermissionExplanation(requireActivity().supportFragmentManager)
                }
            }
        }
    }

    private fun registerForRingtonePickerResult() {
        RingtonePickerContract().apply {
            ringtoneActivityLauncher = registerForActivityResult(this) {
                it?.also {
                    when (ringtoneType) {
                        TYPE_RINGTONE -> detailsViewModel.phoneRingtoneUri.value = it
                        TYPE_NOTIFICATION -> detailsViewModel.notificationSoundUri.value = it
                        TYPE_ALARM -> detailsViewModel.alarmSoundUri.value = it
                        else -> Log.i("EditProfileFragment", "unknown ringtone type")
                    }
                }
            }
        }
    }

    private fun registerForNotificationPolicyResult() {
        notificationPolicyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            profileManager.isNotificationPolicyAccessGranted().also { granted ->
                detailsViewModel.notificationPolicyAccessGranted.value = granted
                if (!granted) {
                    ViewUtil.showInterruptionPolicyAccessExplanation(requireActivity().supportFragmentManager)
                }
            }
        }
    }

    private fun registerForPhonePermissionResult() {
        phonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            detailsViewModel.phonePermissionGranted.value = it
            detailsViewModel.streamsUnlinked.value = it
            if (!it) {
                ViewUtil.showPhoneStatePermissionExplanation(requireActivity().supportFragmentManager)
            }
        }
    }

    private fun setDefaultRingtoneUri(type: Int) {
        val uri: Uri = getActualDefaultRingtoneUri(context, type)
        when {
            detailsViewModel.notificationSoundUri.value == Uri.EMPTY && type == TYPE_NOTIFICATION -> {
                detailsViewModel.notificationSoundUri.value = uri
            }
            detailsViewModel.alarmSoundUri.value == Uri.EMPTY && type == TYPE_ALARM -> {
                detailsViewModel.alarmSoundUri.value = uri
            }
            detailsViewModel.phoneRingtoneUri.value == Uri.EMPTY && type == TYPE_RINGTONE -> {
                detailsViewModel.phoneRingtoneUri.value = uri
            }
        }
    }

    private fun createVibrateEffect() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, 100))
            }
            else -> vibrator?.vibrate(100)
        }
    }

    private fun getRingtoneUri(type: Int): Uri {
        return detailsViewModel.getRingtoneUri(type)
    }

    private fun changePlaybackVolume(streamType: Int, vol: Int) {
        mediaService?.let {
            if (it.isPlaying() && streamType == detailsViewModel.currentStreamType) {
                if (vol > 0) {
                    profileManager.setStreamVolume(streamType, vol, 0)
                } else {
                    detailsViewModel.onStopRingtonePlayback(streamType)
                }
            }
        }
    }

    private fun updateRingerMode(streamType: Int, mode: Int) {
        when (streamType) {
            STREAM_NOTIFICATION -> detailsViewModel.notificationMode.value = mode
            STREAM_RING -> detailsViewModel.ringerMode.value = mode
        }
    }

    private fun updateRingerMode(streamType: Int) {
        vibrator?.let {
            if (it.hasVibrator()) {
                updateRingerMode(streamType, RINGER_MODE_VIBRATE)
            } else {
                updateRingerMode(streamType, RINGER_MODE_SILENT)
            }
        }
    }

    private fun showPopupMenu() {
        val popupMenu: PopupMenu = PopupMenu(requireContext(), binding.interruptionFilterLayout)
        popupMenu.inflate(R.menu.dnd_mode_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.priority_only -> {
                    detailsViewModel.interruptionFilter.value = INTERRUPTION_FILTER_PRIORITY
                    true
                }
                R.id.alarms_only -> {
                    detailsViewModel.interruptionFilter.value = INTERRUPTION_FILTER_ALARMS
                    true
                }
                R.id.total_silence -> {
                    detailsViewModel.interruptionFilter.value = INTERRUPTION_FILTER_NONE
                    true
                }
                R.id.allow_all -> {
                    detailsViewModel.interruptionFilter.value = INTERRUPTION_FILTER_ALL
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun getFragmentInstance(dialogType: ProfileDetailsViewModel.DialogType): DialogFragment {
        return when (dialogType) {
            ProfileDetailsViewModel.DialogType.SUPPRESSED_EFFECTS_OFF ->
                SuppressedEffectsOffDialog.newInstance(detailsViewModel.getProfile())
            ProfileDetailsViewModel.DialogType.SUPPRESSED_EFFECTS_ON ->
                SuppressedEffectsOnDialog.newInstance(detailsViewModel.getProfile())
            else -> throw IllegalArgumentException("Unknown dialog type")
        }
    }

    companion object {

        private const val EXTRA_PROFILE = "profile"

        fun newInstance(profile: Profile?): ProfileDetailsFragment {
            val args: Bundle = Bundle()
            args.putParcelable(EXTRA_PROFILE, profile)
            return ProfileDetailsFragment().apply {
                profile?.let {
                    arguments = args
                }
            }
        }
    }
}
