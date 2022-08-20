package ru.rrtry.silentdroid.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import ru.rrtry.silentdroid.ui.views.PreferenceConstraintLayout
import android.media.AudioManager.*
import android.app.NotificationManager.Policy.*
import android.app.NotificationManager.*
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.adapters.ProfileSpinnerAdapter
import ru.rrtry.silentdroid.entities.Profile
import com.google.android.material.textfield.TextInputLayout
import ru.rrtry.silentdroid.core.*

@SuppressLint("UseSwitchCompatOrMaterialCode")
object BindingAdapters {

    private const val DRAWABLE_ALARM_ON: Int = R.drawable.baseline_alarm_deep_purple_300_24dp
    private const val DRAWABLE_ALARM_OFF: Int = R.drawable.baseline_alarm_off_deep_purple_500_24dp
    private const val DRAWABLE_NOTIFICATIONS_OFF: Int = R.drawable.baseline_notifications_off_black_24dp
    private const val DRAWABLE_RINGER_VIBRATE: Int = R.drawable.baseline_vibration_black_24dp
    private const val DRAWABLE_NOTIFICATIONS_ON: Int = R.drawable.baseline_circle_notifications_deep_purple_300_24dp
    private const val DRAWABLE_MUSIC_OFF: Int = R.drawable.baseline_music_off_black_24dp
    private const val DRAWABLE_MUSIC_ON : Int = R.drawable.baseline_music_note_deep_purple_300_24dp
    private const val DRAWABLE_RINGER_NORMAL: Int = R.drawable.baseline_notifications_active_black_24dp
    private const val DRAWABLE_START_PLAYBACK: Int = R.drawable.ic_baseline_play_arrow_24
    private const val DRAWABLE_STOP_PLAYBACK: Int = R.drawable.ic_baseline_pause_24
    private const val DRAWABLE_VOICE_CALL_DISABLED: Int = R.drawable.ic_baseline_phone_disabled_24
    private const val DRAWABLE_VOICE_CALL_ENABLED: Int = R.drawable.baseline_call_deep_purple_300_24dp

    @JvmStatic
    private fun setEnabledState(layout: View, enabled: Boolean, setViewState: Boolean = true) {
        (layout as PreferenceConstraintLayout).apply {
            if (setViewState) isEnabled = enabled
            disabled = !enabled
        }
    }

    @JvmStatic
    private fun setAlarmIcon(view: ImageView, enabled: Boolean) {
        view.setImageDrawable(ResourcesCompat.getDrawable(
            view.context.resources, if (enabled) DRAWABLE_ALARM_ON else DRAWABLE_ALARM_OFF, view.context.theme))
    }

    @JvmStatic
    private fun setSilentIcon(view: ImageView) {
        view.setImageDrawable(ResourcesCompat.getDrawable(view.context.resources, DRAWABLE_NOTIFICATIONS_OFF, view.context.theme))
    }

    @JvmStatic
    private fun setVibrateIcon(view: ImageView) {
        view.setImageDrawable(ResourcesCompat.getDrawable(view.context.resources, DRAWABLE_RINGER_VIBRATE, view.context.theme))
    }

    @JvmStatic
    private fun setNormalNotificationIcon(view: ImageView) {
        view.setImageDrawable(ResourcesCompat.getDrawable(view.context.resources, DRAWABLE_NOTIFICATIONS_ON, view.context.theme))
    }

    @JvmStatic
    private fun setMediaOffIcon(view: ImageView) {
        view.setImageDrawable(ResourcesCompat.getDrawable(view.context.resources, DRAWABLE_MUSIC_OFF, view.context.theme))
    }

    @JvmStatic
    private fun setMediaOnIcon(view: ImageView) {
        view.setImageDrawable(ResourcesCompat.getDrawable(view.context.resources, DRAWABLE_MUSIC_ON, view.context.theme))
    }

    @JvmStatic
    private fun setNormalRingerIcon(view: ImageView) {
        view.setImageDrawable(ResourcesCompat.getDrawable(view.context.resources, DRAWABLE_RINGER_NORMAL, view.context.theme))
    }

    @JvmStatic
    private fun setDisallowVoiceCallIcon(view: ImageView) {
        view.setImageDrawable(ResourcesCompat.getDrawable(
            view.context.resources, DRAWABLE_VOICE_CALL_DISABLED, view.context.theme
        ))
    }

