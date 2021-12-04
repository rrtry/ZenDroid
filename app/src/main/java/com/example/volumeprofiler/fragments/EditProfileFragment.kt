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
import com.example.volumeprofiler.activities.customContract.RingtonePickerContract
import com.example.volumeprofiler.databinding.CreateProfileFragmentBinding
import com.example.volumeprofiler.entities.Profile
import android.app.NotificationManager.*
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.media.*
import android.media.AudioManager.*
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
import androidx.constraintlayout.widget.ConstraintSet
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.util.ProfileUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.volumeprofiler.viewmodels.EditProfileViewModel.Event.*
import android.media.RingtoneManager.*
import android.provider.Settings
import androidx.lifecycle.*
import com.example.volumeprofiler.activities.EditProfileActivity.Companion.DND_PREFERENCES_FRAGMENT
import com.example.volumeprofiler.fragments.dialogs.PermissionExplanationDialog
import com.example.volumeprofiler.util.ViewUtil
import com.example.volumeprofiler.util.checkSelfPermission
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

    private var currentStreamType: Int = -1
    private var currentMediaPlayerUri: Uri = Uri.EMPTY

    private var notificationPolicyReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_NOTIFICATION_POLICY_CHANGED) {
                viewModel.notificationPolicyAccessGranted.value = profileUtil.isNotificationPolicyAccessGranted()
            }
        }
    }

    private var hapticService: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            outState.putParcelable(EXTRA_CURRENT_MEDIA_URI, currentMediaPlayerUri)
            outState.putInt(EXTRA_CURRENT_AUDIO_STREAM, currentStreamType)
            mediaPlayer?.currentPosition?.let { outState.putInt(EXTRA_CURRENT_POSITION, it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hapticService = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (savedInstanceState != null) {
            val position: Int = savedInstanceState.getInt(EXTRA_CURRENT_POSITION)
            currentStreamType = savedInstanceState.getInt(EXTRA_CURRENT_POSITION)
            currentMediaPlayerUri = savedInstanceState.getParcelable(EXTRA_CURRENT_MEDIA_URI)!!

            resumeRingtonePlayback(currentMediaPlayerUri, currentStreamType, position)
        }
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

    private fun getUsageAttribute(streamType: Int): Int {
        return when (streamType) {
            STREAM_MUSIC -> AudioAttributes.USAGE_MEDIA
            STREAM_VOICE_CALL -> AudioAttributes.USAGE_VOICE_COMMUNICATION
            STREAM_NOTIFICATION -> AudioAttributes.USAGE_NOTIFICATION
            STREAM_RING -> AudioAttributes.USAGE_NOTIFICATION_RINGTONE
            STREAM_ALARM ->  AudioAttributes.USAGE_ALARM
            else -> AudioAttributes.USAGE_UNKNOWN
        }
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(notificationPolicyReceiver)
        releaseMediaPlayer()
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
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.fragmentEventsFlow.collect {
                        when (it) {

                            is StopRingtonePlayback -> {
                                mediaPlayer?.stop()
                                viewModel.setPlaybackState(it.streamType, false)
                            }

                            is StartRingtonePlayback -> {
                                if (viewModel.getStreamVolume(it.streamType) > 0) {
                                    startRingtonePlayback(it.streamType, it.volume)
                                    viewModel.setPlaybackState(it.streamType, true)
                                }
                            }

                            is StreamVolumeChanged -> {
                                changePlaybackVolume(it.streamType, it.volume)
                            }

                            is AlarmStreamVolumeChanged -> {
                                if (it.streamType == STREAM_ALARM) {
                                    if (it.volume < profileUtil.getAlarmStreamMinVolume()) {
                                        viewModel.alarmVolume.value++
                                    } else {
                                        viewModel.alarmVolume.value = it.volume
                                    }
                                } else if (it.streamType == STREAM_VOICE_CALL) {
                                    if (it.volume < profileUtil.getVoiceCallStreamMinVolume()) {
                                        viewModel.callVolume.value++
                                    } else {
                                        viewModel.callVolume.value = it.volume
                                    }
                                }
                                changePlaybackVolume(it.streamType, it.volume)
                            }

                            is GetDefaultRingtoneUri -> setDefaultRingtoneUri(it.type)

                            is ChangeRingerMode -> {
                                changeRingerMode(it.streamType)
                                if (it.vibrate) {
                                    createVibrateEffect()
                                }
                            }

                            is ChangeRingtoneEvent -> startRingtonePickerActivity(it.ringtoneType)

                            WriteSystemSettingsRequestEvent -> startSystemSettingsActivity()

                            NavigateToNextFragment -> callbacks?.onFragmentReplace(DND_PREFERENCES_FRAGMENT)

                            StoragePermissionRequestEvent -> requestStoragePermission()

                            PhonePermissionRequestEvent -> requestPhoneStatePermission()

                            ShowPopupWindowEvent -> showPopupMenu()

                            NotificationPolicyRequestEvent -> startNotificationPolicyActivity()

                            else -> Log.i("EditProfileFragment", "unknown event")
                        }
                    }
                }
                launch {
                    viewModel.policyAllowsRingerStream.collect { policyAllowsStream ->
                        if (!policyAllowsStream) {
                            viewModel.ringVolume.value = 0
                            viewModel.ringerMode.value = RINGER_MODE_SILENT
                            if (currentStreamType == STREAM_RING) {
                                releaseMediaPlayer()
                                viewModel.setPlaybackState(STREAM_RING, false)
                            }
                        }
                    }
                }
                launch {
                    viewModel.policyAllowsNotificationsStream.collect { policyAllowsStream ->
                        if (!policyAllowsStream) {
                            viewModel.notificationVolume.value = 0
                            viewModel.notificationMode.value = RINGER_MODE_SILENT
                            if (currentStreamType == STREAM_NOTIFICATION) {
                                releaseMediaPlayer()
                                viewModel.setPlaybackState(STREAM_NOTIFICATION, false)
                            }
                        }
                    }
                }
                launch {
                    viewModel.policyAllowsMediaStream.collect {
                        if (!it && currentStreamType == STREAM_MUSIC) {
                            mediaPlayer?.stop()
                            viewModel.setPlaybackState(STREAM_MUSIC, false)
                        }
                    }
                }
                launch {
                    viewModel.policyAllowsAlarmStream.collect {
                        if (!it && currentStreamType == STREAM_ALARM) {
                            mediaPlayer?.stop()
                            viewModel.setPlaybackState(STREAM_ALARM, false)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setStoragePermissionProperty(): Unit {
        val granted: Boolean = checkSelfPermission(requireContext(), READ_EXTERNAL_STORAGE)
        viewModel.storagePermissionGranted.value = granted
        if (!granted) {
            viewModel.notificationSoundUri.value = Uri.EMPTY
            viewModel.alarmSoundUri.value = Uri.EMPTY
            viewModel.phoneRingtoneUri.value = Uri.EMPTY
        }
    }

    private fun setPhonePermissionProperty(): Unit {
        viewModel.phonePermissionGranted.value = checkSelfPermission(requireContext(), READ_PHONE_STATE)
    }

    private fun setNotificationPolicyProperty(): Unit {
        viewModel.notificationPolicyAccessGranted.value = profileUtil.isNotificationPolicyAccessGranted()
    }

    private fun setCanWriteSettingsProperty(): Unit {
        viewModel.canWriteSettings.value = profileUtil.canModifySystemPreferences()
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

    private fun setDefaultRingtoneUri(type: Int): Unit {
        val uri: Uri = getActualDefaultRingtoneUri(context, type)
        when {
            viewModel.notificationSoundUri.value == Uri.EMPTY && type == TYPE_NOTIFICATION -> {
                viewModel.notificationSoundUri.value = uri
            }
            viewModel.alarmSoundUri.value == Uri.EMPTY && type == TYPE_ALARM -> {
                viewModel.alarmSoundUri.value = uri
            }
            viewModel.phoneRingtoneUri.value == Uri.EMPTY && type == TYPE_RINGTONE -> {
                viewModel.phoneRingtoneUri.value = uri
            }
        }
    }

    private fun updateRingtoneUris(): Unit {
        viewModel.notificationSoundUri.value =
            if (viewModel.notificationUri != Uri.EMPTY) viewModel.notificationUri else getActualDefaultRingtoneUri(context, TYPE_NOTIFICATION)
        viewModel.phoneRingtoneUri.value =
            if (viewModel.ringtoneUri != Uri.EMPTY) viewModel.ringtoneUri else getActualDefaultRingtoneUri(context, TYPE_RINGTONE)
        viewModel.alarmSoundUri.value =
            if (viewModel.alarmUri != Uri.EMPTY) viewModel.alarmUri else getActualDefaultRingtoneUri(context, TYPE_ALARM)
    }

    private fun registerForStoragePermissionResult(): Unit {
        storagePermissionCallback = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.storagePermissionGranted.value = it
            when {
                it -> {
                    updateRingtoneUris()
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
                        viewModel.ringtoneUri = it
                    }
                    TYPE_NOTIFICATION -> {
                        viewModel.notificationSoundUri.value = it
                        viewModel.notificationUri = it
                    }
                    TYPE_ALARM -> {
                        viewModel.alarmSoundUri.value = it
                        viewModel.alarmUri = it
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
                contract.existingUri = viewModel.phoneRingtoneUri.value
            }
            TYPE_NOTIFICATION -> {
                contract.existingUri = viewModel.notificationSoundUri.value
            }
            TYPE_ALARM -> {
                contract.existingUri = viewModel.alarmSoundUri.value
            }
            else -> Log.i("EditProfileFragment", "unknown ringtone type")
        }
        ringtoneActivityCallback.launch(type)
    }

    private fun startNotificationPolicyActivity(): Unit {
        notificationPolicyCallback.launch(Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun getDisplayDensity(): Float {
        return resources.displayMetrics.density
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
                hapticService?.vibrate(VibrationEffect.createOneShot(100, 155))
            } else {
                hapticService?.vibrate(100)
            }
        }
    }

    private fun getRingtoneUri(type: Int): Uri {
        val uri: Uri = viewModel.getRingtoneUri(type)
        return if (uri == Uri.EMPTY) {
            profileUtil.getDefaultRingtoneUri(type)
        } else {
            uri
        }
    }

    private fun releaseMediaPlayer(): Unit {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun changePlaybackVolume(streamType: Int, vol: Int): Unit {
        if (mediaPlayer?.isPlaying == true && streamType == currentStreamType) {
            if (vol > 0) {
                profileUtil.setStreamVolume(streamType, vol, 0)
            } else {
                mediaPlayer?.stop()
                viewModel.setPlaybackState(streamType, false)
            }
        }
    }

    private fun prepareMediaPlayer(uri: Uri, streamType: Int): Unit {
        mediaPlayer?.reset()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(getUsageAttribute(streamType))
                .build()
            )
            setVolume(1f, 1f)
            setDataSource(requireContext(), uri)
            prepare()
        }
    }

    private fun resumeRingtonePlayback(uri: Uri, streamType: Int, position: Int): Unit {
        prepareMediaPlayer(uri, streamType)
        mediaPlayer?.setOnCompletionListener {
            viewModel.setPlaybackState(streamType, false)
            releaseMediaPlayer()
        }
        mediaPlayer?.seekTo(position)
        mediaPlayer?.start()
        viewModel.setPlaybackState(streamType, true)
    }

    private fun startRingtonePlayback(streamType: Int, volume: Int): Unit {

        profileUtil.setStreamVolume(streamType, volume, 0)

        val ringtoneMap: Map<Int, Int> = mapOf(
            STREAM_ALARM to TYPE_ALARM,
            STREAM_NOTIFICATION to TYPE_NOTIFICATION,
            STREAM_RING to TYPE_RINGTONE,
            STREAM_MUSIC to TYPE_RINGTONE,
            STREAM_VOICE_CALL to TYPE_RINGTONE
        )

        currentMediaPlayerUri = getRingtoneUri(ringtoneMap[streamType]!!)
        prepareMediaPlayer(currentMediaPlayerUri, streamType)

        mediaPlayer?.setOnCompletionListener {
            viewModel.setPlaybackState(streamType, false)
            releaseMediaPlayer()
        }
        mediaPlayer?.start()
        currentStreamType = streamType
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
        private const val EXTRA_CURRENT_POSITION: String = "position"
        private const val LOG_TAG = "EditProfileFragment"
        private const val EXTRA_CURRENT_AUDIO_STREAM: String = "audio_stream"
        private const val EXTRA_CURRENT_MEDIA_URI: String = "uri"

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
