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
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel
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
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.util.ProfileUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel.Event.*
import android.media.RingtoneManager.*
import android.provider.Settings
import androidx.lifecycle.*
import com.example.volumeprofiler.activities.ProfileDetailsActivity.Companion.INTERRUPTION_FILTER_FRAGMENT
import com.example.volumeprofiler.activities.ProfileDetailsActivity.Companion.TAG_PROFILE_FRAGMENT
import com.example.volumeprofiler.fragments.dialogs.PermissionExplanationDialog
import com.example.volumeprofiler.util.ViewUtil
import com.example.volumeprofiler.util.checkSelfPermission
import com.example.volumeprofiler.util.ui.animations.AnimUtil
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlin.math.abs

@SuppressLint("UseSwitchCompatOrMaterialCode")
@AndroidEntryPoint
class ProfileDetailsFragment: Fragment() {

    @Inject
    lateinit var profileUtil: ProfileUtil

    private val detailsViewModel: ProfileDetailsViewModel by activityViewModels()
    private var callbacks: EditProfileActivityCallbacks? = null

    private var _binding: CreateProfileFragmentBinding? = null
    private val binding: CreateProfileFragmentBinding get() = _binding!!

    private lateinit var ringtoneActivityLauncher: ActivityResultLauncher<Int>
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPolicyLauncher: ActivityResultLauncher<Intent>
    private lateinit var systemPreferencesLauncher: ActivityResultLauncher<Intent>
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>

    private var currentStreamType: Int = -1
    private var currentMediaUri: Uri? = null

