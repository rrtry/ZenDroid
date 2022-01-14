package com.example.volumeprofiler.util.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import com.example.volumeprofiler.views.SwitchableConstraintLayout
import android.media.AudioManager.*
import android.app.NotificationManager.Policy.*
import android.app.NotificationManager.*
import android.content.Context
import android.graphics.drawable.Drawable
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.example.volumeprofiler.R
import com.example.volumeprofiler.util.interruptionPolicy.*
import com.google.android.material.appbar.CollapsingToolbarLayout

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
    private fun setEnabledState(layout: ViewGroup, enabled: Boolean): Unit {
        layout.isEnabled = enabled
        (layout as SwitchableConstraintLayout).disabled = !enabled
    }

    @JvmStatic
    private fun setAlarmIcon(imageView: ImageView, enabled: Boolean): Unit {
        imageView.setImageDrawable(ResourcesCompat.getDrawable(
            imageView.context.resources, if (enabled) DRAWABLE_ALARM_ON else DRAWABLE_ALARM_OFF, imageView.context.theme))
    }

    @JvmStatic
    private fun setSilentIcon(icon: ImageView): Unit {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, DRAWABLE_NOTIFICATIONS_OFF, icon.context.theme))
    }

    @JvmStatic
    private fun setVibrateIcon(icon: ImageView): Unit {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, DRAWABLE_RINGER_VIBRATE, icon.context.theme))
    }

    @JvmStatic
    private fun setNormalNotificationIcon(icon: ImageView): Unit {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, DRAWABLE_NOTIFICATIONS_ON, icon.context.theme))
    }

    @JvmStatic
    private fun setMediaOffIcon(icon: ImageView): Unit {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, DRAWABLE_MUSIC_OFF, icon.context.theme))
    }

    @JvmStatic
    private fun setMediaOnIcon(icon: ImageView): Unit {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, DRAWABLE_MUSIC_ON, icon.context.theme))
    }

    @JvmStatic
    private fun setNormalRingerIcon(icon: ImageView): Unit {
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
    : Unit {
        setAlarmIcon(imageView, interruptionPolicyAllowsAlarmsStream(
            alarmInterruptionFilter, priorityCategories, policyAccessGranted
        ) && !canMuteAlarmStream(index))
    }

    @JvmStatic
    @BindingAdapter("mediaInterruptionFilter", "mediaPriorityCategories", "policyAccessGranted")
    fun bindMediaSliderLayout(
        viewGroup: SwitchableConstraintLayout,
        mediaInterruptionFilter: Int,
        mediaPriorityCategories: Int,
        policyAccessGranted: Boolean
    ): Unit {
        setEnabledState(viewGroup, interruptionPolicyAllowsMediaStream(
            mediaInterruptionFilter,
            mediaPriorityCategories,
            policyAccessGranted
        ))
    }

    @JvmStatic
    @BindingAdapter("ringerInterruptionFilter", "ringerPriorityCategories", "policyAccessGranted", "streamsUnlinked")
    fun bindRingerSliderLayout(
        viewGroup: SwitchableConstraintLayout,
        ringerInterruptionFilter: Int,
        ringerPriorityCategories: Int,
        policyAccessGranted: Boolean,
        streamsUnlinked: Boolean
    ): Unit {
        setEnabledState(viewGroup, interruptionPolicyAllowsRingerStream(
            ringerInterruptionFilter,
            ringerPriorityCategories,
            policyAccessGranted,
            streamsUnlinked
        ))
    }

    @JvmStatic
    @BindingAdapter("alarmInterruptionFilter", "alarmPriorityCategories", "policyAccessGranted")
    fun bindAlarmSliderLayout(
        viewGroup: SwitchableConstraintLayout,
        alarmInterruptionFilter: Int,
        alarmPriorityCategories: Int,
        policyAccessGranted: Boolean
    ): Unit {
        setEnabledState(viewGroup, interruptionPolicyAllowsAlarmsStream(
            alarmInterruptionFilter,
            alarmPriorityCategories,
            policyAccessGranted
        ))
    }

    @JvmStatic
    @BindingAdapter("mediaInterruptionFilter", "mediaPriorityCategories", "notificationAccessGranted", "mediaVolume")
    fun bindMediaIcon(
        imageView: ImageView,
        mediaInterruptionFilter: Int,
        mediaPriorityCategories: Int,
        notificationAccessGranted: Boolean,
        mediaVolume: Int)
    : Unit {
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
    @BindingAdapter("title", "currentFragmentTitle", requireAll = false)
    fun bindToolbarTitle(view: CollapsingToolbarLayout, title: String, currentFragmentTitle: String): Unit {
        view.title = title
    }

    @JvmStatic
    @BindingAdapter("storagePermissionGranted")
    fun bindRingtoneLayout(view: SwitchableConstraintLayout, storagePermissionGranted: Boolean): Unit {
        view.disabled = !storagePermissionGranted
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
    @BindingAdapter("rootViewGroup", "prioritySenders", "priorityCategories", "categoryType")
    fun bindStarredContactsLayout(
        view: View, rootViewGroup:
        ViewGroup, prioritySenders: Int,
        priorityCategories: Int,
        categoryType: Int): Unit {
        val transition: AutoTransition = AutoTransition().apply {
            excludeChildren(R.id.exceptionsCallsLayout, true)
            excludeChildren(R.id.exceptionsMessagesLayout, true)
            excludeChildren(R.id.otherInterruptionsLayout, true)
        }
        TransitionManager.beginDelayedTransition(rootViewGroup, transition)
        view.isVisible = prioritySenders == PRIORITY_SENDERS_STARRED && (priorityCategories and categoryType) != 0
    }

    @JvmStatic
    @BindingAdapter("repeatingCallersPriorityCategories", "callSenders")
    fun bindRepeatingCallersSwitch(view: Switch, repeatingCallersPriorityCategories: Int, callSenders: Int): Unit {
        if (callSenders == PRIORITY_SENDERS_ANY
            && isBitSet(repeatingCallersPriorityCategories, PRIORITY_CATEGORY_CALLS)) {
            view.isChecked = true
        } else {
            view.isChecked = isBitSet(repeatingCallersPriorityCategories, PRIORITY_CATEGORY_REPEAT_CALLERS)
        }
    }

    @JvmStatic
    @BindingAdapter("callSenders", "priorityCategories", requireAll = false)
    fun bindRepeatingCallersLayout(view: SwitchableConstraintLayout, callSenders: Int, priorityCategories: Int): Unit {
        setEnabledState(view, callSenders != PRIORITY_SENDERS_ANY || priorityCategories and PRIORITY_CATEGORY_CALLS == 0)
    }

    @JvmStatic
    @BindingAdapter( "interruptionFilter", "priorityCategories", "policyAccessGranted", "streamsUnlinked")
    fun notificationLayoutTransition(
        view: SwitchableConstraintLayout,
        interruptionFilter: Int,
        priorityCategories: Int,
        policyAccessGranted: Boolean,
        streamsUnlinked: Boolean)
    : Unit {
        setEnabledState(view, interruptionPolicyAllowsNotificationStream(
            interruptionFilter,
            priorityCategories,
            policyAccessGranted,
            streamsUnlinked))
    }

    @JvmStatic
    @BindingAdapter("mediaInterruptionFilter", "mediaPriorityCategories", "notificationAccessGranted",requireAll = false)
    fun bindMediaSeekBar(view: SeekBar, mediaInterruptionFilter: Int, mediaPriorityCategories: Int, notificationAccessGranted: Boolean): Unit {
        view.isEnabled = interruptionPolicyAllowsMediaStream(mediaInterruptionFilter, mediaPriorityCategories, notificationAccessGranted)
    }

    @JvmStatic
    @BindingAdapter("callInterruptionFilter")
    fun bindCallSeekBar(view: SeekBar, callInterruptionFilter: Int): Unit {

    }

    @JvmStatic
    @BindingAdapter("playing")
    fun bindPlayButton(imageButton: ImageButton, playing: Boolean): Unit {
        val context: Context = imageButton.context
        val drawable: Drawable? = ResourcesCompat.getDrawable(
            context.resources,
            if (playing) DRAWABLE_STOP_PLAYBACK else DRAWABLE_START_PLAYBACK,
            context.theme
        )
        imageButton.setImageDrawable(drawable)
    }

    @JvmStatic
    @BindingAdapter("notificationInterruptionFilter", "notificationPriorityCategories", "notificationMode", "notificationAccessGranted", "streamsUnlinked")
    fun bindNotificationSeekBar(
        view: SeekBar,
        notificationInterruptionFilter: Int,
        notificationPriorityCategories: Int,
        notificationMode: Int,
        notificationAccessGranted: Boolean,
        streamsUnlinked: Boolean): Unit {

        view.isEnabled = interruptionPolicyAllowsNotificationStream(
            notificationInterruptionFilter,
            notificationPriorityCategories,
            notificationAccessGranted,
            streamsUnlinked)
    }

    @JvmStatic
    @BindingAdapter("iconRingerMode", "ringerIconInterruptionFilter", "ringerPriorityCategories", "notificationAccessGranted", "streamsUnlinked")
    fun bindRingerIcon(
        icon: ImageView,
        ringerMode: Int,
        ringerIconInterruptionFilter: Int,
        ringerPriorityCategories: Int,
        notificationAccessGranted: Boolean,
        streamsUnlinked: Boolean): Unit {

        if (!interruptionPolicyAllowsRingerStream(
                ringerIconInterruptionFilter,
                ringerPriorityCategories,
                notificationAccessGranted,
                streamsUnlinked)) {
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
    @BindingAdapter("notificationMode", "notificationInterruptionFilter", "notificationPriorityCategories", "notificationAccessGranted", "streamsUnlinked")
    fun bindNotificationIcon(
        icon: ImageView,
        notificationMode: Int,
        notificationInterruptionFilter: Int,
        notificationPriorityCategories: Int,
        notificationAccessGranted: Boolean,
        streamsUnlinked: Boolean): Unit {
        if (!interruptionPolicyAllowsNotificationStream(notificationInterruptionFilter, notificationPriorityCategories, notificationAccessGranted, streamsUnlinked)) {
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
    fun bindVibrateForCallsLayout(viewGroup: SwitchableConstraintLayout, canWriteSettings: Boolean): Unit {
        viewGroup.disabled = !canWriteSettings
    }

    @JvmStatic
    @BindingAdapter("ringerMode", "ringerSeekBarInterruptionFilter", "ringerSeekBarPropertyCategories", "notificationAccessGranted", "streamsUnlinked")
    fun bindRingSeekBar(
        view: SeekBar,
        ringerMode: Int,
        ringerSeekBarInterruptionFilter: Int,
        ringerSeekBarPropertyCategories: Int,
        notificationAccessGranted: Boolean,
        streamsUnlinked: Boolean): Unit {
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
    fun bindAlarmSeekBar(view: SeekBar, alarmInterruptionFilter: Int, alarmPriorityCategories: Int, notificationAccessGranted: Boolean): Unit {
        view.isEnabled = interruptionPolicyAllowsAlarmsStream(alarmInterruptionFilter, alarmPriorityCategories, notificationAccessGranted)
    }

    @JvmStatic
    @BindingAdapter("shouldVibrateForCalls")
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun bindVibrateForCallsSwitch(view: Switch, shouldVibrateForCalls: Int) {
        view.isChecked = shouldVibrateForCalls == 1
    }

    @JvmStatic
    @BindingAdapter("notificationPolicyAccessGranted")
    fun bindInterruptionFilterLayout(viewGroup: SwitchableConstraintLayout, notificationPolicyAccessGranted: Boolean): Unit {
        viewGroup.disabled = !notificationPolicyAccessGranted
    }

    @JvmStatic
    @BindingAdapter("preferencesInterruptionFilter", "notificationPolicyAccess")
    fun bindInterruptionPreferencesLayout(viewGroup: ViewGroup, interruptionFilter: Int, notificationPolicyAccess: Boolean): Unit {
        if (notificationPolicyAccess) {
            setEnabledState(viewGroup, interruptionFilter == INTERRUPTION_FILTER_PRIORITY)
        } else {
            setEnabledState(viewGroup, false)
        }
    }

    @JvmStatic
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    @BindingAdapter("streamsUnlinked")
    fun bindUnlinkStreamsSwitch(switch: Switch, streamsUnlinked: Boolean): Unit {
        switch.isChecked = streamsUnlinked
    }

    @JvmStatic
    @BindingAdapter("silentModeInterruptionFilter", "silentModePriorityCategories", "notificationAccessGranted", "streamsUnlinked")
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun bindRingerSilentModeLayout(viewGroup: ViewGroup,
                                   interruptionFilterRinger: Int,
                                   silentModePriorityCategories: Int,
                                   notificationAccessGranted: Boolean,
                                   streamsUnlinked: Boolean): Unit {
        if (!notificationAccessGranted) {
            setEnabledState(viewGroup, true)
        } else {
            setEnabledState(viewGroup, interruptionPolicyAllowsRingerStream(interruptionFilterRinger, silentModePriorityCategories, notificationAccessGranted, streamsUnlinked))
        }
    }
}