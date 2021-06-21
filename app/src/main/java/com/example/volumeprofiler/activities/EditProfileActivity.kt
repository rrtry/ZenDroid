package com.example.volumeprofiler.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.example.volumeprofiler.*
import com.example.volumeprofiler.fragments.ApplyChangesDialog
import com.example.volumeprofiler.interfaces.ApplyChangesDialogCallbacks
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import com.example.volumeprofiler.viewmodels.EditProfileViewModel

@SuppressLint("UseSwitchCompatOrMaterialCode")
class EditProfileActivity: AppCompatActivity(), ApplyChangesDialogCallbacks {

    private val viewModel: EditProfileViewModel by viewModels()
    private lateinit var editText: EditText
    private lateinit var mediaSeekBar: SeekBar
    private lateinit var phoneSeekBar: SeekBar
    private lateinit var notificationSeekBar: SeekBar
    private lateinit var ringerSeekBar: SeekBar
    private lateinit var alarmSeekBar: SeekBar
    private lateinit var phoneRingtoneLayout: LinearLayout
    private lateinit var notificationSoundLayout: LinearLayout
    private lateinit var alarmSoundLayout: LinearLayout
    private lateinit var doNotDisturbLayout: RelativeLayout
    private lateinit var phoneRingtoneTitle: TextView
    private lateinit var notificationSoundTitle: TextView
    private lateinit var alarmSoundTitle: TextView
    //private lateinit var screenLockingSoundSwitch: Switch
    //private lateinit var chargingSoundAndVibrationSwitch: Switch
    //private lateinit var touchSoundSwitch: Switch
    //private lateinit var touchVibrationSwitch: Switch
    //private lateinit var shutterSoundSwitch: Switch
    //private lateinit var dialTonesSwitch: Switch
    private var profilesAndEvents: List<ProfileAndEvent>? = null

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (menu != null) {
            val drawable: Drawable?
            val item: MenuItem = menu.findItem(R.id.saveChangesButton)
            return if (intent.extras?.get(EXTRA_UUID) != null) {
                drawable = ResourcesCompat.getDrawable(resources, android.R.drawable.ic_menu_save, null)
                item.icon = drawable
                true
            } else {
                drawable = ResourcesCompat.getDrawable(resources, android.R.drawable.ic_menu_add, null)
                item.icon = drawable
                true
            }
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if (item.itemId == R.id.saveChangesButton) {
            onApply()
            return true
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        return if (menu != null) {
            menuInflater.inflate(R.menu.action_menu_save_changes, menu)
            true
        }
        else {
            false
        }
    }

    private fun hideSoftInput(): Unit {
        val inputManager: InputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(editText.windowToken, 0)
        editText.clearFocus()
        editText.isCursorVisible = false
    }

    private fun initializeViews(): Unit {
        phoneRingtoneLayout = findViewById(R.id.phoneRingtoneLayout)
        notificationSoundLayout = findViewById(R.id.notificationSoundLayout)
        alarmSoundLayout = findViewById(R.id.alarmSoundLayout)
        doNotDisturbLayout = findViewById(R.id.doNotDisturbLayout)
        editText = findViewById(R.id.profileName)
        mediaSeekBar = findViewById(R.id.mediaSeekBar)
        phoneSeekBar = findViewById(R.id.phoneSeekBar)
        notificationSeekBar = findViewById(R.id.notificationSeekBar)
        ringerSeekBar = findViewById(R.id.ringerSeekBar)
        alarmSeekBar = findViewById(R.id.alarmSeekBar)

        phoneRingtoneTitle = findViewById(R.id.currentPhoneRingtone)
        alarmSoundTitle = findViewById(R.id.currentAlarmSound)
        notificationSoundTitle = findViewById(R.id.currentNotificationSound)
        if (intent.extras?.get(EXTRA_UUID) != null) {
            phoneRingtoneTitle.text = getRingtoneTitle(viewModel.mutableProfile!!.phoneRingtoneUri)
            notificationSoundTitle.text = getRingtoneTitle(viewModel.mutableProfile!!.notificationSoundUri)
            alarmSoundTitle.text = getRingtoneTitle(viewModel.mutableProfile!!.alarmSoundUri)
        }
        else {
            if (viewModel.mutableProfile!!.phoneRingtoneUri == Uri.EMPTY) {
                viewModel.mutableProfile!!.phoneRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(
                        this, RingtoneManager.TYPE_RINGTONE
                )
            }
            else if (viewModel.mutableProfile!!.alarmSoundUri == Uri.EMPTY) {
                viewModel.mutableProfile!!.alarmSoundUri = RingtoneManager.getActualDefaultRingtoneUri(
                        this, RingtoneManager.TYPE_ALARM
                )
            }
            else if (viewModel.mutableProfile!!.notificationSoundUri == Uri.EMPTY) {
                viewModel.mutableProfile!!.notificationSoundUri = RingtoneManager.getActualDefaultRingtoneUri(
                        this, RingtoneManager.TYPE_NOTIFICATION
                )
            }
            phoneRingtoneTitle.text = getRingtoneTitle(viewModel.mutableProfile!!.phoneRingtoneUri)
            alarmSoundTitle.text = getRingtoneTitle(viewModel.mutableProfile!!.alarmSoundUri)
            notificationSoundTitle.text = getRingtoneTitle(viewModel.mutableProfile!!.notificationSoundUri)
        }
        //screenLockingSoundSwitch = findViewById(R.id.screen_locking_sounds_switch)
        //chargingSoundAndVibrationSwitch = findViewById(R.id.charging_sounds_and_vibration_switch)
        //touchSoundSwitch = findViewById(R.id.touch_sounds_switch)
        //touchVibrationSwitch = findViewById(R.id.touch_vibration_switch)
        //dialTonesSwitch = findViewById(R.id.dial_pad_tones_switch)
    }

    private fun getRingtoneTitle(uri: Uri): String {
        val ringtone: Ringtone = RingtoneManager.getRingtone(this, uri)
        return ringtone.getTitle(this)
    }

    private fun setViewCallbacks(): Unit {
        val seekBarArray: Array<SeekBar> = arrayOf(mediaSeekBar, phoneSeekBar, notificationSeekBar, ringerSeekBar, alarmSeekBar)
        /*
        val switchArray: Array<Switch> = arrayOf(screenLockingSoundSwitch, chargingSoundAndVibrationSwitch, touchSoundSwitch,
                touchVibrationSwitch, dialTonesSwitch)
         */
        for (element in seekBarArray) {
            element.setOnSeekBarChangeListener(SeekBarChangeListener())
        }
        /*
        for (element in switchArray) {
            element.setOnCheckedChangeListener(SwitchChangeListener())
        }
         */
        editText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                editText.isCursorVisible = true
            }
        }
        editText.setOnKeyListener(object : View.OnKeyListener {

            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {

                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    hideSoftInput()
                    return true
                }
                return false
            }
        })
        phoneRingtoneLayout.setOnClickListener {
            val intent: Intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                this.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            }
            startActivityForResult(intent, REQUEST_CODE_RINGTONE)
        }
        notificationSoundLayout.setOnClickListener {
            val intent: Intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                this.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            }
            startActivityForResult(intent, REQUEST_CODE_NOTIFICATION)
        }
        alarmSoundLayout.setOnClickListener {
            val intent: Intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                this.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            }
            startActivityForResult(intent, REQUEST_CODE_ALARM)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            when (requestCode) {

                REQUEST_CODE_RINGTONE -> {
                    viewModel.mutableProfile!!.phoneRingtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)!!
                }
                REQUEST_CODE_NOTIFICATION -> {
                    viewModel.mutableProfile!!.notificationSoundUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)!!
                }
                REQUEST_CODE_ALARM -> {
                    viewModel.mutableProfile!!.alarmSoundUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)!!
                }
            }
        }
    }

    private fun setLiveDataObservers(): Unit {
        viewModel.profileLiveData.observe(this, androidx.lifecycle.Observer {
            if (it != null) {
                if (viewModel.mutableProfile == null) {
                    viewModel.mutableProfile = it
                }
                updateUI()
            }
        })
        viewModel.profileAndEventLiveData.observe(this, androidx.lifecycle.Observer {
            if (it != null) {
                profilesAndEvents = it
            }
        })
    }

    @SuppressWarnings("unchecked")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(LOG_TAG, "changedMade: ${viewModel.changesMade}")
        setContentView(R.layout.create_profile)
        if (intent.extras?.get(EXTRA_UUID) != null) {
            supportActionBar?.title = "Edit profile"
            viewModel.setProfile(intent.extras?.get(EXTRA_UUID) as UUID)
        } else {
            if (viewModel.mutableProfile == null) {
                viewModel.mutableProfile = Profile("Profile")
                viewModel.changesMade = true
            }
            supportActionBar?.title = "Create profile"
        }
        initializeViews()
        setViewCallbacks()
        setLiveDataObservers()
    }

    private fun applyAudioSettings(): Unit {
        val sharedPreferencesUtil = SharedPreferencesUtil.getInstance()
        if (sharedPreferencesUtil.getActiveProfileId()
                == viewModel.mutableProfile!!.id.toString()) {
            val profileUtil: ProfileUtil = ProfileUtil.getInstance()
            val settingsPair = ProfileUtil.getVolumeSettingsMapPair(viewModel.mutableProfile!!)
            profileUtil.applyAudioSettings(settingsPair.first, settingsPair.second, viewModel.mutableProfile!!.id, viewModel.mutableProfile!!.title)
        }
    }

    private fun saveChanges(): Unit {
        val profile: Profile = viewModel.mutableProfile!!
        profile.title = editText.text.toString()
        if (viewModel.mutableProfile!!.callVolume == 0) {
            viewModel.mutableProfile!!.callVolume++
        }
        if (profilesAndEvents != null) {
            for (i in profilesAndEvents!!) {
                val event: Event = i.event
                setAlarm(event, profile)
            }
        }
        if (viewModel.mutableProfile != null) {
            applyAudioSettings()
            if (intent.extras?.get(EXTRA_UUID) == null) {
                viewModel.addProfile(profile)
            }
            else {
                viewModel.updateProfile(profile)
            }
        }
    }

    private fun setAlarm(event: Event, profile: Profile): Unit {
        val eventOccurrences: Array<Int> = event.workingDays.split("").slice(1..event.workingDays.length).map { it.toInt() }.toTypedArray()
        val volumeSettingsMap: Pair<Map<Int, Int>, Map<String, Int>> = ProfileUtil.getVolumeSettingsMapPair(profile)
        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
        alarmUtil.setAlarm(volumeSettingsMap, eventOccurrences,
                event.localDateTime, event.eventId, false, profile.id, profile.title)
    }

    private fun updateUI() {
        editText.text = Editable.Factory.getInstance().newEditable(viewModel.mutableProfile!!.title)
        mediaSeekBar.progress = viewModel.mutableProfile!!.mediaVolume
        phoneSeekBar.progress = viewModel.mutableProfile!!.callVolume
        notificationSeekBar.progress = viewModel.mutableProfile!!.notificationVolume
        ringerSeekBar.progress = viewModel.mutableProfile!!.ringVolume
        alarmSeekBar.progress = viewModel.mutableProfile!!.alarmVolume
        /*
        screenLockingSoundSwitch.isChecked = viewModel.mutableProfile!!.screenLockingSounds != 0
        chargingSoundAndVibrationSwitch.isChecked = viewModel.mutableProfile!!.chargingSoundsAndVibration != 0
        touchSoundSwitch.isChecked = viewModel.mutableProfile!!.touchSounds != 0
        touchVibrationSwitch.isChecked = viewModel.mutableProfile!!.touchVibration != 0
        shutterSoundSwitch.isChecked = viewModel.mutableProfile!!.shutterSound != 0
        dialTonesSwitch.isChecked = viewModel.mutableProfile!!.dialTones != 0
         */
    }

    /*
    private inner class SwitchChangeListener: CompoundButton.OnCheckedChangeListener {

        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            if (editText.hasFocus()) {
                hideSoftInput()
            }
            if (buttonView != null && buttonView.isPressed) {
                viewModel.changesMade = true
                buttonView.isChecked = isChecked

                when (buttonView) {

                    screenLockingSoundSwitch -> {
                        viewModel.mutableProfile!!.screenLockingSounds = if (isChecked) 1 else 0
                    }

                    chargingSoundAndVibrationSwitch -> {
                        viewModel.mutableProfile!!.chargingSoundsAndVibration = if (isChecked) 1 else 0
                    }

                    touchSoundSwitch -> {
                        viewModel.mutableProfile!!.touchSounds = if (isChecked) 1 else 0
                    }

                    touchVibrationSwitch -> {
                        viewModel.mutableProfile!!.touchVibration = if (isChecked) 1 else 0
                    }

                    dialTonesSwitch -> {
                        viewModel.mutableProfile!!.dialTones = if (isChecked) 1 else 0
                    }
                }
            }
        }
    }
     */

    private inner class SeekBarChangeListener: SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (seekBar != null && fromUser) {
                viewModel.changesMade = true
                seekBar.progress = progress
                when (seekBar) {

                    mediaSeekBar -> {
                        viewModel.mutableProfile!!.mediaVolume = progress
                    }

                    phoneSeekBar -> {
                        viewModel.mutableProfile!!.callVolume = progress
                    }

                    notificationSeekBar -> {
                        viewModel.mutableProfile!!.notificationVolume = progress
                    }

                    ringerSeekBar -> {
                        viewModel.mutableProfile!!.ringVolume = progress
                    }

                    alarmSeekBar -> {
                        viewModel.mutableProfile!!.alarmVolume = progress
                    }
                }
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            if (editText.hasFocus()) {
                hideSoftInput()
            }
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            Log.i(LOG_TAG, "onStopTrackingTouch()")
        }
    }

    override fun onBackPressed() {
        if (viewModel.changesMade) {
            val fragment: ApplyChangesDialog = ApplyChangesDialog()
            fragment.show(supportFragmentManager, null)
        }
        else {
            super.onBackPressed()
        }
    }

    override fun onApply() {
        saveChanges()
        super.onBackPressed()
    }

    override fun onDismiss() {
        super.onBackPressed()
    }
    
    companion object {

        private const val EXTRA_UUID = "uuid"
        private const val LOG_TAG = "EditProfileActivity"
        private const val REQUEST_CODE_RINGTONE: Int = 1
        private const val REQUEST_CODE_NOTIFICATION: Int = 2
        private const val REQUEST_CODE_ALARM: Int = 4

        fun newIntent(context: Context, id: UUID?): Intent {
            val intent = Intent(context, EditProfileActivity::class.java)
            if (id != null) {
                intent.putExtra(EXTRA_UUID, id)
            }
            return intent
        }
    }
}