package ru.rrtry.silentdroid.ui.fragments

import android.Manifest.permission.*
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel
import androidx.databinding.DataBindingUtil
import ru.rrtry.silentdroid.contract.RingtonePickerContract
import ru.rrtry.silentdroid.entities.Profile
import android.app.NotificationManager.*
import android.content.*
import android.content.Context.VIBRATOR_SERVICE
import android.media.*
import android.media.AudioManager.*
import android.net.Uri
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
import ru.rrtry.silentdroid.interfaces.ProfileDetailsActivityCallback
import ru.rrtry.silentdroid.core.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.media.RingtoneManager.*
import android.os.*
import android.provider.Settings.System.canWrite
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ru.rrtry.silentdroid.ui.activities.ProfileDetailsDetailsActivity.Companion.INTERRUPTION_FILTER_FRAGMENT
import ru.rrtry.silentdroid.services.PlaybackService
import ru.rrtry.silentdroid.ui.activities.ProfileDetailsDetailsActivity.Companion.NOTIFICATION_RESTRICTIONS_FRAGMENT
import ru.rrtry.silentdroid.util.ViewUtil
import ru.rrtry.silentdroid.util.checkPermission
import kotlinx.coroutines.launch
import ru.rrtry.silentdroid.databinding.CreateProfileFragmentBinding
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel.ViewEvent.*
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel.*
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel.DialogType.*

@AndroidEntryPoint
class ProfileDetailsFragment: ViewBindingFragment<CreateProfileFragmentBinding>(), MediaPlayer.OnCompletionListener {

    @Inject
    lateinit var profileManager: ProfileManager

    private val viewModel: ProfileDetailsViewModel by activityViewModels()

    private lateinit var ringtoneActivityLauncher: ActivityResultLauncher<Int>
    private lateinit var notificationPolicyLauncher: ActivityResultLauncher<Intent>
    private lateinit var systemPreferencesLauncher: ActivityResultLauncher<Intent>
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>

    private var callbacksDetails: ProfileDetailsActivityCallback? = null
    private var vibrator: Vibrator? = null

