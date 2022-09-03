package ru.rrtry.silentdroid.ui.fragments

import android.Manifest.permission.*
import android.app.NotificationManager
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
import android.content.Context.NOTIFICATION_SERVICE
import android.media.*
import android.media.AudioManager.*
import android.net.Uri
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
import ru.rrtry.silentdroid.interfaces.ProfileDetailsActivityCallback
import dagger.hilt.android.AndroidEntryPoint
import android.media.RingtoneManager.*
import android.os.*
import android.provider.Settings.System.canWrite
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.*
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionSet
import com.google.android.material.snackbar.Snackbar
import ru.rrtry.silentdroid.ui.activities.ProfileDetailsActivity.Companion.INTERRUPTION_FILTER_FRAGMENT
import ru.rrtry.silentdroid.services.RingtonePlaybackService
import ru.rrtry.silentdroid.ui.activities.ProfileDetailsActivity.Companion.NOTIFICATION_RESTRICTIONS_FRAGMENT
import ru.rrtry.silentdroid.util.ViewUtil
import ru.rrtry.silentdroid.util.checkPermission
import kotlinx.coroutines.launch
import ru.rrtry.silentdroid.core.AppRingtoneManager
import ru.rrtry.silentdroid.core.AppVibrator
import ru.rrtry.silentdroid.core.externalInterruptionPolicyAllowsStream
import ru.rrtry.silentdroid.core.getStreamMutedStringRes
import ru.rrtry.silentdroid.databinding.CreateProfileFragmentBinding
import ru.rrtry.silentdroid.util.openPackageInfoActivity
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel.ViewEvent.*
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel.*
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel.DialogType.*
import javax.inject.Inject

@AndroidEntryPoint
class ProfileDetailsFragment: ViewBindingFragment<CreateProfileFragmentBinding>(), MediaPlayer.OnCompletionListener {

    @Inject lateinit var ringtoneManager: AppRingtoneManager
    @Inject lateinit var vibrator: AppVibrator

    private val viewModel: ProfileDetailsViewModel by activityViewModels()

    private lateinit var notificationManager: NotificationManager

    private lateinit var ringtoneActivityLauncher: ActivityResultLauncher<Int>
    private lateinit var notificationPolicyLauncher: ActivityResultLauncher<Intent>
    private lateinit var systemPreferencesLauncher: ActivityResultLauncher<Intent>
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<String>

    private var callback: ProfileDetailsActivityCallback? = null
    private val savePlayerPosition: Boolean
        get() {
            return requireActivity().isChangingConfigurations &&
                    viewModel.isRingtonePlaying()
    }

