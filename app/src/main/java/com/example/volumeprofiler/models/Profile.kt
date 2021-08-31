package com.example.volumeprofiler.models

import android.net.Uri
import androidx.room.PrimaryKey
import java.util.UUID
import android.app.NotificationManager.Policy.*
import android.media.AudioManager
import android.app.NotificationManager.*
import android.os.Parcelable
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.room.Ignore
import com.example.volumeprofiler.BR
import kotlinx.parcelize.*

@Parcelize
@androidx.room.Entity
data class Profile(var title: String,
                   @PrimaryKey val id: UUID = UUID.randomUUID(),

                   var mediaVolume: Int = 0,
                   var callVolume: Int = 1,
                   var notificationVolume: Int = 0,
                   var ringVolume: Int = 0,
                   var alarmVolume: Int = 1,

                   var phoneRingtoneUri: Uri = Uri.EMPTY,
                   var notificationSoundUri: Uri = Uri.EMPTY,
                   var alarmSoundUri: Uri = Uri.EMPTY,

                   var interruptionFilter: Int = INTERRUPTION_FILTER_PRIORITY,
                   var isInterruptionFilterActive: Int = 1,
                   var ringerMode: Int = AudioManager.RINGER_MODE_NORMAL,
                   var isVibrateForCallsActive: Int = 0,

                   var priorityCategories: ArrayList<Int> = arrayListOf(PRIORITY_CATEGORY_CALLS, PRIORITY_CATEGORY_MESSAGES),
                   var priorityCallSenders: Int = PRIORITY_SENDERS_ANY,
                   var priorityMessageSenders: Int = PRIORITY_SENDERS_ANY,
                   var screenOnVisualEffects: ArrayList<Int> = arrayListOf(),
                   var screenOffVisualEffects: ArrayList<Int> = arrayListOf(),
                   var primaryConversationSenders: Int = CONVERSATION_SENDERS_ANYONE): Parcelable {

    /*
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _mediaVolume: Int = 1
        set(value) {
            mediaVolume = value
            field = value
            notifyPropertyChanged(BR._mediaVolume)
        }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _callVolume: Int = 1
    set(value) {
        callVolume = value
        field = value
        notifyPropertyChanged(BR._callVolume)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _notificationVolume: Int = 0
    set(value) {
        notificationVolume = value
        field = value
        notifyPropertyChanged(BR._notificationVolume)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _ringVolume: Int = 0
    set(value) {
        ringVolume = value
        field = value
        notifyPropertyChanged(BR._ringVolume)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _alarmVolume: Int = 1
    set(value) {
        alarmVolume = value
        field = value
        notifyPropertyChanged(BR._alarmVolume)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _phoneRingtoneUri: Uri = Uri.EMPTY
    set(value) {
        phoneRingtoneUri = value
        field = value
        notifyPropertyChanged(BR._phoneRingtoneUri)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _notificationSoundUri: Uri = Uri.EMPTY
    set(value) {
        notificationSoundUri = value
        field = value
        notifyPropertyChanged(BR._notificationSoundUri)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _alarmSoundUri: Uri = Uri.EMPTY
    set(value) {
        alarmSoundUri = value
        field = value
        notifyPropertyChanged(BR._alarmSoundUri)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _interruptionFilter: Int = INTERRUPTION_FILTER_ALL
    set(value) {
        interruptionFilter = value
        field = value
        notifyPropertyChanged(BR._interruptionFilter)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _isInterruptionFilterActive: Int = 0
    set(value) {
        isInterruptionFilterActive = value
        field = value
        notifyPropertyChanged(BR._isInterruptionFilterActive)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _ringerMode: Int = AudioManager.RINGER_MODE_NORMAL
    set(value) {
        ringerMode = value
        field = value
        notifyPropertyChanged(BR._ringerMode)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _isVibrateForCallsActive: Int = 0
    set(value) {
        isVibrateForCallsActive = value
        field = value
        notifyPropertyChanged(BR._isVibrateForCallsActive)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _priorityCategories: ArrayList<Int> = arrayListOf(PRIORITY_CATEGORY_CALLS, PRIORITY_CATEGORY_MESSAGES)
    set(value) {
        priorityCategories = value
        field = value
        notifyPropertyChanged(BR._priorityCategories)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _priorityCallSenders: Int = PRIORITY_SENDERS_ANY
    set(value) {
        priorityCallSenders = value
        field = value
        notifyPropertyChanged(BR._priorityCallSenders)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _priorityMessageSenders: Int = PRIORITY_SENDERS_ANY
    set(value) {
        priorityMessageSenders = value
        field = value
        notifyPropertyChanged(BR._priorityMessageSenders)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _screenOnVisualEffects: ArrayList<Int> = arrayListOf()
    set(value) {
        screenOnVisualEffects = value
        field = value
        notifyPropertyChanged(BR._screenOnVisualEffects)
    }
    @Ignore
    @IgnoredOnParcel
    @get:Bindable
    var _screenOffVisualEffects: ArrayList<Int> = arrayListOf()
    set(value) {
        screenOffVisualEffects = value
        field = value
        notifyPropertyChanged(BR._screenOffVisualEffects)
    }

    @Ignore
    override fun toString(): String {
        return title
    }
     */
}