    @JvmStatic
    private fun setAllowVoiceCallIcon(view: ImageView) {
        view.setImageDrawable(ResourcesCompat.getDrawable(
            view.context.resources, DRAWABLE_VOICE_CALL_ENABLED, view.context.theme
        ))
    }

    @JvmStatic
    @BindingAdapter("alarmInterruptionFilter", "priorityCategories", "policyAccessGranted", "index", "isFixedVolume")
    fun bindAlarmIcon(
        imageView: ImageView,
        alarmInterruptionFilter: Int,
        priorityCategories: Int,
        policyAccessGranted: Boolean,
        index: Int,
        isFixedVolume: Boolean)
    {
        setAlarmIcon(imageView, interruptionPolicyAllowsAlarmsStream(
            alarmInterruptionFilter, priorityCategories, policyAccessGranted)
                && !canMuteAlarmStream(index)
                && !isFixedVolume)
    }

    @JvmStatic
    @BindingAdapter("mediaInterruptionFilter", "mediaPriorityCategories", "policyAccessGranted", "isFixedVolume")
    fun bindMediaSeekbarLayout(
        viewGroup: View,
        mediaInterruptionFilter: Int,
        mediaPriorityCategories: Int,
        policyAccessGranted: Boolean,
        isFixedVolume: Boolean)
    {
        setEnabledState(viewGroup, interruptionPolicyAllowsMediaStream(
            mediaInterruptionFilter,
            mediaPriorityCategories,
            policyAccessGranted) && !isFixedVolume)
    }

    @JvmStatic
    @BindingAdapter("ringerInterruptionFilter", "ringerPriorityCategories", "policyAccessGranted", "streamsUnlinked", "isFixedVolume")
    fun bindRingerSeekbarLayout(
        viewGroup: View,
        ringerInterruptionFilter: Int,
        ringerPriorityCategories: Int,
        policyAccessGranted: Boolean,
        streamsUnlinked: Boolean,
        isFixedVolume: Boolean)
    {
        setEnabledState(viewGroup, interruptionPolicyAllowsRingerStream(
            ringerInterruptionFilter,
            ringerPriorityCategories,
            policyAccessGranted,
            streamsUnlinked) && !isFixedVolume)
    }

    @JvmStatic
    @BindingAdapter("alarmInterruptionFilter", "alarmPriorityCategories", "policyAccessGranted", "isFixedVolume")
    fun bindAlarmSeekbarLayout(
        viewGroup: View,
        alarmInterruptionFilter: Int,
        alarmPriorityCategories: Int,
        policyAccessGranted: Boolean,
        isFixedVolume: Boolean)
    {
        setEnabledState(viewGroup, interruptionPolicyAllowsAlarmsStream(
            alarmInterruptionFilter,
            alarmPriorityCategories,
            policyAccessGranted) && !isFixedVolume)
    }

    @JvmStatic
    @BindingAdapter("mediaInterruptionFilter", "mediaPriorityCategories", "notificationAccessGranted", "mediaVolume", "isFixedVolume")
    fun bindMediaIcon(
        imageView: ImageView,
        mediaInterruptionFilter: Int,
        mediaPriorityCategories: Int,
        notificationAccessGranted: Boolean,
        mediaVolume: Int,
        isFixedVolume: Boolean)
    {
        val isMute: Boolean = mediaVolume == 0
        val policyAllows: Boolean = interruptionPolicyAllowsMediaStream(
            mediaInterruptionFilter,
            mediaPriorityCategories,
            notificationAccessGranted
        )
        if (!policyAllows || isMute || isFixedVolume) {
            setMediaOffIcon(imageView)
        } else {
            setMediaOnIcon(imageView)
        }
    }

    @JvmStatic
    @BindingAdapter("storagePermissionGranted")
    fun bindRingtoneLayout(view: View, storagePermissionGranted: Boolean): Unit {
        setEnabledState(view, storagePermissionGranted)
    }

    @JvmStatic
    @Suppress("deprecation")
    @BindingAdapter("suppressedEffectScreenOn")
    fun bindSuppressedEffectScreenOnSwitch(view: Switch, suppressedEffectScreenOn: Int): Unit {
        view.isChecked = (suppressedEffectScreenOn and SUPPRESSED_EFFECT_SCREEN_ON) != 0
    }