    private var mediaService: RingtonePlaybackService? = null
    private val serviceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            (service as RingtonePlaybackService.LocalBinder).apply {
                mediaService = getService()
                viewModel.resumeRingtonePlayback()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            viewModel.setPlaybackState(
                viewModel.getPlayingStream(),
                false
            )
            mediaService = null
        }
    }

    private val notificationPolicyReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_NOTIFICATION_POLICY_CHANGED ||
                intent?.action == ACTION_INTERRUPTION_FILTER_CHANGED)
            {
                val playingStream: Int = viewModel.getPlayingStream()
                if (playingStream == -1) return

                if (!externalInterruptionPolicyAllowsStream(
                        viewModel.getPlayingStream(),
                        notificationManager.currentInterruptionFilter,
                        notificationManager.notificationPolicy))
                {
                    viewModel.onStopRingtonePlayback(playingStream)
                }
            }
        }
    }

    private val lifecycleObserver: DefaultLifecycleObserver = object : DefaultLifecycleObserver {

        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            viewModel.storagePermissionGranted.value = checkPermission(READ_EXTERNAL_STORAGE)
            viewModel.phonePermissionGranted.value = checkPermission(READ_PHONE_STATE)
            viewModel.notificationPolicyAccessGranted.value = notificationManager.isNotificationPolicyAccessGranted
            viewModel.canWriteSettings.value = canWrite(requireContext())
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            if (savePlayerPosition) {
                viewModel.savePlayerPosition(
                    mediaService?.getCurrentPosition() ?: 0
                )
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            viewModel.stopPlayback()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireActivity() as ProfileDetailsActivityCallback
        registerForRingtonePickerResult()
        registerForNotificationPolicyResult()
        registerForPhonePermissionResult()
        registerForStoragePermissionResult()
        registerForSystemSettingsResult()
    }

    @Suppress("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TransitionSet().apply {

            addTransition(Fade())
            addTransition(Slide(Gravity.START))

            enterTransition = this
            exitTransition = this
        }
        notificationManager = requireContext().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
        viewLifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        return viewBinding.root
    }

    override fun onResume() {
        super.onResume()
        registerNotificationPolicyChangeReceiver()
    }

    override fun onStart() {
        super.onStart()
        requireContext().also { context ->
            Intent(context, RingtonePlaybackService::class.java).also { intent ->
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterNotificationPolicyChangeReceiver()
    }

    override fun onStop() {
        super.onStop()
        requireContext().unbindService(serviceConnection)
        mediaService = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
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
        val uri: Uri = viewModel.getRingtoneUri(ringtones[event.streamType]!!)

        if (!externalInterruptionPolicyAllowsStream(
                event.streamType,
                notificationManager.currentInterruptionFilter,
                notificationManager.notificationPolicy))
        {
            ViewUtil.showSnackbar(
                viewBinding.constraintRoot,
                resources.getString(getStreamMutedStringRes(event.streamType, true)),
                Snackbar.LENGTH_LONG,
                resources.getString(R.string.unmute))
            {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(
                        INTERRUPTION_FILTER_ALL
                    )
                }
            }
            return
        }

        viewModel.currentMediaUri = uri
        viewModel.currentStreamType = event.streamType

        mediaService?.start(
            this@ProfileDetailsFragment,
            uri,
            event.streamType,
            vol)
        viewModel.setPlaybackState(event.streamType, true)
    }

    private fun onChangeRingerMode(event: ChangeRingerMode) {

        var ringerMode: Int = if (vibrator.hasVibrator) RINGER_MODE_VIBRATE else RINGER_MODE_SILENT

        when (event.streamType) {
            STREAM_RING -> {
                viewModel.silenceRinger(ringerMode)
            }
            STREAM_NOTIFICATION -> {
                if (event.hasSeparateNotificationStream) ringerMode = RINGER_MODE_SILENT
                viewModel.silenceNotifications(ringerMode)
            }
        }
        if (ringerMode == RINGER_MODE_VIBRATE) vibrator.createVibrationEffect()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        callback?.setNestedScrollingEnabled(true)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.fragmentEventsFlow.collect { event ->
                        when (event) {

                            is ShowStreamMutedSnackbar -> showStreamMutedSnackbar(event)
                            is ShowDialogFragment -> showDialogFragment(event.dialogType)
                            is ResumeRingtonePlayback -> onResumeRingtonePlayback(event)
                            is StopRingtonePlayback -> onStopRingtonePlayback(event)
                            is StartRingtonePlayback -> onStartRingtonePlayback(event)
                            is ChangeRingerMode -> onChangeRingerMode(event)
                            is StreamVolumeChanged -> changePlaybackVolume(event.streamType, event.volume)
                            is GetDefaultRingtoneUri -> setDefaultRingtoneUri(event.type)
                            is ChangeRingtoneEvent -> startRingtonePickerActivity(event.ringtoneType)
                            is StoragePermissionRequestEvent -> storagePermissionLauncher.launch(READ_EXTERNAL_STORAGE)
                            is PhonePermissionRequestEvent -> phonePermissionLauncher.launch(READ_PHONE_STATE)
                            is NotificationPolicyRequestEvent -> startNotificationPolicyActivity()
                            is WriteSystemSettingsRequestEvent -> startSystemSettingsActivity()
                            is StartPermissionsActivity -> context?.openPackageInfoActivity()
                            is ShowInterruptionFilterFragment -> callback?.onFragmentReplace(INTERRUPTION_FILTER_FRAGMENT)
                            is ShowNotificationRestrictionsFragment -> callback?.onFragmentReplace(NOTIFICATION_RESTRICTIONS_FRAGMENT)
                            is ShowPopupWindowEvent -> showPopupMenu()

                            else -> Log.i("EditProfileFragment", "unknown event")
                        }
                    }
                }
                launch {
                    viewModel.policyAllowsRingerStream.collect { policyAllowsStream ->
                        if (!policyAllowsStream) viewModel.onStopRingtonePlayback(STREAM_RING)
                    }
                }
                launch {
                    viewModel.policyAllowsNotificationsStream.collect { policyAllowsStream ->
                        if (!policyAllowsStream) viewModel.onStopRingtonePlayback(STREAM_NOTIFICATION)
                    }
                }
                launch {
                    viewModel.policyAllowsMediaStream.collect { policyAllowsStream ->
                        if (!policyAllowsStream) viewModel.onStopRingtonePlayback(STREAM_MUSIC)
                    }
                }
                launch {
                    viewModel.policyAllowsAlarmStream.collect { policyAllowsSteam ->
                        if (!policyAllowsSteam) viewModel.onStopRingtonePlayback(STREAM_ALARM)
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
            TYPE_RINGTONE -> contract.uri = viewModel.phoneRingtoneUri.value
            TYPE_NOTIFICATION -> contract.uri = viewModel.notificationSoundUri.value
            TYPE_ALARM -> contract.uri = viewModel.alarmSoundUri.value
            else -> Log.i("EditProfileFragment", "unknown ringtone type")
        }
        ringtoneActivityLauncher.launch(type)
    }

    private fun startNotificationPolicyActivity() {
        notificationPolicyLauncher.launch(Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun registerNotificationPolicyChangeReceiver() {
        requireContext().registerReceiver(
            notificationPolicyReceiver,
            IntentFilter().apply {
                addAction(ACTION_NOTIFICATION_POLICY_CHANGED)
                addAction(ACTION_INTERRUPTION_FILTER_CHANGED)
            }
        )
    }

    private fun unregisterNotificationPolicyChangeReceiver() {
        requireContext().unregisterReceiver(notificationPolicyReceiver)
    }

    private fun registerForSystemSettingsResult() {
        systemPreferencesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            canWrite(requireContext()).let { granted ->
                viewModel.canWriteSettings.value = granted
                if (!granted) ViewUtil.showSystemSettingsPermissionExplanation(requireActivity().supportFragmentManager)
            }
        }
    }

    private fun registerForRingtonePickerResult() {
        RingtonePickerContract().apply {
            ringtoneActivityLauncher = registerForActivityResult(this) { uri ->
                if (uri == null) return@registerForActivityResult
                when (ringtoneType) {
                    TYPE_RINGTONE -> viewModel.phoneRingtoneUri.value = uri
                    TYPE_NOTIFICATION -> viewModel.notificationSoundUri.value = uri
                    TYPE_ALARM -> viewModel.alarmSoundUri.value = uri
                }
            }
        }
    }

    private fun registerForNotificationPolicyResult() {
        notificationPolicyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            notificationManager.isNotificationPolicyAccessGranted.also { granted ->
                viewModel.notificationPolicyAccessGranted.value = granted
                if (!granted) ViewUtil.showInterruptionPolicyAccessExplanation(requireActivity().supportFragmentManager)
            }
        }
    }

    private fun registerForStoragePermissionResult() {
        storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.storagePermissionGranted.value = granted
            if (!granted) ViewUtil.showStoragePermissionExplanation(requireActivity().supportFragmentManager)
        }
    }

    private fun registerForPhonePermissionResult() {
        phonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.phonePermissionGranted.value = granted
            viewModel.streamsUnlinked.value = granted
            if (!granted) ViewUtil.showPhoneStatePermissionExplanation(requireActivity().supportFragmentManager)
        }
    }

    private fun setDefaultRingtoneUri(type: Int) {
        val uri: Uri = ringtoneManager.getDefaultRingtoneUri(type)
        when (type) {
            TYPE_NOTIFICATION -> viewModel.setDefaultNotificationSoundUri(uri)
            TYPE_ALARM -> viewModel.setDefaultAlarmSoundUri(uri)
            TYPE_RINGTONE -> viewModel.setDefaultRingtoneUri(uri)
        }
    }

    private fun changePlaybackVolume(streamType: Int, vol: Int) {
        mediaService?.let { player ->
            if (player.isPlaying() &&
                streamType == viewModel.currentStreamType)
            {
                if (vol > 0) {
                    mediaService?.setStreamVolume(streamType, vol, 0)
                } else {
                    viewModel.onStopRingtonePlayback(streamType)
                }
            }
        }
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
            SUPPRESSED_EFFECTS_OFF -> SuppressedEffectsOffDialog.newInstance(viewModel.getProfile())
            SUPPRESSED_EFFECTS_ON -> SuppressedEffectsOnDialog.newInstance(viewModel.getProfile())
            else -> throw IllegalArgumentException("Unknown dialog type")
        }
        dialogFragment.show(
            requireActivity().supportFragmentManager,
            null
        )
    }

    private fun showStreamMutedSnackbar(event: ShowStreamMutedSnackbar) {
        ViewUtil.showSnackbar(
            viewBinding.constraintRoot,
            resources.getString(getStreamMutedStringRes(event.streamType, false)),
            Snackbar.LENGTH_LONG
        )
    }

    companion object {

        private const val EXTRA_PROFILE = "profile"

        private val ringtones: Map<Int, Int> = mapOf(
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
