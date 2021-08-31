package com.example.volumeprofiler.util.ui

import android.app.NotificationManager
import android.os.Build
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.example.volumeprofiler.views.SwitchableConstraintLayout
import android.media.AudioManager.*
import android.app.NotificationManager.Policy.*

object BindingAdapters {

    private fun setEnabledState(layout: ViewGroup, enabled: Boolean): Unit {
        layout.isEnabled = enabled
        (layout as SwitchableConstraintLayout).disabled = !enabled
    }

    @JvmStatic
    @BindingAdapter("ringerMode", "ringerSeekBar")
    fun setRingerState(view: Switch, ringerMode: Int, ringerSeekBar: SeekBar) {
        view.isChecked = ringerMode != RINGER_MODE_NORMAL && ringerMode != RINGER_MODE_VIBRATE
        ringerSeekBar.isEnabled = !view.isChecked
    }

    @JvmStatic
    @BindingAdapter("shouldVibrateForCalls")
    fun setVibrateForCallsSwitch(view: Switch, shouldVibrateForCalls: Int) {
        view.isChecked = shouldVibrateForCalls == 1
    }

    @JvmStatic
    @BindingAdapter(
            "priorityCategories",
            "interruptionFilter", "mediaSeekBar",
            "callSeekBar",
            "notificationSeekBar",
            "ringerSeekBar",
            "alarmSeekBar",
            "ringerMode",
            "silentModeLayout", "interruptionPreferences")
    fun setInterruptionFilter(
            view: TextView,
            priorityCategories: List<Int>,
            interruptionFilter: Int,
            mediaSeekBar: SeekBar,
            callSeekBar: SeekBar,
            notificationSeekBar: SeekBar,
            ringerSeekBar: SeekBar, alarmSeekBar: SeekBar,
            ringerMode: Int, silentModeLayout: ViewGroup, interruptionPreferences: ViewGroup) {
        when (interruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> {
                setEnabledState(silentModeLayout, true)
                mediaSeekBar.isEnabled = if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
                    priorityCategories.contains(PRIORITY_CATEGORY_MEDIA)
                } else {
                    true
                }
                callSeekBar.isEnabled = true
                notificationSeekBar.isEnabled = true
                ringerSeekBar.isEnabled = ringerMode == RINGER_MODE_NORMAL || ringerMode == RINGER_MODE_VIBRATE
                alarmSeekBar.isEnabled = if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
                    priorityCategories.contains(PRIORITY_CATEGORY_ALARMS)
                } else {
                    true
                }
            }
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> {
                setEnabledState(silentModeLayout, false)
                mediaSeekBar.isEnabled = true
                callSeekBar.isEnabled = true
                notificationSeekBar.isEnabled = false
                ringerSeekBar.isEnabled = false
                alarmSeekBar.isEnabled = true
            }
            NotificationManager.INTERRUPTION_FILTER_NONE -> {
                setEnabledState(silentModeLayout, false)
                mediaSeekBar.isEnabled = false
                callSeekBar.isEnabled = false
                notificationSeekBar.isEnabled = false
                ringerSeekBar.isEnabled = false
                alarmSeekBar.isEnabled = false
            }
            NotificationManager.INTERRUPTION_FILTER_ALL -> {
                setEnabledState(silentModeLayout, true)
                mediaSeekBar.isEnabled = true
                callSeekBar.isEnabled = true
                notificationSeekBar.isEnabled = true
                ringerSeekBar.isEnabled = ringerMode == RINGER_MODE_NORMAL || ringerMode == RINGER_MODE_VIBRATE
                alarmSeekBar.isEnabled = true
            }
        }
    }
}