    @JvmStatic
    @Suppress("deprecation")
    @BindingAdapter("suppressedEffectScreenOff")
    fun bindSuppressedEffectScreenOffSwitch(view: Switch, suppressedEffectScreenOff: Int): Unit {
        view.isChecked = (suppressedEffectScreenOff and SUPPRESSED_EFFECT_SCREEN_OFF) != 0
    }

    @JvmStatic
    @BindingAdapter("repeatingCallersPriorityCategories", "callSenders")
    fun bindRepeatingCallersSwitch(view: Switch, repeatingCallersPriorityCategories: Int, callSenders: Int): Unit {
        if (callSenders == PRIORITY_SENDERS_ANY
            && containsCategory(repeatingCallersPriorityCategories, PRIORITY_CATEGORY_CALLS))
        {
            view.isChecked = true
        } else {
            view.isChecked = containsCategory(repeatingCallersPriorityCategories, PRIORITY_CATEGORY_REPEAT_CALLERS)
        }
    }

    @JvmStatic
    @BindingAdapter("callSenders", "priorityCategories", requireAll = false)
    fun bindRepeatingCallersLayout(view: View, callSenders: Int, priorityCategories: Int): Unit {
        setEnabledState(view, callSenders != PRIORITY_SENDERS_ANY || priorityCategories and PRIORITY_CATEGORY_CALLS == 0)
    }

    @JvmStatic
    @BindingAdapter(
        "ringerMode",
        "interruptionFilter",
        "priorityCategories",
        "policyAccessGranted",
        "streamsUnlinked",
        "hasSeparateNotificationStream",
        "isFixedVolume"
    )
    fun bindNotificationSeekbarLayout(
        view: View,
        ringerMode: Int,
        interruptionFilter: Int,
        priorityCategories: Int,
        policyAccessGranted: Boolean,
        streamsUnlinked: Boolean,
        hasSeparateNotificationStream: Boolean,
        isFixedVolume: Boolean)
    {
        setEnabledState(view, interruptionPolicyAllowsNotificationStream(
            interruptionFilter,
            priorityCategories,
            policyAccessGranted,
            streamsUnlinked)
                && !ringerModeMutesNotifications(ringerMode, hasSeparateNotificationStream)
                && !isFixedVolume
        )
    }

    @JvmStatic
    @BindingAdapter("mediaInterruptionFilter",
        "mediaPriorityCategories",
        "notificationAccessGranted",
        "isFixedVolume",
        requireAll = false)
    fun bindMediaSeekBar(view: SeekBar,
                         mediaInterruptionFilter: Int,
                         mediaPriorityCategories: Int,
                         notificationAccessGranted: Boolean,
                         isFixedVolume: Boolean)
    {
        view.isEnabled = interruptionPolicyAllowsMediaStream(
            mediaInterruptionFilter,
            mediaPriorityCategories,
            notificationAccessGranted) && !isFixedVolume
    }

    @JvmStatic
    @BindingAdapter("isVolumeFixed")
    fun bindCallIcon(view: ImageView, isVolumeFixed: Boolean) {
        if (isVolumeFixed) {
            setDisallowVoiceCallIcon(view)
        } else {
            setAllowVoiceCallIcon(view)
        }
    }

    @JvmStatic
    @BindingAdapter("callInterruptionFilter", "isFixedVolume")
    fun bindCallSeekBar(view: SeekBar, callInterruptionFilter: Int, isFixedVolume: Boolean) {
        view.isEnabled = !isFixedVolume
    }

    @JvmStatic
    @BindingAdapter("isFixedVolume")
    fun bindCallSeekbarLayout(view: PreferenceConstraintLayout, isFixedVolume: Boolean) {
        setEnabledState(view, !isFixedVolume)
    }

    @JvmStatic
    @BindingAdapter("playing")
    fun bindPlayButton(imageButton: ImageButton, playing: Boolean) {
        val context: Context = imageButton.context
        val drawable: Drawable? = ResourcesCompat.getDrawable(
            context.resources,
            if (playing) DRAWABLE_STOP_PLAYBACK else DRAWABLE_START_PLAYBACK,
            context.theme
        )
        imageButton.setImageDrawable(drawable)
    }

