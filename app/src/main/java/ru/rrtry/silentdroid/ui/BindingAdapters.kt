package ru.rrtry.silentdroid.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import ru.rrtry.silentdroid.ui.views.SwitchableConstraintLayout
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

    @JvmStatic
    private fun setEnabledState(layout: View, enabled: Boolean, setViewState: Boolean = true) {
        (layout as SwitchableConstraintLayout).apply {
            if (setViewState) {
                isEnabled = enabled
            }
            disabled = !enabled
        }
    }

    @JvmStatic
    private fun setAlarmIcon(imageView: ImageView, enabled: Boolean) {
        imageView.setImageDrawable(ResourcesCompat.getDrawable(
            imageView.context.resources, if (enabled) DRAWABLE_ALARM_ON else DRAWABLE_ALARM_OFF, imageView.context.theme))
    }

    @JvmStatic
    private fun setSilentIcon(icon: ImageView) {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, DRAWABLE_NOTIFICATIONS_OFF, icon.context.theme))
    }

    @JvmStatic
    private fun setVibrateIcon(icon: ImageView) {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, DRAWABLE_RINGER_VIBRATE, icon.context.theme))
    }

    @JvmStatic
    private fun setNormalNotificationIcon(icon: ImageView) {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, DRAWABLE_NOTIFICATIONS_ON, icon.context.theme))
    }

    @JvmStatic
    private fun setMediaOffIcon(icon: ImageView) {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, DRAWABLE_MUSIC_OFF, icon.context.theme))
    }

    @JvmStatic
    private fun setMediaOnIcon(icon: ImageView) {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, DRAWABLE_MUSIC_ON, icon.context.theme))
    }

    @JvmStatic
    private fun setNormalRingerIcon(icon: ImageView) {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, DRAWABLE_RINGER_NORMAL, icon.context.theme))
    }

    @JvmStatic
    @BindingAdapter("alarmInterruptionFilter", "priorityCategories", "policyAccessGranted", "index")
    fun bindAlarmIcon(
        imageView: ImageView,
        alarmInterruptionFilter: Int,
        priorityCategories: Int,
        policyAccessGranted: Boolean,
        index: Int)
    {
        setAlarmIcon(imageView, interruptionPolicyAllowsAlarmsStream(
            alarmInterruptionFilter, priorityCategories, policyAccessGranted)
                && !canMuteAlarmStream(index))
    }

    @JvmStatic
    @BindingAdapter("mediaInterruptionFilter", "mediaPriorityCategories", "policyAccessGranted")
    fun bindMediaSliderLayout(
        viewGroup: View,
        mediaInterruptionFilter: Int,
        mediaPriorityCategories: Int,
        policyAccessGranted: Boolean)
    {
        setEnabledState(viewGroup, interruptionPolicyAllowsMediaStream(
            mediaInterruptionFilter,
            mediaPriorityCategories,
            policyAccessGranted))
    }

    @JvmStatic
    @BindingAdapter("ringerInterruptionFilter", "ringerPriorityCategories", "policyAccessGranted", "streamsUnlinked")
    fun bindRingerSliderLayout(
        viewGroup: View,
        ringerInterruptionFilter: Int,
        ringerPriorityCategories: Int,
        policyAccessGranted: Boolean,
        streamsUnlinked: Boolean)
    {
        setEnabledState(viewGroup, interruptionPolicyAllowsRingerStream(
            ringerInterruptionFilter,
            ringerPriorityCategories,
            policyAccessGranted,
            streamsUnlinked))
    }

    @JvmStatic
    @BindingAdapter("alarmInterruptionFilter", "alarmPriorityCategories", "policyAccessGranted")
    fun bindAlarmSliderLayout(
        viewGroup: View,
        alarmInterruptionFilter: Int,
        alarmPriorityCategories: Int,
        policyAccessGranted: Boolean)
    {
        setEnabledState(viewGroup, interruptionPolicyAllowsAlarmsStream(
            alarmInterruptionFilter,
            alarmPriorityCategories,
            policyAccessGranted))
    }

    @JvmStatic
    @BindingAdapter("mediaInterruptionFilter", "mediaPriorityCategories", "notificationAccessGranted", "mediaVolume")
    fun bindMediaIcon(
        imageView: ImageView,
        mediaInterruptionFilter: Int,
        mediaPriorityCategories: Int,
        notificationAccessGranted: Boolean,
        mediaVolume: Int)
    {
        if (!interruptionPolicyAllowsMediaStream(mediaInterruptionFilter, mediaPriorityCategories, notificationAccessGranted)) {
            setMediaOffIcon(imageView)
        } else {
            if (mediaVolume > 0) {
                setMediaOnIcon(imageView)
            } else {
                setMediaOffIcon(imageView)
            }
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
        "hasSeparateNotificationStream"
    )
    fun notificationLayout(
        view: View,
        ringerMode: Int,
        interruptionFilter: Int,
        priorityCategories: Int,
        policyAccessGranted: Boolean,
        streamsUnlinked: Boolean,
        hasSeparateNotificationStream: Boolean)
    {
        setEnabledState(view, interruptionPolicyAllowsNotificationStream(
            interruptionFilter,
            priorityCategories,
            policyAccessGranted,
            streamsUnlinked) && !ringerModeMutesNotifications(ringerMode, hasSeparateNotificationStream)
        )
    }

    @JvmStatic
    @BindingAdapter("mediaInterruptionFilter", "mediaPriorityCategories", "notificationAccessGranted",requireAll = false)
    fun bindMediaSeekBar(view: SeekBar, mediaInterruptionFilter: Int, mediaPriorityCategories: Int, notificationAccessGranted: Boolean) {
        view.isEnabled = interruptionPolicyAllowsMediaStream(mediaInterruptionFilter, mediaPriorityCategories, notificationAccessGranted)
    }

    @JvmStatic
    @BindingAdapter("callInterruptionFilter")
    fun bindCallSeekBar(view: SeekBar, callInterruptionFilter: Int): Unit {

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
    )
    fun bindNotificationSeekBar(
        view: SeekBar,
        notificationInterruptionFilter: Int,
        notificationPriorityCategories: Int,
        ringerMode: Int,
        notificationAccessGranted: Boolean,
        streamsUnlinked: Boolean,
        hasSeparateNotificationStream: Boolean)
    {
        view.isEnabled = interruptionPolicyAllowsNotificationStream(
            notificationInterruptionFilter,
            notificationPriorityCategories,
            notificationAccessGranted,
            streamsUnlinked) && !ringerModeMutesNotifications(ringerMode, hasSeparateNotificationStream)
    }

    @JvmStatic
    @BindingAdapter("iconRingerMode",
        "ringerIconInterruptionFilter",
        "ringerPriorityCategories",
        "notificationAccessGranted",
        "streamsUnlinked"
    )
    fun bindRingerIcon(
        icon: ImageView,
        ringerMode: Int,
        ringerIconInterruptionFilter: Int,
        ringerPriorityCategories: Int,
        notificationAccessGranted: Boolean,
        streamsUnlinked: Boolean)
    {
        if (!interruptionPolicyAllowsRingerStream(
                ringerIconInterruptionFilter,
                ringerPriorityCategories,
                notificationAccessGranted,
                streamsUnlinked))
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
        "streamsUnlinked"
    )
    fun bindNotificationIcon(
        icon: ImageView,
        notificationMode: Int,
        notificationInterruptionFilter: Int,
        notificationPriorityCategories: Int,
        notificationAccessGranted: Boolean,
        streamsUnlinked: Boolean)
    {
        if (!interruptionPolicyAllowsNotificationStream(
                notificationInterruptionFilter,
                notificationPriorityCategories,
                notificationAccessGranted,
                streamsUnlinked))
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
        "streamsUnlinked"
    )
    fun bindRingSeekBar(
        view: SeekBar,
        ringerMode: Int,
        ringerSeekBarInterruptionFilter: Int,
        ringerSeekBarPropertyCategories: Int,
        notificationAccessGranted: Boolean,
        streamsUnlinked: Boolean)
    {
        if (notificationAccessGranted) {
            view.isEnabled = interruptionPolicyAllowsRingerStream(
                ringerSeekBarInterruptionFilter,
                ringerSeekBarPropertyCategories,
                notificationAccessGranted,
                streamsUnlinked)
        } else {
            view.isEnabled = true
        }
    }

    @JvmStatic
    @BindingAdapter("alarmInterruptionFilter", "alarmPriorityCategories", "notificationAccessGranted")
    fun bindAlarmSeekBar(view: SeekBar, alarmInterruptionFilter: Int, alarmPriorityCategories: Int, notificationAccessGranted: Boolean) {
        view.isEnabled = interruptionPolicyAllowsAlarmsStream(
            alarmInterruptionFilter,
            alarmPriorityCategories,
            notificationAccessGranted)
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
    @BindingAdapter("handlesNotifications")
    fun bindUnlinkStreamsLayout(layout: ConstraintLayout, handlesNotifications: Boolean) {
        setEnabledState(layout, !handlesNotifications)
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