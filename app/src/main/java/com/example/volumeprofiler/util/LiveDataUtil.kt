package com.example.volumeprofiler.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.example.volumeprofiler.util.interruptionPolicy.interruptionPolicyAllowsNotificationStream
import android.media.AudioManager.*
import com.example.volumeprofiler.util.ProfileUtil.Companion.RINGER_MODE_BLOCKED
import com.example.volumeprofiler.util.interruptionPolicy.interruptionPolicyAllowsRingerStream

fun getNotificationMode(
    notificationVolumeLiveData: LiveData<Int>,
    interruptionFilterLiveData: LiveData<Int>,
    priorityCategoriesLiveData: LiveData<ArrayList<Int>>,
    streamsUnlinkedLiveData: LiveData<Boolean>,
    notificationAccessGrantedLiveData: LiveData<Boolean>,
    supportsVibration: Boolean
    ): MediatorLiveData<Int> {

    return MediatorLiveData<Int>().apply {

        var notificationVolume: Int? = null
        var interruptionFilter: Int? = null
        var priorityCategories: ArrayList<Int>? = null
        var streamsUnlinked: Boolean? = null
        var notificationAccessGranted: Boolean? = null

        fun update() {

            val localNotificationVolume: Int? = notificationVolume
            val localInterruptionFilter: Int? = interruptionFilter
            val localPriorityCategories: ArrayList<Int>? = priorityCategories
            val localStreamsUnlinked: Boolean? = streamsUnlinked
            var localNotificationAccessGranted: Boolean? = notificationAccessGranted

            if (
                localNotificationVolume != null &&
                localStreamsUnlinked != null &&
                localInterruptionFilter != null &&
                localPriorityCategories != null &&
                localNotificationAccessGranted != null &&
                localStreamsUnlinked != null) {

                value = when {
                    !interruptionPolicyAllowsNotificationStream(
                        localInterruptionFilter,
                        localPriorityCategories,
                        localNotificationAccessGranted,
                        localStreamsUnlinked) -> {
                        RINGER_MODE_BLOCKED
                    }
                    localNotificationVolume < 1 -> {
                        if (supportsVibration) {
                            RINGER_MODE_VIBRATE
                        } else {
                            RINGER_MODE_SILENT
                        }
                    }
                    else -> {
                        RINGER_MODE_NORMAL
                    }
                }
            }
        }

        addSource(notificationAccessGrantedLiveData) {
            notificationAccessGranted = it
            update()
        }
        addSource(notificationVolumeLiveData) {
            notificationVolume = it
            update()
        }
        addSource(interruptionFilterLiveData) {
            interruptionFilter = it
            update()
        }
        addSource(priorityCategoriesLiveData) {
            priorityCategories = it
            update()
        }
        addSource(streamsUnlinkedLiveData) {
            streamsUnlinked = it
            update()
        }
    }

}

fun getRingerMode(
    ringerVolumeLiveData: LiveData<Int>,
    interruptionFilterLiveData: LiveData<Int>,
    priorityCategoriesLiveData: LiveData<ArrayList<Int>>,
    streamsUnlinkedLiveData: LiveData<Boolean>,
    notificationAccessGrantedLiveData: LiveData<Boolean>,
    supportsVibration: Boolean
): MediatorLiveData<Int> {

    return MediatorLiveData<Int>().apply {

        var ringerVolume: Int? = null
        var interruptionFilter: Int? = null
        var priorityCategories: ArrayList<Int>? = null
        var streamsUnlinked: Boolean? = null
        var notificationAccessGranted: Boolean? = null

        fun update() {

            val localRingerVolume: Int? = ringerVolume
            val localInterruptionFilter: Int? = interruptionFilter
            val localPriorityCategories: ArrayList<Int>? = priorityCategories
            val localStreamsUnlinked: Boolean? = streamsUnlinked
            val localNotificationAccessGranted: Boolean? = notificationAccessGranted

            if (
                localRingerVolume != null &&
                localStreamsUnlinked != null &&
                localInterruptionFilter != null &&
                localPriorityCategories != null &&
                localNotificationAccessGranted != null &&
                localStreamsUnlinked != null) {

                value = when {
                    !interruptionPolicyAllowsRingerStream(
                        localInterruptionFilter,
                        localPriorityCategories,
                        localNotificationAccessGranted,
                        localStreamsUnlinked) -> {
                        RINGER_MODE_SILENT
                    }
                    localRingerVolume < 1 -> {
                        if (supportsVibration) {
                            RINGER_MODE_VIBRATE
                        } else {
                            RINGER_MODE_SILENT
                        }
                    }
                    else -> {
                        RINGER_MODE_NORMAL
                    }
                }
            }
        }
        addSource(notificationAccessGrantedLiveData) {
            notificationAccessGranted = it
            update()
        }
        addSource(ringerVolumeLiveData) {
            ringerVolume = it
            update()
        }
        addSource(interruptionFilterLiveData) {
            interruptionFilter = it
            update()
        }
        addSource(priorityCategoriesLiveData) {
            priorityCategories = it
            update()
        }
        addSource(streamsUnlinkedLiveData) {
            streamsUnlinked = it
            update()
        }
    }
}