    @JvmStatic
    @BindingAdapter(
        "notificationInterruptionFilter",
        "notificationPriorityCategories",
        "ringerMode",
        "notificationAccessGranted",
        "streamsUnlinked",
        "hasSeparateNotificationStream",
        "isFixedVolume"
    )
    fun bindNotificationSeekBar(
        view: SeekBar,
        notificationInterruptionFilter: Int,
        notificationPriorityCategories: Int,
        ringerMode: Int,
        notificationAccessGranted: Boolean,
        streamsUnlinked: Boolean,
        hasSeparateNotificationStream: Boolean,
        isFixedVolume: Boolean)
    {
        view.isEnabled = interruptionPolicyAllowsNotificationStream(
            notificationInterruptionFilter,
            notificationPriorityCategories,
            notificationAccessGranted,
            streamsUnlinked)
                && !ringerModeMutesNotifications(ringerMode, hasSeparateNotificationStream)
                && !isFixedVolume
    }

    @JvmStatic
    @BindingAdapter("iconRingerMode",
        "ringerIconInterruptionFilter",
        "ringerPriorityCategories",
        "notificationAccessGranted",
        "streamsUnlinked",
        "isFixedVolume"
    )
    fun bindRingerIcon(
        icon: ImageView,
        ringerMode: Int,
        ringerIconInterruptionFilter: Int,
        ringerPriorityCategories: Int,
        notificationAccessGranted: Boolean,
        streamsUnlinked: Boolean,
        isFixedVolume: Boolean)
    {
        if (!interruptionPolicyAllowsRingerStream(
                ringerIconInterruptionFilter,
                ringerPriorityCategories,
                notificationAccessGranted,
                streamsUnlinked) || isFixedVolume)
        {
            setSilentIcon(icon)
        } else {
            when (ringerMode) {
                RINGER_MODE_NORMAL -> setNormalRingerIcon(icon)
                RINGER_MODE_VIBRATE -> setVibrateIcon(icon)
                RINGER_MODE_SILENT -> setSilentIcon(icon)
            }
        }
    }

    @JvmStatic
    @BindingAdapter(
        "notificationMode",
        "notificationInterruptionFilter",
        "notificationPriorityCategories",
        "notificationAccessGranted",
        "streamsUnlinked",
        "isFixedVolume"
    )
    fun bindNotificationIcon(
        icon: ImageView,
        notificationMode: Int,
        notificationInterruptionFilter: Int,
        notificationPriorityCategories: Int,
        notificationAccessGranted: Boolean,
        streamsUnlinked: Boolean,
        isFixedVolume: Boolean)
    {
        if (!interruptionPolicyAllowsNotificationStream(
                notificationInterruptionFilter,
                notificationPriorityCategories,
                notificationAccessGranted,
                streamsUnlinked) || isFixedVolume)
        {
            setSilentIcon(icon)
        } else {
            when (notificationMode) {
                RINGER_MODE_NORMAL -> setNormalNotificationIcon(icon)
                RINGER_MODE_VIBRATE -> setVibrateIcon(icon)
                RINGER_MODE_SILENT -> setSilentIcon(icon)
            }
        }
    }

    @JvmStatic
    @BindingAdapter("canWriteSettings")
    fun bindVibrateForCallsLayout(viewGroup: View, canWriteSettings: Boolean) {
        setEnabledState(viewGroup, canWriteSettings, false)
    }

    @JvmStatic
    @BindingAdapter("ringerMode",
        "ringerSeekBarInterruptionFilter",
        "ringerSeekBarPropertyCategories",
        "notificationAccessGranted",
        "streamsUnlinked",
        "isFixedVolume"
    )
    fun bindRingSeekBar(
        view: SeekBar,
        ringerMode: Int,
        ringerSeekBarInterruptionFilter: Int,
        ringerSeekBarPropertyCategories: Int,
        notificationAccessGranted: Boolean,
        streamsUnlinked: Boolean,
        isFixedVolume: Boolean)
    {
        view.isEnabled = (interruptionPolicyAllowsRingerStream(
            ringerSeekBarInterruptionFilter,
            ringerSeekBarPropertyCategories,
            notificationAccessGranted,
            streamsUnlinked) && !isFixedVolume)
    }

