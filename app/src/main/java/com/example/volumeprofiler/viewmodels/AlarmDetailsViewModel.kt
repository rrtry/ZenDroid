package com.example.volumeprofiler.viewmodels

import androidx.lifecycle.*
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.database.repositories.ProfileRepository
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.util.AlarmUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class AlarmDetailsViewModel @Inject constructor(
        private val profileRepository: ProfileRepository,
        private val alarmRepository: AlarmRepository
): ViewModel() {

    private var areArgsSet: Boolean = false

    val profilesStateFlow: StateFlow<List<Profile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), listOf())

    val selectedSpinnerPosition: MutableStateFlow<Int> = MutableStateFlow(-1)
    val scheduledDays: MutableStateFlow<Int> = MutableStateFlow(0)
    val scheduled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val localTime: MutableStateFlow<LocalTime> = MutableStateFlow(LocalTime.now())
    val weekDaysLocalTime: MutableStateFlow<LocalTime> = MutableStateFlow(LocalTime.now())

    val scheduleUIUpdates: Flow<Boolean> = combine(localTime, scheduledDays) {
        localTime, scheduledDays -> scheduledDays == 0 && localTime > LocalTime.now()
    }

    private var id: Long? = null
    private var isScheduled: Boolean = false

    private val channel: Channel<Event> = Channel(Channel.BUFFERED)
    val eventsFlow: Flow<Event> = channel.receiveAsFlow()

    sealed class Event {

        data class ShowDialogEvent(val dialogType: DialogType): Event()
    }

    enum class DialogType {
        DAYS_SELECTION,
        TIME_SELECTION
    }

    fun onTimeSelectButtonClick(): Unit {
        viewModelScope.launch {
            channel.send(Event.ShowDialogEvent(DialogType.TIME_SELECTION))
        }
    }

    fun onDaysSelectButtonClick(): Unit {
        viewModelScope.launch {
            channel.send(Event.ShowDialogEvent(DialogType.DAYS_SELECTION))
        }
    }

    private fun getNextInstanceDate(): Instant {
        return AlarmUtil.getNextAlarmTime(scheduledDays.value, localTime.value)
            .toInstant()
    }

    private fun getAlarmState(): Int {
        return if (scheduled.value) 1 else 0
    }

    private fun getProfileUUID(): UUID {
        return profilesStateFlow.value[selectedSpinnerPosition.value].id
    }

    fun getProfile(): Profile {
        return profilesStateFlow.value[selectedSpinnerPosition.value]
    }

    fun getAlarmId(): Long? {
        return this.id
    }

    private fun getPosition(uuid: UUID, profiles: List<Profile>): Int {
        for ((index, i) in profiles.withIndex()) {
            if (i.id == uuid) {
                return index
            }
        }
        return 0
    }

    fun setArgs(alarmRelation: AlarmRelation?, profiles: List<Profile>): Unit {
        if (!areArgsSet && alarmRelation != null) {

            selectedSpinnerPosition.value = getPosition(alarmRelation.profile.id, profiles)
            scheduledDays.value = alarmRelation.alarm.scheduledDays
            localTime.value = alarmRelation.alarm.localStartTime
            scheduled.value = alarmRelation.alarm.isScheduled == 1

            id = alarmRelation.alarm.id
            isScheduled = alarmRelation.alarm.isScheduled == 1

            areArgsSet = true
        }
    }

    fun getAlarm(): Alarm {
        val alarm: Alarm = Alarm(
            localStartTime = localTime.value,
            scheduledDays = scheduledDays.value,
            isScheduled = getAlarmState(),
            instanceStartTime = getNextInstanceDate(),
            zoneId = ZoneId.systemDefault(),
            profileUUID = getProfileUUID(),
        )
        if (id != null) {
            alarm.id = this.id!!
        }
        return alarm
    }

    override fun onCleared(): Unit {
        super.onCleared()
        channel.close()
    }

    fun addAlarm(alarm: Alarm): Unit {
        viewModelScope.launch {
            alarmRepository.addAlarm(alarm)
        }
    }

    fun updateAlarm(alarm: Alarm): Unit {
        viewModelScope.launch {
            alarmRepository.updateAlarm(alarm)
        }
    }
}