package com.example.volumeprofiler.util.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import com.example.volumeprofiler.views.SwitchableConstraintLayout
import android.media.AudioManager.*
import android.app.NotificationManager.Policy.*
import android.app.NotificationManager.*
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.EditProfileActivity
import com.example.volumeprofiler.util.interruptionPolicy.isAlarmStreamActive
import com.example.volumeprofiler.util.interruptionPolicy.isMediaStreamActive
import com.example.volumeprofiler.util.interruptionPolicy.isNotificationStreamActive
import com.example.volumeprofiler.util.interruptionPolicy.isRingerStreamActive
import com.google.android.material.appbar.CollapsingToolbarLayout

object BindingAdapters {

    private fun setEnabledState(layout: ViewGroup, enabled: Boolean): Unit {
        layout.isEnabled = enabled
        (layout as SwitchableConstraintLayout).disabled = !enabled
    }

    private fun setSilentIcon(icon: ImageView): Unit {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, R.drawable.baseline_notifications_off_black_24dp, icon.context.theme))
    }

    private fun setVibrateIcon(icon: ImageView): Unit {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, R.drawable.baseline_vibration_black_24dp, icon.context.theme))
    }

    private fun setNormalNotificationIcon(icon: ImageView): Unit {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, R.drawable.baseline_circle_notifications_deep_purple_300_24dp, icon.context.theme))
    }

    private fun setNormalRingerIcon(icon: ImageView): Unit {
        icon.setImageDrawable(ResourcesCompat.getDrawable(icon.context.resources, R.drawable.baseline_notifications_active_black_24dp, icon.context.theme))
    }

    @JvmStatic
    @BindingAdapter("title", "currentFragmentTitle", requireAll = false)
    fun bindToolbarTitle(view: CollapsingToolbarLayout, title: String, currentFragmentTitle: String): Unit {
        view.title = if (currentFragmentTitle == EditProfileActivity.TAG_PROFILE_FRAGMENT) title else "Do Not Disturb"
    }

    @JvmStatic
    @BindingAdapter("storagePermissionGranted")
    fun bindRingtoneLayout(view: SwitchableConstraintLayout, storagePermissionGranted: Boolean): Unit {
        view.disabled = !storagePermissionGranted
    }

    @JvmStatic
    @Suppress("deprecation")
    @BindingAdapter("suppressedEffectScreenOn")
    fun bindSuppressedEffectScreenOnSwitch(view: Switch, suppressedEffectScreenOn: List<Int>): Unit {
        view.isChecked = suppressedEffectScreenOn.contains(SUPPRESSED_EFFECT_SCREEN_ON)
    }

    @JvmStatic
    @Suppress("deprecation")
    @BindingAdapter("suppressedEffectScreenOff")
    fun bindSuppressedEffectScreenOffSwitch(view: Switch, suppressedEffectScreenOff: List<Int>): Unit {
        view.isChecked = suppressedEffectScreenOff.contains(SUPPRESSED_EFFECT_SCREEN_OFF)
    }

    @JvmStatic
    @BindingAdapter("rootViewGroup", "prioritySenders", "priorityCategories", "categoryType")
    fun bindStarredContactsLayout(view: View, rootViewGroup: ViewGroup, prioritySenders: Int, priorityCategories: List<Int>, categoryType: Int): Unit {
        val transition: AutoTransition = AutoTransition().apply {
            this.excludeChildren(R.id.exceptionsCallsLayout, true)
            this.excludeChildren(R.id.exceptionsMessagesLayout, true)
            this.excludeChildren(R.id.otherInterruptionsLayout, true)
        }
        TransitionManager.beginDelayedTransition(rootViewGroup, transition)
        view.isVisible = prioritySenders == PRIORITY_SENDERS_STARRED && priorityCategories.contains(categoryType)
    }

    @JvmStatic
    @BindingAdapter("repeatingCallersPriorityCategories", "callSenders", requireAll = false)
    fun bindRepeatingCallersSwitch(view: Switch, repeatingCallersPriorityCategories: List<Int>, callSenders: Int): Unit {
        view.isChecked = repeatingCallersPriorityCategories.contains(PRIORITY_CATEGORY_REPEAT_CALLERS)
    }

    @JvmStatic
    @BindingAdapter("callSenders", "priorityCategories", requireAll = false)
    fun bindRepeatingCallersLayout(view: SwitchableConstraintLayout, callSenders: Int, priorityCategories: List<Int>): Unit {
        setEnabledState(view, callSenders != PRIORITY_SENDERS_ANY || !priorityCategories.contains(PRIORITY_CATEGORY_CALLS))
    }

    @JvmStatic
    @BindingAdapter("title")
    fun bindActivityToolbar(toolbar: Toolbar, title: String): Unit {
        toolbar.title = title
    }

    @JvmStatic
    @BindingAdapter("mediaInterruptionFilter", "mediaPriorityCategories", "notificationAccessGranted",requireAll = false)
    fun bindMediaSeekBar(view: SeekBar, mediaInterruptionFilter: Int, mediaPriorityCategories: List<Int>, notificationAccessGranted: Boolean): Unit {
        view.isEnabled = isMediaStreamActive(mediaInterruptionFilter, mediaPriorityCategories, notificationAccessGranted)
    }

    @JvmStatic
    @BindingAdapter("callInterruptionFilter")
    fun bindCallSeekBar(view: SeekBar, callInterruptionFilter: Int): Unit {
        view.isEnabled = true
    }

    @JvmStatic
    @BindingAdapter("notificationInterruptionFilter", "notificationPriorityCategories", "notificationMode", "notificationAccessGranted")
    fun bindNotificationSeekBar(view: SeekBar, notificationInterruptionFilter: Int, notificationPriorityCategories: List<Int>, notificationMode: Int, notificationAccessGranted: Boolean): Unit {
        view.isEnabled = isNotificationStreamActive(notificationInterruptionFilter, notificationPriorityCategories, notificationAccessGranted)
    }

    @JvmStatic
    @BindingAdapter("iconRingerMode", "ringerIconInterruptionFilter", "ringerPriorityCategories", "notificationAccessGranted")
    fun bindRingerIcon(icon: ImageView, ringerMode: Int, ringerIconInterruptionFilter: Int, ringerPriorityCategories: List<Int>, notificationAccessGranted: Boolean): Unit {
        if (!isRingerStreamActive(ringerIconInterruptionFilter, ringerPriorityCategories, notificationAccessGranted)) {
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
    @BindingAdapter("notificationMode", "notificationInterruptionFilter", "notificationPriorityCategories", "notificationAccessGranted", requireAll = false)
    fun bindNotificationIcon(icon: ImageView, notificationMode: Int, notificationInterruptionFilter: Int, notificationPriorityCategories: List<Int>, notificationAccessGranted: Boolean): Unit {
        if (!isNotificationStreamActive(notificationInterruptionFilter, notificationPriorityCategories, notificationAccessGranted)) {
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
    @BindingAdapter("ringerMode", "ringerSeekBarInterruptionFilter", "ringerSeekBarPropertyCategories", "notificationAccessGranted", requireAll = false)
    fun bindRingSeekBar(view: SeekBar, ringerMode: Int, ringerSeekBarInterruptionFilter: Int, ringerSeekBarPropertyCategories: List<Int>, notificationAccessGranted: Boolean): Unit {
        view.isEnabled = isRingerStreamActive(ringerSeekBarInterruptionFilter, ringerSeekBarPropertyCategories, notificationAccessGranted)
    }

    @JvmStatic
    @BindingAdapter("alarmInterruptionFilter", "alarmPriorityCategories", "notificationAccessGranted", requireAll = false)
    fun bindAlarmSeekBar(view: SeekBar, alarmInterruptionFilter: Int, alarmPriorityCategories: List<Int>, notificationAccessGranted: Boolean): Unit {
        view.isEnabled = isAlarmStreamActive(alarmInterruptionFilter, alarmPriorityCategories, notificationAccessGranted)
    }

    @JvmStatic
    @BindingAdapter("ringerMode", "ringerSwitchInterruptionFilter", "ringerSwitchPriorityCategories", "notificationAccessGranted", requireAll = false)
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun bindRingerSwitch(view: Switch, ringerMode: Int, ringerInterruptionFilter: Int, ringerSwitchPriorityCategories: List<Int>, notificationAccessGranted: Boolean) {
        if (!isRingerStreamActive(ringerInterruptionFilter, ringerSwitchPriorityCategories, notificationAccessGranted)) {
            view.isChecked = true
        } else {
            view.isChecked = ringerMode == RINGER_MODE_SILENT
        }
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
    @BindingAdapter("preferencesInterruptionFilter", "notificationPolicyAccess", requireAll = false)
    fun bindInterruptionPreferencesLayout(viewGroup: ViewGroup, interruptionFilter: Int, notificationPolicyAccess: Boolean): Unit {
        if (notificationPolicyAccess) {
            setEnabledState(viewGroup, interruptionFilter == INTERRUPTION_FILTER_PRIORITY)
        } else {
            setEnabledState(viewGroup, false)
        }
    }

    @JvmStatic
    @BindingAdapter("silentModeInterruptionFilter", "silentModePriorityCategories", "notificationAccessGranted")
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun bindRingerSilentModeLayout(viewGroup: ViewGroup, interruptionFilterRinger: Int, silentModePriorityCategories: List<Int>, notificationAccessGranted: Boolean): Unit {
        if (!notificationAccessGranted) {
            setEnabledState(viewGroup, true)
        } else {
            setEnabledState(viewGroup, (interruptionFilterRinger == INTERRUPTION_FILTER_PRIORITY && (silentModePriorityCategories.contains(PRIORITY_CATEGORY_CALLS) || silentModePriorityCategories.contains(PRIORITY_CATEGORY_REPEAT_CALLERS))) || (interruptionFilterRinger == INTERRUPTION_FILTER_ALL))
        }
    }
}