    @JvmStatic
    @BindingAdapter("alarmInterruptionFilter", "alarmPriorityCategories", "notificationAccessGranted", "isFixedVolume")
    fun bindAlarmSeekBar(view: SeekBar,
                         alarmInterruptionFilter: Int,
                         alarmPriorityCategories: Int,
                         notificationAccessGranted: Boolean,
                         isFixedVolume: Boolean)
    {
        view.isEnabled = (interruptionPolicyAllowsAlarmsStream(
            alarmInterruptionFilter,
            alarmPriorityCategories,
            notificationAccessGranted) && !isFixedVolume)
    }

    @JvmStatic
    @BindingAdapter("shouldVibrateForCalls")
    fun bindVibrateForCallsSwitch(view: Switch, shouldVibrateForCalls: Int) {
        view.isChecked = shouldVibrateForCalls == 1
    }

    @JvmStatic
    @BindingAdapter("notificationPolicyAccessGranted")
    fun bindInterruptionFilterLayout(viewGroup: View, notificationPolicyAccessGranted: Boolean) {
        setEnabledState(viewGroup, notificationPolicyAccessGranted, false)
    }

    @JvmStatic
    @BindingAdapter("visualsInterruptionFilter", "notificationPolicyAccessGranted")
    fun bindNotificationsVisualsLayout(viewGroup: View, interruptionFilter: Int, notificationAccessGranted: Boolean) {
        setEnabledState(
            viewGroup,
            interruptionFilter != INTERRUPTION_FILTER_ALL && notificationAccessGranted,
            true
        )
    }

    @JvmStatic
    @BindingAdapter("preferencesInterruptionFilter", "notificationPolicyAccess")
    fun bindInterruptionPreferencesLayout(viewGroup: ViewGroup, interruptionFilter: Int, notificationPolicyAccess: Boolean) {
        setEnabledState(
            viewGroup,
            interruptionFilter == INTERRUPTION_FILTER_PRIORITY && notificationPolicyAccess,
            true
        )
    }

    @JvmStatic
    @BindingAdapter("streamsUnlinked")
    fun bindUnlinkStreamsSwitch(switch: Switch, streamsUnlinked: Boolean) {
        switch.isChecked = streamsUnlinked
    }

    @JvmStatic
    @BindingAdapter("handlesNotifications", "isFixedVolume")
    fun bindUnlinkStreamsLayout(layout: ConstraintLayout, handlesNotifications: Boolean, isFixedVolume: Boolean) {
        setEnabledState(layout, !handlesNotifications && !isFixedVolume)
    }

    @JvmStatic
    @BindingAdapter("iconRes")
    fun bindProfileImage(imageView: ImageView, iconRes: Int) {
        imageView.setImageDrawable(
            ContextCompat.getDrawable(imageView.context, iconRes)
        )
    }

    @JvmStatic
    @BindingAdapter("errorText")
    fun setErrorText(textInputLayout: TextInputLayout, errorText: String?) {
        textInputLayout.error = errorText
    }

    @JvmStatic
    @BindingAdapter(value = ["profiles", "selectedProfile", "selectedProfileAttrChanged"], requireAll = false)
    fun bindProfileSpinner(spinner: Spinner, profiles: List<Profile>?, selectedProfile: Profile?, listener: InverseBindingListener) {

        if (profiles == null) return

        spinner.adapter = ProfileSpinnerAdapter(spinner.context, R.layout.spinner_profile_view, profiles)
        setCurrentSelection(spinner, selectedProfile)
        setSpinnerListener(spinner, listener)
    }

    @JvmStatic
    @InverseBindingAdapter(attribute = "selectedProfile")
    fun getSelectedProfile(spinner: Spinner): Profile {
        return spinner.selectedItem as Profile
    }

    private fun setSpinnerListener(spinner: Spinner, listener: InverseBindingListener) {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)  = listener.onChange()
            override fun onNothingSelected(adapterView: AdapterView<*>) = listener.onChange()
        }
    }

    private fun setCurrentSelection(spinner: Spinner, selectedItem: Profile?): Boolean {
        for (index in 0 until spinner.adapter.count) {
            if ((spinner.getItemAtPosition(index) as Profile).id == selectedItem?.id) {
                spinner.setSelection(index)
                return true
            }
        }
        return false
    }
}