    private var notificationPolicyReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_NOTIFICATION_POLICY_CHANGED) {
                detailsViewModel.notificationPolicyAccessGranted.value = profileUtil.isNotificationPolicyAccessGranted()
            }
        }
    }

    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        detailsViewModel.currentFragmentTag.value = TAG_PROFILE_FRAGMENT
        registerForRingtonePickerResult()
        registerForStoragePermissionResult()
        registerForNotificationPolicyResult()
        registerForPhonePermissionResult()
        registerForSystemSettingsResult()
        callbacks = requireActivity() as EditProfileActivityCallbacks
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (savedInstanceState != null) {

            val position: Int = savedInstanceState.getInt(EXTRA_CURRENT_POSITION, -1)
            currentStreamType = savedInstanceState.getInt(EXTRA_CURRENT_POSITION, -1)
            currentMediaUri = savedInstanceState.getParcelable(EXTRA_CURRENT_MEDIA_URI)

            currentMediaUri?.let {
                if (position != -1) {
                    resumeRingtonePlayback(it, currentStreamType, position)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.create_profile_fragment, container, false)
        binding.viewModel = detailsViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            outState.putParcelable(EXTRA_CURRENT_MEDIA_URI, currentMediaUri)
            outState.putInt(EXTRA_CURRENT_AUDIO_STREAM, currentStreamType)
            mediaPlayer?.currentPosition?.let { outState.putInt(EXTRA_CURRENT_POSITION, it) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        revealToolbarItems()

        val activityBinding = callbacks?.getBinding()!!
        activityBinding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (isVisible) {
                when {
                    abs(verticalOffset) == appBarLayout.totalScrollRange -> {
                        AnimUtil.scaleAnimation(activityBinding.menuEditNameButton, true)
                    }
                    verticalOffset == 0 -> {
                        AnimUtil.scaleAnimation(activityBinding.menuEditNameButton, false)
                    }
                }
            }
        })
        requireActivity().supportFragmentManager.setFragmentResultListener(
            PermissionExplanationDialog.PERMISSION_REQUEST_KEY, viewLifecycleOwner,
            { _, result ->
                if (result.getBoolean(PermissionExplanationDialog.EXTRA_RESULT_OK)) {
                    val permission: String = result.getString(PermissionExplanationDialog.EXTRA_PERMISSION)!!
                    if (permission == ACCESS_NOTIFICATION_POLICY) {
                        startNotificationPolicyActivity()
                    } else {
                        if (shouldShowRequestPermissionRationale(permission)) {
                            if (permission == READ_PHONE_STATE) {
                                phonePermissionLauncher.launch(READ_PHONE_STATE)
                            }
                            else {
                                storagePermissionLauncher.launch(READ_EXTERNAL_STORAGE)
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
                    detailsViewModel.fragmentEventsFlow.collect {
                        when (it) {
                            is StopRingtonePlayback -> {
                                mediaPlayer?.stop()
                                detailsViewModel.setPlaybackState(it.streamType, false)
                            }
                            is StartRingtonePlayback -> {
                                if (detailsViewModel.getStreamVolume(it.streamType) > 0) {
                                    startRingtonePlayback(it.streamType, it.volume)
                                    detailsViewModel.setPlaybackState(it.streamType, true)
                                }
                            }
                            is AlarmStreamVolumeChanged -> {
                                if (it.streamType == STREAM_ALARM) {
                                    if (it.volume < profileUtil.getAlarmStreamMinVolume()) {
                                        detailsViewModel.alarmVolume.value++
                                    } else {
                                        detailsViewModel.alarmVolume.value = it.volume
                                    }
                                } else if (it.streamType == STREAM_VOICE_CALL) {
                                    if (it.volume < profileUtil.getVoiceCallStreamMinVolume()) {
                                        detailsViewModel.callVolume.value++
                                    } else {
                                        detailsViewModel.callVolume.value = it.volume
                                    }
                                }
                                changePlaybackVolume(it.streamType, it.volume)
                            }
                            is ChangeRingerMode -> {
                                updateRingerMode(it.streamType)
                                if (it.vibrate) {
                                    createVibrateEffect()
                                }
                            }

                            is StreamVolumeChanged -> changePlaybackVolume(it.streamType, it.volume)
                            is GetDefaultRingtoneUri -> setDefaultRingtoneUri(it.type)
                            is ChangeRingtoneEvent -> startRingtonePickerActivity(it.ringtoneType)

                            WriteSystemSettingsRequestEvent -> startSystemSettingsActivity()
                            NavigateToNextFragment -> callbacks?.onFragmentReplace(INTERRUPTION_FILTER_FRAGMENT)
                            StoragePermissionRequestEvent -> storagePermissionLauncher.launch(READ_EXTERNAL_STORAGE)
                            PhonePermissionRequestEvent -> phonePermissionLauncher.launch(READ_PHONE_STATE)
                            ShowPopupWindowEvent -> showPopupMenu()
                            NotificationPolicyRequestEvent -> startNotificationPolicyActivity()

                            else -> Log.i("EditProfileFragment", "unknown event")
                        }
                    }
                }
                launch {
                    detailsViewModel.policyAllowsRingerStream.collect { policyAllowsStream ->
                        if (!policyAllowsStream) {
                            detailsViewModel.ringVolume.value = 0
                            detailsViewModel.ringerMode.value = RINGER_MODE_SILENT
                            if (currentStreamType == STREAM_RING) {
                                releaseMediaPlayer()
                                detailsViewModel.setPlaybackState(STREAM_RING, false)
                            }
                        }
                    }
                }
                launch {
                    detailsViewModel.policyAllowsNotificationsStream.collect { policyAllowsStream ->
                        if (!policyAllowsStream) {
                            detailsViewModel.notificationVolume.value = 0
                            detailsViewModel.notificationMode.value = RINGER_MODE_SILENT
                            if (currentStreamType == STREAM_NOTIFICATION) {
                                releaseMediaPlayer()
                                detailsViewModel.setPlaybackState(STREAM_NOTIFICATION, false)
                            }
                        }
                    }
                }
                launch {
                    detailsViewModel.policyAllowsMediaStream.collect {
                        if (!it && currentStreamType == STREAM_MUSIC) {
                            mediaPlayer?.stop()
                            detailsViewModel.setPlaybackState(STREAM_MUSIC, false)
                        }
                    }
                }
                launch {
                    detailsViewModel.policyAllowsAlarmStream.collect {
                        if (!it && currentStreamType == STREAM_ALARM) {
                            mediaPlayer?.stop()
                            detailsViewModel.setPlaybackState(STREAM_ALARM, false)
                        }
                    }
                }
            }
        }
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
        releaseMediaPlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
        ringtoneActivityLauncher.unregister()
        storagePermissionLauncher.unregister()
        notificationPolicyLauncher.unregister()
        systemPreferencesLauncher.unregister()
        phonePermissionLauncher.unregister()
    }

    private fun startSystemSettingsActivity(): Unit {
        systemPreferencesLauncher.launch(
            Intent(ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${requireActivity().packageName}"))
        )
    }

    private fun startRingtonePickerActivity(type: Int): Unit {
        val contract: RingtonePickerContract = ringtoneActivityLauncher.contract as RingtonePickerContract
        when (type) {
            TYPE_RINGTONE -> {
                contract.existingUri = detailsViewModel.phoneRingtoneUri.value
            }
            TYPE_NOTIFICATION -> {
                contract.existingUri = detailsViewModel.notificationSoundUri.value
            }
            TYPE_ALARM -> {
                contract.existingUri = detailsViewModel.alarmSoundUri.value
            }
            else -> Log.i("EditProfileFragment", "unknown ringtone type")
        }
        ringtoneActivityLauncher.launch(type)
    }

    private fun startNotificationPolicyActivity(): Unit {
        notificationPolicyLauncher.launch(Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun setStoragePermissionProperty(): Unit {
        val granted: Boolean = checkSelfPermission(requireContext(), READ_EXTERNAL_STORAGE)
        detailsViewModel.storagePermissionGranted.value = granted
        if (!granted) {
            detailsViewModel.notificationSoundUri.value = Uri.EMPTY
            detailsViewModel.alarmSoundUri.value = Uri.EMPTY
            detailsViewModel.phoneRingtoneUri.value = Uri.EMPTY
        }
    }

    private fun setPhonePermissionProperty(): Unit {
        detailsViewModel.phonePermissionGranted.value = checkSelfPermission(requireContext(), READ_PHONE_STATE)
    }

    private fun setNotificationPolicyProperty(): Unit {
        detailsViewModel.notificationPolicyAccessGranted.value = profileUtil.isNotificationPolicyAccessGranted()
    }

    private fun setCanWriteSettingsProperty(): Unit {
        detailsViewModel.canWriteSettings.value = profileUtil.canModifySystemPreferences()
    }

    private fun registerForStoragePermissionResult(): Unit {
        storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            detailsViewModel.storagePermissionGranted.value = it
            when {
                it -> {
                    callbacks?.updateMediaUris()
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
        systemPreferencesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val canWriteSettings: Boolean = Settings.System.canWrite(requireContext())
            detailsViewModel.canWriteSettings.value = canWriteSettings
            if (!canWriteSettings) {
                ViewUtil.showSystemSettingsPermissionExplanation(requireActivity().supportFragmentManager)
            }
        }
    }

    private fun registerForRingtonePickerResult(): Unit {
        val contract: RingtonePickerContract = RingtonePickerContract()
        ringtoneActivityLauncher = registerForActivityResult(contract) {
            if (it != null) {
                when (contract.ringtoneType) {
                    TYPE_RINGTONE -> {
                        detailsViewModel.phoneRingtoneUri.value = it
                        detailsViewModel.ringtoneUri = it
                    }
                    TYPE_NOTIFICATION -> {
                        detailsViewModel.notificationSoundUri.value = it
                        detailsViewModel.notificationUri = it
                    }
                    TYPE_ALARM -> {
                        detailsViewModel.alarmSoundUri.value = it
                        detailsViewModel.alarmUri = it
                    }
                    else -> Log.i("EditProfileFragment", "unknown ringtone type")
                }
            }
        }
    }

    private fun registerForNotificationPolicyResult(): Unit {
        notificationPolicyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val granted: Boolean = profileUtil.isNotificationPolicyAccessGranted()
            detailsViewModel.notificationPolicyAccessGranted.value = granted
            if (!granted) {
                ViewUtil.showInterruptionPolicyAccessExplanation(requireActivity().supportFragmentManager)
            }
        }
    }

    private fun registerForPhonePermissionResult(): Unit {
        phonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            detailsViewModel.phonePermissionGranted.value = it
            when {
                it -> detailsViewModel.streamsUnlinked.value = true
                else -> ViewUtil.showPhoneStatePermissionExplanation(requireActivity().supportFragmentManager)
            }
        }
    }

    private fun setDefaultRingtoneUri(type: Int): Unit {
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

    @Suppress("deprecation")
    private fun createVibrateEffect(): Unit {
        if (vibrator!!.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator?.vibrate(100)
            }
        }
    }

    private fun getRingtoneUri(type: Int): Uri {
        val uri: Uri = detailsViewModel.getRingtoneUri(type)
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
                detailsViewModel.setPlaybackState(streamType, false)
            }
        }
    }

    private fun prepareMediaPlayer(uri: Uri, streamType: Int): Unit {
        mediaPlayer?.reset()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setLegacyStreamType(streamType)
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
            detailsViewModel.setPlaybackState(streamType, false)
            releaseMediaPlayer()
        }
        mediaPlayer?.seekTo(position)
        mediaPlayer?.start()
        detailsViewModel.setPlaybackState(streamType, true)
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

        currentMediaUri = getRingtoneUri(ringtoneMap[streamType]!!)
        prepareMediaPlayer(currentMediaUri!!, streamType)

        mediaPlayer?.setOnCompletionListener {
            detailsViewModel.setPlaybackState(streamType, false)
            releaseMediaPlayer()
        }
        mediaPlayer?.start()
        currentStreamType = streamType
    }

    private fun updateRingerMode(streamType: Int, mode: Int): Unit {
        when (streamType) {
            STREAM_NOTIFICATION -> {
                detailsViewModel.notificationMode.value = mode
            }
            STREAM_RING -> {
                detailsViewModel.ringerMode.value = mode
            }
        }
    }

    private fun updateRingerMode(streamType: Int): Unit {
        if (vibrator!!.hasVibrator()) {
            updateRingerMode(streamType, RINGER_MODE_VIBRATE)
        } else {
            updateRingerMode(streamType, RINGER_MODE_SILENT)
        }
    }

    private fun showPopupMenu(): Unit {
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

    private fun revealToolbarItems(): Unit {
        val binding = callbacks?.getBinding()!!
        if (binding.menuSaveChangesButton.visibility != View.VISIBLE) {
            AnimUtil.scaleAnimation(binding.menuSaveChangesButton, true)
        }
    }

    companion object {

        private const val EXTRA_PROFILE = "profile"
        private const val EXTRA_CURRENT_POSITION: String = "position"
        private const val EXTRA_CURRENT_AUDIO_STREAM: String = "audio_stream"
        private const val EXTRA_CURRENT_MEDIA_URI: String = "uri"

        fun newInstance(profile: Profile?): ProfileDetailsFragment {
            val args: Bundle = Bundle()
            args.putParcelable(EXTRA_PROFILE, profile)
            return ProfileDetailsFragment().apply {
                if (profile != null) {
                    this.arguments = args
                }
            }
        }
    }
}