    private var mediaService: PlaybackService? = null
    private val serviceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            (service as PlaybackService.LocalBinder).apply {
                mediaService = getService()
                if (viewModel.resumePlayback) {
                    viewModel.onResumeRingtonePlayback(
                        viewModel.currentStreamType,
                        viewModel.playerPosition
                    )
                }
                viewModel.resumePlayback = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            viewModel.setPlaybackState(
                viewModel.getPlayingRingtone(),
                false
            )
            mediaService = null
        }
    }

    private val notificationPolicyReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_NOTIFICATION_POLICY_CHANGED) {
                viewModel.notificationPolicyAccessGranted.value = profileManager.isNotificationPolicyAccessGranted()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        registerNotificationPolicyChangeReceiver()
        registerForRingtonePickerResult()
        registerForNotificationPolicyResult()
        registerForPhonePermissionResult()
        registerForSystemSettingsResult()
        callbacksDetails = requireActivity() as ProfileDetailsActivityCallback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("deprecation")
        vibrator = requireContext().getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    override fun getBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): CreateProfileFragmentBinding {
        return DataBindingUtil.inflate(inflater, R.layout.create_profile_fragment, container, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        viewBinding.viewModel = viewModel
        viewBinding.lifecycleOwner = viewLifecycleOwner
        return viewBinding.root
    }

    override fun onStart() {
        super.onStart()
        requireActivity().also {
            Intent(it, PlaybackService::class.java).also { intent ->
                it.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setNotificationPolicyAccessProperty()
        setCanWriteSettingsProperty()
        setPhonePermissionProperty()
    }

    override fun onStop() {
        super.onStop()
        if (requireActivity().isChangingConfigurations &&
            viewModel.isMediaPlaying())
        {
            viewModel.playerPosition = mediaService?.getCurrentPosition() ?: 0
            viewModel.resumePlayback = true
        }
        viewModel.stopPlayback()
        requireActivity().unbindService(serviceConnection)
        mediaService = null
    }

    override fun onDetach() {
        super.onDetach()
        callbacksDetails = null
        unregisterNotificationPolicyChangeReceiver()
        ringtoneActivityLauncher.unregister()
        notificationPolicyLauncher.unregister()
        systemPreferencesLauncher.unregister()
        phonePermissionLauncher.unregister()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        mediaService?.release()
        viewModel.setPlaybackState(viewModel.currentStreamType, false)
    }

    private fun onResumeRingtonePlayback(event: ResumeRingtonePlayback) {
        viewModel.currentMediaUri?.let { uri ->

            mediaService?.resume(
                this@ProfileDetailsFragment,
                uri,
                viewModel.currentStreamType,
                viewModel.getStreamVolume(viewModel.currentStreamType),
                event.position)

            viewModel.setPlaybackState(viewModel.currentStreamType, true)
        }
    }

    private fun onStopRingtonePlayback(event: StopRingtonePlayback) {
        mediaService?.release()
        viewModel.setPlaybackState(event.streamType, false)
    }

    private fun onStartRingtonePlayback(event: StartRingtonePlayback) {
        val vol: Int = viewModel.getStreamVolume(event.streamType)
        if (vol > 0) {

            viewModel.currentMediaUri = viewModel.getRingtoneUri(ringtoneMap[event.streamType]!!)
            viewModel.currentStreamType = event.streamType
            viewModel.currentMediaUri?.let { uri ->

                mediaService?.start(
                    this@ProfileDetailsFragment,
                    uri,
                    event.streamType,
                    viewModel.getStreamVolume(event.streamType))

                viewModel.setPlaybackState(event.streamType, true)
            }
        }
    }

    private fun onChangeRingerMode(event: ChangeRingerMode) {
        updateRingerMode(event.streamType)
        if (event.vibrate) {
            createVibrationEffect()
        }
        if (event.showSnackbar) {
            // TODO: implement
        }
    }

    private fun onRingerDisallowed(policyAllowsStream: Boolean) {
        if (!policyAllowsStream) {

            viewModel.ringVolume.value = 0
            viewModel.ringerMode.value = RINGER_MODE_SILENT

            viewModel.onStopRingtonePlayback(STREAM_RING)
        }
    }

    private fun onNotificationsDisallowed(policyAllowsStream: Boolean) {
        if (!policyAllowsStream) {

            viewModel.notificationVolume.value = 0
            viewModel.notificationMode.value = RINGER_MODE_SILENT

            viewModel.onStopRingtonePlayback(STREAM_NOTIFICATION)
        }
    }

    private fun onMediaDisallowed(policyAllowsStream: Boolean) {
        if (!policyAllowsStream) {
            viewModel.onStopRingtonePlayback(STREAM_MUSIC)
        }
    }

    private fun onAlarmsDisallowed(policyAllowsStream: Boolean) {
        if (!policyAllowsStream) {
            viewModel.onStopRingtonePlayback(STREAM_ALARM)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        callbacksDetails?.setNestedScrollingEnabled(true)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.fragmentEventsFlow.collect {
                        when (it) {

                            is ShowDialogFragment -> showDialogFragment(it.dialogType)
                            is ResumeRingtonePlayback -> onResumeRingtonePlayback(it)
                            is StopRingtonePlayback -> onStopRingtonePlayback(it)
                            is StartRingtonePlayback -> onStartRingtonePlayback(it)
                            is ChangeRingerMode -> onChangeRingerMode(it)
                            is StreamVolumeChanged -> changePlaybackVolume(it.streamType, it.volume)
                            is GetDefaultRingtoneUri -> setDefaultRingtoneUri(it.type)
                            is ChangeRingtoneEvent -> startRingtonePickerActivity(it.ringtoneType)

                            PhonePermissionRequestEvent -> phonePermissionLauncher.launch(READ_PHONE_STATE)
                            NotificationPolicyRequestEvent -> startNotificationPolicyActivity()
                            WriteSystemSettingsRequestEvent -> startSystemSettingsActivity()

                            ShowInterruptionFilterFragment -> callbacksDetails?.onFragmentReplace(INTERRUPTION_FILTER_FRAGMENT)
                            ShowNotificationRestrictionsFragment -> callbacksDetails?.onFragmentReplace(NOTIFICATION_RESTRICTIONS_FRAGMENT)
                            ShowPopupWindowEvent -> showPopupMenu()

                            else -> Log.i("EditProfileFragment", "unknown event")
                        }
                    }
                }
                launch {
                    viewModel.policyAllowsRingerStream.collect { policyAllowsStream ->
                        onRingerDisallowed(policyAllowsStream)
                    }
                }
                launch {
                    viewModel.policyAllowsNotificationsStream.collect { policyAllowsStream ->
                        onNotificationsDisallowed(policyAllowsStream)
                    }
                }
                launch {
                    viewModel.policyAllowsMediaStream.collect {
                        onMediaDisallowed(it)
                    }
                }
                launch {
                    viewModel.policyAllowsAlarmStream.collect {
                        onAlarmsDisallowed(it)
                    }
                }
            }
        }
    }

    private fun startSystemSettingsActivity() {
        systemPreferencesLauncher.launch(
            Intent(
                ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${requireContext().packageName}")
            )
        )
    }

    private fun startRingtonePickerActivity(type: Int) {
        val contract: RingtonePickerContract = ringtoneActivityLauncher.contract as RingtonePickerContract
        when (type) {
            TYPE_RINGTONE -> contract.existingUri = viewModel.phoneRingtoneUri.value
            TYPE_NOTIFICATION -> contract.existingUri = viewModel.notificationSoundUri.value
            TYPE_ALARM -> contract.existingUri = viewModel.alarmSoundUri.value
            else -> Log.i("EditProfileFragment", "unknown ringtone type")
        }
        ringtoneActivityLauncher.launch(type)
    }

    private fun startNotificationPolicyActivity() {
        notificationPolicyLauncher.launch(Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun setNotificationPolicyAccessProperty() {
        viewModel.notificationPolicyAccessGranted.value = profileManager.isNotificationPolicyAccessGranted()
    }

    private fun setPhonePermissionProperty() {
        viewModel.phonePermissionGranted.value = checkPermission(READ_PHONE_STATE)
    }

    private fun setCanWriteSettingsProperty() {
        viewModel.canWriteSettings.value = canWrite(requireContext())
    }

    private fun registerNotificationPolicyChangeReceiver() {
        requireActivity().registerReceiver(
            notificationPolicyReceiver,
            IntentFilter(ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED)
        )
    }

    private fun unregisterNotificationPolicyChangeReceiver() {
        requireActivity().unregisterReceiver(notificationPolicyReceiver)
    }

    private fun registerForSystemSettingsResult() {
        systemPreferencesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            canWrite(requireContext()).let {
                viewModel.canWriteSettings.value = it
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
                        TYPE_RINGTONE -> viewModel.phoneRingtoneUri.value = it
                        TYPE_NOTIFICATION -> viewModel.notificationSoundUri.value = it
                        TYPE_ALARM -> viewModel.alarmSoundUri.value = it
                        else -> Log.i("EditProfileFragment", "unknown ringtone type")
                    }
                }
            }
        }
    }

    private fun registerForNotificationPolicyResult() {
        notificationPolicyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            profileManager.isNotificationPolicyAccessGranted().also { granted ->
                viewModel.notificationPolicyAccessGranted.value = granted
                if (!granted) {
                    ViewUtil.showInterruptionPolicyAccessExplanation(requireActivity().supportFragmentManager)
                }
            }
        }
    }

    private fun registerForPhonePermissionResult() {
        phonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.phonePermissionGranted.value = it
            viewModel.streamsUnlinked.value = it
            if (!it) {
                ViewUtil.showPhoneStatePermissionExplanation(requireActivity().supportFragmentManager)
            }
        }
    }

    private fun setDefaultRingtoneUri(type: Int) {
        val uri: Uri? = getActualDefaultRingtoneUri(context, type)
        when (type) {
            TYPE_NOTIFICATION -> viewModel.setNotificationSoundUri(uri)
            TYPE_ALARM -> viewModel.setAlarmSoundUri(uri)
            TYPE_RINGTONE -> viewModel.setPhoneSoundUri(uri)
        }
    }

    @Suppress("deprecation")
    private fun createVibrationEffect() {
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

    private fun changePlaybackVolume(streamType: Int, vol: Int) {
        mediaService?.let {
            if (it.isPlaying() && streamType == viewModel.currentStreamType) {
                if (vol > 0) {
                    profileManager.setStreamVolume(streamType, vol, 0)
                } else {
                    viewModel.onStopRingtonePlayback(streamType)
                }
            }
        }
    }

    private fun updateRingerMode(streamType: Int, mode: Int) {
        if (streamType == STREAM_NOTIFICATION) {
            viewModel.notificationMode.value = mode
        } else if (streamType == STREAM_RING) {
            viewModel.ringerMode.value = mode
        }
    }

    private fun updateRingerMode(streamType: Int) {
        if (vibrator == null) {
            updateRingerMode(streamType, RINGER_MODE_SILENT)
            return
        }
        updateRingerMode(
            streamType,
            if (vibrator!!.hasVibrator()) RINGER_MODE_VIBRATE else RINGER_MODE_SILENT
        )
    }

    private fun showPopupMenu() {
        val popupMenu: PopupMenu = PopupMenu(requireContext(), viewBinding.interruptionFilterLayout)
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

    private fun showDialogFragment(dialogType: DialogType) {
        val dialogFragment: DialogFragment = when (dialogType) {
            SUPPRESSED_EFFECTS_OFF ->
                SuppressedEffectsOffDialog.newInstance(viewModel.getProfile())
            SUPPRESSED_EFFECTS_ON ->
                SuppressedEffectsOnDialog.newInstance(viewModel.getProfile())
            else -> throw IllegalArgumentException("Unknown dialog type")
        }
        dialogFragment.show(
            requireActivity().supportFragmentManager,
            null
        )
    }

    companion object {

        private const val EXTRA_PROFILE = "profile"
        private val ringtoneMap: Map<Int, Int> = mapOf(
            STREAM_ALARM to TYPE_ALARM,
            STREAM_NOTIFICATION to TYPE_NOTIFICATION,
            STREAM_RING to TYPE_RINGTONE,
            STREAM_MUSIC to TYPE_RINGTONE,
            STREAM_VOICE_CALL to TYPE_RINGTONE
        )

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
